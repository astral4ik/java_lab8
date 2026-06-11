package itmo.lab.client.gui;

import itmo.lab.client.Client;
import itmo.lab.client.gui.windows.LoginWindow;
import itmo.lab.client.gui.windows.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/** Точка входа JavaFX-приложения: запускает авторизацию, поллинг и главное окно. */
public class GuiMain extends Application {

    /** Инициализирует приложение: показывает окно входа, затем открывает главное окно. */
    @Override
    public void start(Stage primaryStage) {
        Client client = new Client();
        AppState state = new AppState();
        ClientBridge bridge = new ClientBridge(client, state);

        LoginWindow loginWindow = new LoginWindow(bridge);
        loginWindow.showAndWait();

        if (!loginWindow.isAuthenticated()) {
            Platform.exit();
            return;
        }

        PollingService polling = new PollingService(bridge, state);
        new MainWindow(state, bridge, client, polling).show(primaryStage);
        polling.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
