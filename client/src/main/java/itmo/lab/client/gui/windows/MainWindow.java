package itmo.lab.client.gui.windows;

import itmo.lab.client.Client;
import itmo.lab.client.gui.AppState;
import itmo.lab.client.gui.ClientBridge;
import itmo.lab.client.gui.PollingService;
import itmo.lab.client.gui.WorkerFilter;
import itmo.lab.client.gui.dialogs.*;
import itmo.lab.client.gui.i18n.I18n;
import itmo.lab.client.gui.panels.WorkersCanvasPanel;
import itmo.lab.client.gui.panels.WorkersTablePanel;
import itmo.lab.data.Worker;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Главное окно приложения: меню, таблица, канвас и строка состояния. */
public class MainWindow {

    private final AppState state;
    private final ClientBridge bridge;
    private final Client client;
    private final PollingService polling;

    private final WorkersTablePanel tablePanel;
    private final WorkersCanvasPanel canvasPanel;
    private final Label statusLabel = new Label(" ");
    private WorkerFilter currentFilter = new WorkerFilter();

    private Stage stage;

    /** Создаёт главное окно и инициализирует таблицу и канвас. */
    public MainWindow(AppState state, ClientBridge bridge, Client client, PollingService polling) {
        this.state = state;
        this.bridge = bridge;
        this.client = client;
        this.polling = polling;
        this.tablePanel  = new WorkersTablePanel(state);
        this.canvasPanel = new WorkersCanvasPanel(state);
    }

    /** Инициализирует и показывает главное окно на переданном Stage. */
    public void show(Stage primaryStage) {
        this.stage = primaryStage;

        primaryStage.titleProperty().bind(I18n.b("app.title"));
        primaryStage.setWidth(1100);
        primaryStage.setHeight(700);

        BorderPane root = new BorderPane();
        root.setTop(buildMenuBar());
        root.setCenter(buildTabs());
        root.setBottom(buildStatusBar());

        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest(e -> {
            polling.stop();
            canvasPanel.stopAnimation();
        });

        state.addChangeListener(workers -> {
            tablePanel.setWorkers(workers);
            updateStatus();
        });

        primaryStage.show();
    }

    private MenuItem menuItem(String key, Runnable action) {
        MenuItem item = new MenuItem();
        item.textProperty().bind(I18n.b(key));
        item.setOnAction(e -> action.run());
        return item;
    }

    private MenuBar buildMenuBar() {
        MenuBar bar = new MenuBar();

        Menu collMenu = new Menu();
        collMenu.textProperty().bind(I18n.b("menu.collection"));
        collMenu.getItems().addAll(
                menuItem("menu.add",       this::doInsert), new SeparatorMenuItem(),
                menuItem("menu.deleteAll", this::doClear),
                menuItem("menu.info",      this::doInfo),
                menuItem("menu.refresh",   this::doRefresh), new SeparatorMenuItem(),
                menuItem("menu.script",    this::doScript));

        Menu cmdMenu = new Menu();
        cmdMenu.textProperty().bind(I18n.b("menu.commands"));
        cmdMenu.getItems().addAll(
                menuItem("menu.countByDate", this::doCountByDate),
                menuItem("menu.uniqueDates", this::doUniqueDates));

        Menu workerMenu = new Menu();
        workerMenu.textProperty().bind(I18n.b("menu.object"));
        workerMenu.getItems().addAll(
                menuItem("menu.filter",         this::doFilter),
                menuItem("menu.clearFilter",     this::doClearFilter), new SeparatorMenuItem(),
                menuItem("menu.removeLower",     this::doRemoveLower),
                menuItem("menu.removeGreater",   this::doRemoveGreater),
                menuItem("menu.replaceIfLower",  this::doReplaceIfLower));

        Menu langMenu = new Menu();
        langMenu.textProperty().bind(I18n.b("menu.language"));

        ToggleGroup langGroup = new ToggleGroup();
        for (I18n.Lang lang : I18n.Lang.values()) {
            RadioMenuItem item = new RadioMenuItem(lang.displayName);
            item.setToggleGroup(langGroup);
            item.setSelected(lang == I18n.lang.get());
            item.setOnAction(e -> {
                I18n.lang.set(lang);
                tablePanel.setWorkers(state.getWorkers());
            });
            I18n.lang.addListener((obs, old, newLang) -> item.setSelected(newLang == lang));
            langMenu.getItems().add(item);
        }

        bar.getMenus().addAll(collMenu, cmdMenu, workerMenu, langMenu);
        return bar;
    }

