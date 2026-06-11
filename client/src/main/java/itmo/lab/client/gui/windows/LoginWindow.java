package itmo.lab.client.gui.windows;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import itmo.lab.client.gui.ClientBridge;
import itmo.lab.client.gui.i18n.I18n;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Модальное окно авторизации: подключение к серверу, вход и регистрация. */
public class LoginWindow {

    private static final String COLOR_OK   = "#007800";
    private static final String COLOR_WAIT = "#555555";
    private static final String COLOR_ERR  = "#cc0000";

    private final ClientBridge bridge;
    private final Stage stage;
    private boolean authenticated = false;

    private final TextField loginField    = new TextField();
    private final PasswordField passField = new PasswordField();
    private final RadioButton loginRb     = new RadioButton();
    private final RadioButton registerRb  = new RadioButton();
    private final Label statusLabel       = new Label(" ");
    private final Button continueBtn      = new Button();

    private ScheduledExecutorService connectScheduler;

    /** Создаёт модальное окно и запускает планировщик подключения. */
    public LoginWindow(ClientBridge bridge) {
        this.bridge = bridge;
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("WorkersApp");
        stage.setMinWidth(400);
        stage.setMinHeight(280);
        stage.setResizable(false);

        stage.setScene(new Scene(buildRoot(), 420, 315));
        stage.setOnCloseRequest(e -> {
            if (connectScheduler != null) connectScheduler.shutdownNow();
        });

        startConnectScheduler();
    }

    private BorderPane buildRoot() {
        Label title = new Label("WorkersApp");
        title.setFont(Font.font(null, FontWeight.BOLD, 20));

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.setAlignment(Pos.CENTER);

        Label loginLbl = new Label();
        loginLbl.textProperty().bind(I18n.b("login.login"));
        Label passLbl  = new Label();
        passLbl.textProperty().bind(I18n.b("login.password"));

        loginField.setPrefWidth(200);
        passField.setPrefWidth(200);

        form.add(loginLbl,    0, 0);
        form.add(loginField,  1, 0);
        form.add(passLbl,     0, 1);
        form.add(passField,   1, 1);
        GridPane.setHalignment(loginLbl, HPos.RIGHT);
        GridPane.setHalignment(passLbl,  HPos.RIGHT);

        ToggleGroup group = new ToggleGroup();
        loginRb.setToggleGroup(group);
        registerRb.setToggleGroup(group);
        loginRb.setSelected(true);
        loginRb.textProperty().bind(I18n.b("login.doLogin"));
        registerRb.textProperty().bind(I18n.b("login.doRegister"));

        HBox radioRow = new HBox(16, loginRb, registerRb);
        radioRow.setAlignment(Pos.CENTER);

        statusLabel.setTextFill(Color.web(COLOR_WAIT));
        statusLabel.setAlignment(Pos.CENTER);

        continueBtn.textProperty().bind(I18n.b("login.continue"));
        continueBtn.setDefaultButton(true);
        continueBtn.setMaxWidth(Double.MAX_VALUE);
        continueBtn.setOnAction(e -> onSubmit());

        VBox south = new VBox(6, statusLabel, continueBtn);
        south.setAlignment(Pos.CENTER);

        VBox center = new VBox(16, title, form, radioRow, south);
        center.setPadding(new Insets(12, 28, 24, 28));
        center.setAlignment(Pos.CENTER);

        HBox topBar = new HBox(buildLangButton());
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(6, 8, 0, 8));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(center);
        return root;
    }

    private MenuButton buildLangButton() {
        MenuButton btn = new MenuButton(I18n.lang.get().displayName);
        ToggleGroup langGroup = new ToggleGroup();
        for (I18n.Lang l : I18n.Lang.values()) {
            RadioMenuItem item = new RadioMenuItem(l.displayName);
            item.setToggleGroup(langGroup);
            item.setSelected(l == I18n.lang.get());
            item.setOnAction(e -> I18n.lang.set(l));
            I18n.lang.addListener((obs, old, newLang) -> item.setSelected(newLang == l));
            btn.getItems().add(item);
        }
        I18n.lang.addListener((obs, old, newLang) -> btn.setText(newLang.displayName));
        return btn;
    }

    private void startConnectScheduler() {
        if (connectScheduler != null && !connectScheduler.isShutdown()) return;
        connectScheduler = Executors.newSingleThreadScheduledExecutor();
        connectScheduler.scheduleWithFixedDelay(this::attemptConnect, 0, 2, TimeUnit.SECONDS);
    }

    private void attemptConnect() {
        if (bridge.isConnected()) {
            Platform.runLater(() -> setStatusKey("login.connected", COLOR_OK));
            connectScheduler.shutdown();
            return;
        }
        Platform.runLater(() -> setStatusKey("login.connecting", COLOR_WAIT));
        try {
            bridge.tryConnect();
            Platform.runLater(() -> setStatusKey("login.connected", COLOR_OK));
            connectScheduler.shutdown();
        } catch (Exception ignored) {
            Platform.runLater(() -> setStatusKey("login.serverUnavailable", COLOR_WAIT));
        }
    }

    private void onSubmit() {
        String login    = loginField.getText().trim();
        String password = passField.getText().trim();

        if (login.isEmpty() || password.isEmpty()) {
            setStatusKey("login.emptyFields", COLOR_ERR);
            return;
        }
        if (!bridge.isConnected()) {
            setStatusKey("login.waitConnect", COLOR_WAIT);
            startConnectScheduler();
            return;
        }

        setStatusKey("login.authenticating", COLOR_WAIT);
        continueBtn.setDisable(true);

        Task<String> task = new Task<>() {
            boolean success;

            @Override
            protected String call() {
                if (registerRb.isSelected()) {
                    String err = bridge.tryRegister(login, password);
                    if (err != null) return err;
                }
                String err = bridge.tryLogin(login, password);
                success = (err == null);
                return err;
            }

            @Override
            protected void succeeded() {
                continueBtn.setDisable(false);
                if (success) {
                    authenticated = true;
                    if (connectScheduler != null) connectScheduler.shutdownNow();
                    stage.close();
                } else {
                    String msg = getValue();
                    String key = serverCodeToKey(msg);
                    if (key != null) {
                        setStatusKey(key, COLOR_ERR);
                    } else {
                        setStatus(msg, COLOR_ERR);
                    }
                }
            }

            @Override
            protected void failed() {
                continueBtn.setDisable(false);
                if (!bridge.isConnected()) {
                    startConnectScheduler();
                    setStatusKey("login.serverUnavailable", COLOR_WAIT);
                } else {
                    setStatus(I18n.t("dlg.error") + ": " + getException().getMessage(), COLOR_ERR);
                }
            }
        };
        new Thread(task, "auth-task").start();
    }

    private String serverCodeToKey(String msg) {
        if (msg == null) return "login.authError";
        switch (msg) {
            case "ERR_AUTH_FAILED": return "login.authError";
            case "ERR_LOGIN_TAKEN": return "login.loginTaken";
            case "ERR_NOT_AUTH": return "login.notAuth";
            default: return null;
        }
    }

    private void setStatusKey(String key, String color) {
        statusLabel.textProperty().bind(I18n.b(key));
        statusLabel.setTextFill(Color.web(color));
    }

    private void setStatus(String text, String color) {
        statusLabel.textProperty().unbind();
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.web(color));
    }

    /** Показывает окно и блокирует вызывающий поток до его закрытия. */
    public void showAndWait() {
        stage.showAndWait();
    }

    /** Возвращает {@code true}, если пользователь успешно прошёл авторизацию. */
    public boolean isAuthenticated() {
        return authenticated;
    }
}
