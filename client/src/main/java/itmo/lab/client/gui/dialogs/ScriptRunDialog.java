package itmo.lab.client.gui.dialogs;

import itmo.lab.client.Client;
import itmo.lab.client.ClientConsole;
import itmo.lab.client.FileInputSource;
import itmo.lab.client.InputSource;
import itmo.lab.client.gui.AppState;
import itmo.lab.client.gui.ClientBridge;
import itmo.lab.client.gui.i18n.I18n;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;

/** Диалог выполнения команд из текстового скрипт-файла в фоновом потоке. */
public class ScriptRunDialog {

    private final Client client;
    private final ClientBridge bridge;
    private final AppState state;
    private final Window owner;

    private Stage stage;
    private TextArea output;

    public ScriptRunDialog(Window owner, Client client, ClientBridge bridge, AppState state) {
        this.owner  = owner;
        this.client = client;
        this.bridge = bridge;
        this.state  = state;
    }

    /** Открывает диалог выбора файла и запускает выполнение выбранного скрипта. */
    public void runWithFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("script.title"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        buildStage();
        runScript(file.getAbsolutePath());
        stage.showAndWait();
    }

    private void buildStage() {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.titleProperty().bind(I18n.b("script.title"));
        stage.setWidth(620);
        stage.setHeight(420);

        output = new TextArea();
        output.setEditable(false);
        output.setFont(Font.font("Monospaced", 12));
        output.setWrapText(false);

        Button closeBtn = new Button();
        closeBtn.textProperty().bind(I18n.b("btn.close"));
        closeBtn.setOnAction(e -> stage.close());

        HBox south = new HBox(closeBtn);
        south.setAlignment(Pos.CENTER_RIGHT);
        south.setPadding(new Insets(8, 12, 8, 12));

        BorderPane root = new BorderPane(output, null, null, south, null);
        root.setPadding(new Insets(8));
        stage.setScene(new Scene(root));
    }

    private void runScript(String path) {
        output.setText("");
        appendLine(I18n.t("script.running") + path + "\n");

        ClientConsole console = client.getConsole();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                InputSource oldInput = console.getInputSource();
                FileInputSource fileSource = null;
                try {
                    fileSource = new FileInputSource(path);
                    console.setInputSource(fileSource);
                    console.setFromFile(true);
                    console.setOutputSink(msg -> Platform.runLater(() -> appendLine(msg)));

                    String rawLine;
                    while ((rawLine = fileSource.readLine()) != null) {
                        final String cmd = rawLine.trim();
                        if (cmd.isEmpty() || cmd.startsWith("#") || cmd.startsWith("execute_script")) continue;
                        Platform.runLater(() -> appendLine("> " + cmd));
                        client.processCommand(cmd);
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> appendLine(I18n.t("script.error") + e.getMessage()));
                } finally {
                    console.setFromFile(false);
                    console.setInputSource(oldInput);
                    console.setOutputSink(System.out::println);
                    if (fileSource != null) try { fileSource.close(); } catch (Exception ignored) {}
                }
                return null;
            }

            @Override
            protected void succeeded() {
                appendLine(I18n.t("script.done"));
                var workers = bridge.show();
                if (workers != null) state.setWorkers(workers);
            }
        };
        new Thread(task, "script-runner").start();
    }

    private void appendLine(String text) {
        output.appendText(text + "\n");
    }
}