    private TabPane buildTabs() {
        Tab tableTab = new Tab();
        tableTab.textProperty().bind(I18n.b("table.tab"));
        tableTab.setClosable(false);
        tableTab.setContent(tablePanel);
        tablePanel.setOnDoubleClick(this::showWorkerInfo);

        TextField canvasFilter = new TextField();
        canvasFilter.setPromptText("…");
        canvasFilter.setPrefWidth(200);
        Label filterLbl = new Label();
        filterLbl.textProperty().bind(I18n.b("canvas.filter"));
        Button clearBtn = new Button();
        clearBtn.textProperty().bind(I18n.b("canvas.filterClear"));
        clearBtn.setOnAction(e -> canvasFilter.clear());
        canvasFilter.textProperty().addListener((obs, old, text) -> canvasPanel.setFilter(text));

        HBox canvasTopBar = new HBox(6, filterLbl, canvasFilter, clearBtn);
        canvasTopBar.setPadding(new Insets(6, 8, 4, 8));

        BorderPane canvasWrapper = new BorderPane();
        canvasWrapper.setTop(canvasTopBar);
        canvasWrapper.setCenter(canvasPanel);
        canvasPanel.setOnWorkerClick(w -> {
            WorkerInfoDialog dlg = new WorkerInfoDialog(stage, w, state.getCurrentLogin());
            if (isOwner(w)) {
                dlg.setOnEdit(() -> doEdit(w));
                dlg.setOnDelete(() -> doDelete(w));
            }
            dlg.showAndWait();
        });

        Tab canvasTab = new Tab();
        canvasTab.textProperty().bind(I18n.b("canvas.tab"));
        canvasTab.setClosable(false);
        canvasTab.setContent(canvasWrapper);

        TabPane tabs = new TabPane(tableTab, canvasTab);
        return tabs;
    }

    private HBox buildStatusBar() {
        statusLabel.setPadding(new Insets(2, 8, 2, 8));
        HBox bar = new HBox(statusLabel);
        bar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1 0 0 0;");
        return bar;
    }

    private void doInsert() {
        Worker w = WorkerFormDialog.showCreate(stage);
        if (w == null) return;
        runAsync(() -> bridge.insert(w), msg -> {
            if (msg != null) showError(msg);
            doRefresh();
        });
    }

    private void doEdit(Worker existing) {
        Worker updated = WorkerFormDialog.showEdit(stage, existing);
        if (updated == null) return;
        runAsync(() -> bridge.update(existing.getId(), updated), msg -> {
            if (msg != null) showError(msg);
            doRefresh();
        });
    }

