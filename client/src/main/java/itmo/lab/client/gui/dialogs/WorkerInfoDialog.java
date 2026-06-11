package itmo.lab.client.gui.dialogs;

import itmo.lab.client.gui.i18n.I18n;
import itmo.lab.data.Worker;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Диалог просмотра подробной информации о работнике с кнопками редактирования и удаления. */
public class WorkerInfoDialog {

    private Runnable onEdit;
    private Runnable onDelete;
    private final Stage stage;
    private final boolean isOwner;

    public WorkerInfoDialog(Window owner, Worker w, String currentLogin) {
        this.isOwner = currentLogin != null && currentLogin.equals(w.getOwnerLogin());

        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(I18n.t("info.title") + w.getId());
        stage.setMinWidth(360);
        stage.setMinHeight(340);

        stage.setScene(new Scene(buildRoot(w), 400, 400));
    }

    private BorderPane buildRoot(Worker w) {
        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(6);
        fields.setPadding(new Insets(16, 20, 8, 20));
        ColumnConstraints lc = new ColumnConstraints();
        lc.setHalignment(HPos.RIGHT);
        lc.setMinWidth(120);
        ColumnConstraints vc = new ColumnConstraints();
        vc.setHgrow(Priority.ALWAYS);
        fields.getColumnConstraints().addAll(lc, vc);

        int row = 0;
        row = addRow(fields, row, "info.id",           String.valueOf(w.getId()));
        row = addRow(fields, row, "info.name",         w.getName() != null ? w.getName() : I18n.t("none"));
        if (w.getCoordinates() != null) {
            row = addRow(fields, row, "info.coords",
                    "X=" + w.getCoordinates().getX() + ", Y=" + w.getCoordinates().getY());
        }
        row = addRow(fields, row, "info.salary",
                I18n.formatNumber(w.getSalary()) + I18n.t("info.salarySuffix"));
        if (w.getStartDate() != null)
            row = addRow(fields, row, "info.startDate",  I18n.formatDate(w.getStartDate()));
        if (w.getCreationDate() != null)
            row = addRow(fields, row, "info.creationDate", I18n.formatDate(w.getCreationDate()));
        row = addRow(fields, row, "info.position",
                w.getPosition() != null ? w.getPosition().name() : I18n.t("none"));
        row = addRow(fields, row, "info.status",
                w.getStatus() != null ? w.getStatus().name() : I18n.t("none"));
        if (w.getOrganization() != null)
            row = addRow(fields, row, "info.organization",
                    w.getOrganization().getFullName() != null ? w.getOrganization().getFullName() : I18n.t("none"));
        addRow(fields, row, "info.owner",
                w.getOwnerLogin() != null ? w.getOwnerLogin() : I18n.t("none"));

        ScrollPane scroll = new ScrollPane(fields);
        scroll.setFitToWidth(true);
        scroll.setBorder(Border.EMPTY);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 12, 12, 12));

        if (isOwner) {
            Button deleteBtn = new Button();
            deleteBtn.textProperty().bind(I18n.b("btn.delete"));
            deleteBtn.setTextFill(Color.rgb(180, 0, 0));
            deleteBtn.setOnAction(e -> { stage.close(); Platform.runLater(() -> { if (onDelete != null) onDelete.run(); }); });
            buttons.getChildren().add(deleteBtn);

            Button editBtn = new Button();
            editBtn.textProperty().bind(I18n.b("btn.edit"));
            editBtn.setOnAction(e -> { stage.close(); Platform.runLater(() -> { if (onEdit != null) onEdit.run(); }); });
            buttons.getChildren().add(editBtn);
        }

        Button closeBtn = new Button();
        closeBtn.textProperty().bind(I18n.b("btn.close"));
        closeBtn.setDefaultButton(true);
        closeBtn.setOnAction(e -> stage.close());
        buttons.getChildren().add(closeBtn);

        BorderPane root = new BorderPane();
        root.setCenter(scroll);
        root.setBottom(buttons);
        return root;
    }

    private int addRow(GridPane gp, int row, String labelKey, String value) {
        Label lbl = new Label();
        lbl.textProperty().bind(I18n.b(labelKey));
        Label val = new Label(value);
        val.setFont(Font.font(null, FontWeight.BOLD, 12));
        val.setWrapText(true);
        gp.add(lbl, 0, row);
        gp.add(val, 1, row);
        return row + 1;
    }

    /** Задаёт обработчик, вызываемый при нажатии кнопки «Редактировать». */
    public void setOnEdit(Runnable onEdit)     { this.onEdit = onEdit; }
    /** Задаёт обработчик, вызываемый при нажатии кнопки «Удалить». */
    public void setOnDelete(Runnable onDelete) { this.onDelete = onDelete; }

    /** Показывает диалог без блокировки. */
    public void show() { stage.show(); }
    /** Показывает диалог и блокирует вызывающий поток до его закрытия. */
    public void showAndWait() { stage.showAndWait(); }
}
