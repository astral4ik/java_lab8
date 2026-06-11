package itmo.lab.client.gui.panels;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import itmo.lab.client.gui.AppState;
import itmo.lab.client.gui.WorkerFilter;
import itmo.lab.client.gui.i18n.I18n;
import itmo.lab.data.Worker;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/** Таблица работников с текстовым и расширенным фильтром, поддержкой сортировки и двойного клика. */
public class WorkersTablePanel extends BorderPane {

    private final ObservableList<Worker> allWorkers = FXCollections.observableArrayList();
    private final FilteredList<Worker> filtered = new FilteredList<>(allWorkers, w -> true);
    private final SortedList<Worker> sorted = new SortedList<>(filtered);
    private final TableView<Worker> table = new TableView<>(sorted);
    private final TextField filterField = new TextField();
    private WorkerFilter advancedFilter = new WorkerFilter();

    private TableColumn<Worker, Integer> idCol;
    private TableColumn<Worker, String> nameCol;
    private TableColumn<Worker, Integer> xCol;
    private TableColumn<Worker, Float> yCol;
    private TableColumn<Worker, Integer> salaryCol;
    private TableColumn<Worker, LocalDateTime> dateCol;
    private TableColumn<Worker, String> posCol, statusCol, orgCol, ownerCol;

    private Consumer<Worker> onDoubleClick;

    public WorkersTablePanel(AppState state) {
        setPadding(new Insets(8));

        sorted.comparatorProperty().bind(table.comparatorProperty());

        buildColumns();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        filterField.setPromptText("…");
        filterField.setPrefWidth(200);

        Label filterLbl = new Label();
        filterLbl.textProperty().bind(I18n.b("table.filter"));

        Button clearBtn = new Button(I18n.t("canvas.filterClear"));
        clearBtn.setOnAction(e -> filterField.clear());

        filterField.textProperty().addListener((obs, old, text) -> refresh());

        I18n.lang.addListener((obs, old, newLang) -> {
            clearBtn.setText(I18n.t("canvas.filterClear"));
            table.refresh();
        });

        HBox topBar = new HBox(6, filterLbl, filterField, clearBtn);
        topBar.setPadding(new Insets(0, 0, 6, 0));

        setTop(topBar);
        setCenter(table);

        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                Worker w = table.getSelectionModel().getSelectedItem();
                if (w != null && onDoubleClick != null) onDoubleClick.accept(w);
            }
        });

        state.addChangeListener(this::setWorkers);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildColumns() {
        idCol = colTyped("col.id", w -> w.getId(), String::valueOf);
        nameCol = col("col.name", w -> w.getName() != null ? w.getName() : "");
        xCol = colTyped("col.x",
                w -> w.getCoordinates() != null ? w.getCoordinates().getX() : null,
                n -> I18n.formatNumber(n));
        yCol = colTyped("col.y",
                w -> w.getCoordinates() != null ? w.getCoordinates().getY() : null,
                n -> I18n.formatNumber(n));
        salaryCol = colTyped("col.salary", w -> w.getSalary(), n -> I18n.formatNumber(n));
        dateCol = colTyped("col.startDate", w -> w.getStartDate(), d -> I18n.formatDate(d));
        posCol = col("col.position", w -> w.getPosition() != null ? w.getPosition().name() : I18n.t("none"));
        statusCol = col("col.status", w -> w.getStatus() != null ? w.getStatus().name() : I18n.t("none"));
        orgCol = col("col.organization", w -> w.getOrganization() != null && w.getOrganization().getFullName() != null
                ? w.getOrganization().getFullName() : "");
        ownerCol = col("col.owner", w -> w.getOwnerLogin() != null ? w.getOwnerLogin() : "");

        int[] widths = {50, 140, 60, 60, 90, 130, 110, 140, 150, 110};
        TableColumn[] cols = {idCol, nameCol, xCol, yCol, salaryCol, dateCol, posCol, statusCol, orgCol, ownerCol};
        for (int i = 0; i < cols.length; i++) {
            cols[i].setPrefWidth(widths[i]);
            cols[i].setSortable(true);
        }
        table.getColumns().setAll(cols);
    }

    private TableColumn<Worker, String> col(String key, Function<Worker, String> extractor) {
        TableColumn<Worker, String> col = new TableColumn<>();
        col.textProperty().bind(I18n.b(key));
        col.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(extractor.apply(data.getValue())));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });
        return col;
    }

    private <T> TableColumn<Worker, T> colTyped(String key, Function<Worker, T> extractor,
                                                 Function<T, String> formatter) {
        TableColumn<Worker, T> col = new TableColumn<>();
        col.textProperty().bind(I18n.b(key));
        col.setCellValueFactory(data -> new SimpleObjectProperty<>(extractor.apply(data.getValue())));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatter.apply(item));
            }
        });
        return col;
    }

    private void refresh() {
        String lower = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        filtered.setPredicate(w -> (lower.isEmpty() || matchesFilter(w, lower)) && advancedFilter.matches(w));
    }

    private boolean matchesFilter(Worker w, String lower) {
        if (String.valueOf(w.getId()).contains(lower)) return true;
        if (w.getName() != null && w.getName().toLowerCase().contains(lower)) return true;
        if (w.getCoordinates() != null) {
            if (String.valueOf(w.getCoordinates().getX()).contains(lower)) return true;
            if (String.valueOf(w.getCoordinates().getY()).contains(lower)) return true;
        }
        if (String.valueOf(w.getSalary()).contains(lower)) return true;
        if (w.getStartDate() != null && w.getStartDate().toString().toLowerCase().contains(lower)) return true;
        if (w.getPosition() != null && w.getPosition().name().toLowerCase().contains(lower)) return true;
        if (w.getStatus() != null && w.getStatus().name().toLowerCase().contains(lower)) return true;
        if (w.getOrganization() != null && w.getOrganization().getFullName() != null
                && w.getOrganization().getFullName().toLowerCase().contains(lower)) return true;
        if (w.getOwnerLogin() != null && w.getOwnerLogin().toLowerCase().contains(lower)) return true;
        return false;
    }

    /** Устанавливает расширенный фильтр и обновляет отображаемые данные. */
    public void setAdvancedFilter(WorkerFilter f) {
        this.advancedFilter = f != null ? f : new WorkerFilter();
        refresh();
    }

    /** Заменяет список работников и обновляет таблицу. */
    public void setWorkers(List<Worker> workers) {
        allWorkers.setAll(workers);
    }

    /** Возвращает выбранного в таблице работника, или {@code null} если ничего не выбрано. */
    public Worker getSelectedWorker() {
        return table.getSelectionModel().getSelectedItem();
    }

    /** Устанавливает обработчик двойного клика по строке таблицы. */
    public void setOnDoubleClick(Consumer<Worker> handler) {
        this.onDoubleClick = handler;
    }
}