    private void doDelete(Worker w) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format(I18n.t("dlg.confirmDeleteWorker"), w.getId(), w.getName()),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle(I18n.t("dlg.confirm"));
        confirm.setHeaderText(null);
        confirm.initOwner(stage);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) return;
        runAsync(() -> bridge.removeKey(w.getId()), msg -> {
            if (msg != null) showError(msg);
            doRefresh();
        });
    }

    private void doClear() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                I18n.t("dlg.confirmClear"), ButtonType.YES, ButtonType.NO);
        confirm.setTitle(I18n.t("dlg.confirm"));
        confirm.setHeaderText(null);
        confirm.initOwner(stage);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) return;
        runAsync(bridge::clear, msg -> {
            if (msg != null) showError(msg);
            doRefresh();
        });
    }

    private void showInfo(String titleKey, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(I18n.t(titleKey));
        a.setHeaderText(null);
        a.initOwner(stage);
        a.showAndWait();
    }

    private void doInfo() {
        runAsync(bridge::info, msg -> showInfo("dlg.info", msg));
    }

    private void doRefresh() {
        runAsync(bridge::show, workers -> {
            if (workers != null) state.setWorkers(workers);
        });
    }

    private void doScript() {
        ScriptRunDialog dlg = new ScriptRunDialog(stage, client, bridge, state);
        dlg.runWithFileChooser();
    }

    private void doCountByDate() {
        String date = DateInputDialog.show(stage, "dlg.countByDateTitle");
        if (date == null) return;
        runAsync(() -> bridge.countLessThanStartDate(date), msg -> showInfo("dlg.result", msg));
    }

    private void doUniqueDates() {
        runAsync(bridge::printUniqueStartDate, msg -> showInfo("dlg.uniqueDates", msg));
    }

    private void applyFilter(WorkerFilter f) {
        currentFilter = f;
        tablePanel.setAdvancedFilter(f);
        canvasPanel.setAdvancedFilter(f);
        updateStatus();
    }

    private void doFilter() {
        WorkerFilter f = FilterDialog.show(stage, currentFilter, state.getWorkers());
        if (f == null) return;
        applyFilter(f);
    }

    private void doClearFilter() {
        applyFilter(new WorkerFilter());
    }

    private void updateStatus() {
        String base = I18n.t("status.user") + state.getCurrentLogin()
                + I18n.t("status.count") + state.getWorkers().size();
        statusLabel.setText(currentFilter.isEmpty() ? base : base + I18n.t("status.filterActive"));
    }

    private void doRemoveLower() {
        Integer id = IdInputDialog.show(stage, "dlg.removeLowerTitle", "dlg.idPrompt");
        if (id == null) return;
        runAsync(() -> bridge.removeLower(id), msg -> {
            if (msg != null) showError(msg);
            doRefresh();
        });
    }

    private void doRemoveGreater() {
        Integer id = IdInputDialog.show(stage, "dlg.removeGreaterTitle", "dlg.idPrompt");
        if (id == null) return;
        runAsync(() -> bridge.removeGreaterKey(id), msg -> {
            if (msg != null) showError(msg);
            doRefresh();
        });
    }

    private void doReplaceIfLower() {
        Integer id = IdInputDialog.show(stage, "dlg.replaceTitle", "dlg.idPrompt");
        if (id == null) return;
        Worker existing = bridge.get(id);
        if (existing == null) {
            showError(String.format(I18n.t("dlg.workerNotFound"), id));
            return;
        }
        Worker updated = WorkerFormDialog.showEdit(stage, existing);
        if (updated == null) return;
        runAsync(() -> bridge.replaceIfLower(id, updated), msg -> {
            if (msg != null) showError(msg);
            doRefresh();
        });
    }

    private void showWorkerInfo(Worker w) {
        WorkerInfoDialog dlg = new WorkerInfoDialog(stage, w, state.getCurrentLogin());
        if (isOwner(w)) {
            dlg.setOnEdit(() -> doEdit(w));
            dlg.setOnDelete(() -> doDelete(w));
        }
        dlg.showAndWait();
    }

    private boolean isOwner(Worker w) {
        return state.getCurrentLogin() != null && state.getCurrentLogin().equals(w.getOwnerLogin());
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            alert.setTitle(I18n.t("dlg.error"));
            alert.setHeaderText(null);
            alert.initOwner(stage);
            alert.showAndWait();
        });
    }

    private <T> void runAsync(Callable<T> task, Consumer<T> onDone) {
        Thread t = new Thread(() -> {
            try {
                T result = task.call();
                Platform.runLater(() -> onDone.accept(result));
            } catch (Exception ex) {
                Platform.runLater(() -> showError(I18n.t("dlg.error") + ": " + ex.getMessage()));
            }
        }, "async-task");
        t.setDaemon(true);
        t.start();
    }
}
