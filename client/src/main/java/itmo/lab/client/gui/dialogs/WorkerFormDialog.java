package itmo.lab.client.gui.dialogs;

import itmo.lab.client.gui.i18n.I18n;
import itmo.lab.data.*;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Диалог создания и редактирования работника. */
public class WorkerFormDialog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Worker result = null;

    private final TextField nameField = new TextField();
    private final Spinner<Integer> xSpin = new Spinner<>(-Integer.MAX_VALUE, 717, 0, 1);
    private final Spinner<Double> ySpin = new Spinner<>(-1e9, 1e9, 0.0, 1.0);
    private final Spinner<Integer> salSpin = new Spinner<>(1, Integer.MAX_VALUE, 1, 1);
    private final TextField startDateFld = new TextField();
    private final ComboBox<Position> posCb = new ComboBox<>();
    private final ComboBox<String> statusCb = new ComboBox<>();

    private final CheckBox orgCheck = new CheckBox();
    private final TextField orgNameFld = new TextField();
    private final Spinner<Integer> orgTurnSpin = new Spinner<>(0, Integer.MAX_VALUE, 0, 1);
    private final Spinner<Integer> orgEmpSpin = new Spinner<>(1, Integer.MAX_VALUE, 1, 1);
    private final TextField orgStreetFld = new TextField();

    private final CheckBox townCheck = new CheckBox();
    private final Spinner<Double> townXSpin = new Spinner<>((double) Long.MIN_VALUE, (double) Long.MAX_VALUE, 0.0, 1.0);
    private final Spinner<Integer> townYSpin = new Spinner<>(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1);
    private final Spinner<Integer> townZSpin = new Spinner<>(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, 1);

    private final Stage stage;

    public WorkerFormDialog(Window owner, String titleKey, Worker existing) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(I18n.t(titleKey) + (existing != null ? existing.getId() : ""));
        stage.setMinWidth(460);
        stage.setMinHeight(520);

        posCb.getItems().setAll(Position.values());
        statusCb.getItems().add(I18n.t("none"));
        for (Status s : Status.values()) statusCb.getItems().add(s.name());
        statusCb.getSelectionModel().selectFirst();

        makeEditable(xSpin);
        makeEditable(ySpin);
        makeEditable(salSpin);
        makeEditable(orgTurnSpin);
        makeEditable(orgEmpSpin);
        makeEditable(townXSpin);
        makeEditable(townYSpin);
        makeEditable(townZSpin);

        if (existing != null) prefill(existing);
        else startDateFld.setText(LocalDateTime.now().format(FMT));

        ScrollPane scroll = new ScrollPane(buildForm());
        scroll.setFitToWidth(true);
        scroll.setBorder(Border.EMPTY);

        Button okBtn = new Button();
        okBtn.textProperty().bind(I18n.b("form.ok"));
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> onOk());

        Button cancelBtn = new Button();
        cancelBtn.textProperty().bind(I18n.b("form.cancel"));
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(8, okBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 12, 8, 12));

        BorderPane root = new BorderPane();
        root.setCenter(scroll);
        root.setBottom(buttons);

        stage.setScene(new Scene(root, 480, 560));
    }

    private GridPane buildForm() {
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);
        gp.setPadding(new Insets(12, 16, 8, 16));
        ColumnConstraints lc = new ColumnConstraints();
        lc.setHalignment(HPos.RIGHT);
        lc.setMinWidth(190);
        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().addAll(lc, fc);

        int row = 0;
        row = addField(gp, row, "form.name", nameField);
        row = addField(gp, row, "form.coordX", xSpin);
        row = addField(gp, row, "form.coordY", ySpin);
        row = addField(gp, row, "form.salary", salSpin);
        row = addField(gp, row, "form.startDate", startDateFld);
        row = addField(gp, row, "form.position", posCb);
        row = addField(gp, row, "form.status", statusCb);

        orgCheck.textProperty().bind(I18n.b("form.addOrg"));
        gp.add(orgCheck, 0, row++, 2, 1);

        TitledPane orgPane = buildOrgPane();
        orgPane.setExpanded(orgCheck.isSelected());
        orgCheck.selectedProperty().addListener((obs, old, sel) -> orgPane.setExpanded(sel));
        gp.add(orgPane, 0, row++, 2, 1);

        return gp;
    }

    private TitledPane buildOrgPane() {
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);
        gp.setPadding(new Insets(8));
        ColumnConstraints lc = new ColumnConstraints();
        lc.setHalignment(HPos.RIGHT);
        lc.setMinWidth(170);
        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().addAll(lc, fc);

        int row = 0;
        row = addField(gp, row, "form.orgName", orgNameFld);
        row = addField(gp, row, "form.orgTurnover", orgTurnSpin);
        row = addField(gp, row, "form.orgEmployees", orgEmpSpin);
        row = addField(gp, row, "form.orgStreet", orgStreetFld);

        townCheck.textProperty().bind(I18n.b("form.addTown"));
        gp.add(townCheck, 0, row++, 2, 1);

        TitledPane townPane = buildTownPane();
        townPane.setExpanded(townCheck.isSelected());
        townCheck.selectedProperty().addListener((obs, old, sel) -> townPane.setExpanded(sel));
        gp.add(townPane, 0, row++, 2, 1);

        TitledPane pane = new TitledPane();
        pane.textProperty().bind(I18n.b("form.org"));
        pane.setContent(gp);
        pane.setCollapsible(true);
        return pane;
    }

    private TitledPane buildTownPane() {
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);
        gp.setPadding(new Insets(8));
        ColumnConstraints lc = new ColumnConstraints();
        lc.setHalignment(HPos.RIGHT);
        lc.setMinWidth(40);
        ColumnConstraints fc = new ColumnConstraints();
        fc.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().addAll(lc, fc);

        int row = 0;
        row = addField(gp, row, "form.townX", townXSpin);
        row = addField(gp, row, "form.townY", townYSpin);
        addField(gp, row, "form.townZ", townZSpin);

        TitledPane pane = new TitledPane();
        pane.textProperty().bind(I18n.b("form.town"));
        pane.setContent(gp);
        pane.setCollapsible(true);
        return pane;
    }

    private int addField(GridPane gp, int row, String labelKey, javafx.scene.Node field) {
        Label lbl = new Label();
        lbl.textProperty().bind(I18n.b(labelKey));
        gp.add(lbl, 0, row);
        gp.add(field, 1, row);
        GridPane.setFillWidth(field, true);
        if (field instanceof Region) ((Region) field).setMaxWidth(Double.MAX_VALUE);
        return row + 1;
    }

    private <T> void makeEditable(Spinner<T> spinner) {
        spinner.setEditable(true);
        spinner.setMaxWidth(Double.MAX_VALUE);
    }

    private void prefill(Worker w) {
        nameField.setText(w.getName() != null ? w.getName() : "");
        if (w.getCoordinates() != null) {
            xSpin.getValueFactory().setValue(w.getCoordinates().getX());
            ySpin.getValueFactory().setValue((double) w.getCoordinates().getY());
        }
        salSpin.getValueFactory().setValue(w.getSalary());
        if (w.getStartDate() != null) startDateFld.setText(w.getStartDate().format(FMT));
        if (w.getPosition() != null) posCb.getSelectionModel().select(w.getPosition());
        if (w.getStatus() != null) statusCb.getSelectionModel().select(w.getStatus().name());
        if (w.getOrganization() != null) {
            orgCheck.setSelected(true);
            Organization org = w.getOrganization();
            orgNameFld.setText(org.getFullName() != null ? org.getFullName() : "");
            orgTurnSpin.getValueFactory().setValue(org.getAnnualTurnover() != null ? org.getAnnualTurnover() : 0);
            orgEmpSpin.getValueFactory().setValue(org.getEmployeesCount());
            if (org.getOfficialAddress() != null) {
                orgStreetFld.setText(org.getOfficialAddress().getStreet() != null ? org.getOfficialAddress().getStreet() : "");
                if (org.getOfficialAddress().getTown() != null) {
                    townCheck.setSelected(true);
                    Location t = org.getOfficialAddress().getTown();
                    townXSpin.getValueFactory().setValue((double) t.getX());
                    townYSpin.getValueFactory().setValue(t.getY());
                    townZSpin.getValueFactory().setValue(t.getZ());
                }
            }
        }
    }

    private void onOk() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) { showError(I18n.t("form.errorEmpty")); return; }

        int x = xSpin.getValue();
        float y = ySpin.getValue().floatValue();
        int salary = salSpin.getValue();

        LocalDateTime startDate;
        try {
            startDate = LocalDateTime.parse(startDateFld.getText().trim(), FMT);
        } catch (DateTimeParseException e) {
            showError(I18n.t("form.errorDate"));
            return;
        }

        Position position = posCb.getValue();
        Status status = null;
        String sel = statusCb.getValue();
        if (sel != null && !sel.equals(I18n.t("none"))) {
            try { status = Status.valueOf(sel); } catch (IllegalArgumentException ignored) {}
        }

        Organization org = null;
        if (orgCheck.isSelected()) {
            String street = orgStreetFld.getText().trim();
            if (street.isEmpty()) { showError(I18n.t("form.errorOrgStreet")); return; }
            int empCount  = orgEmpSpin.getValue();
            int turnoverVal = orgTurnSpin.getValue();
            Integer turnover = turnoverVal > 0 ? turnoverVal : null;
            String orgName = orgNameFld.getText().trim();

            Location town = null;
            if (townCheck.isSelected()) {
                town = new Location(townXSpin.getValue().longValue(), townYSpin.getValue(), townZSpin.getValue());
            }
            Address address = new Address(street, town);
            org = new Organization(orgName.isEmpty() ? null : orgName, turnover, address, empCount);
        }

        result = new Worker(0, name, new Coordinates(x, y), salary, startDate, position, status, org);
        stage.close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setTitle(I18n.t("dlg.error"));
        alert.setHeaderText(null);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    /** Возвращает созданного или отредактированного работника, или {@code null} при отмене. */
    public Worker getResult() { return result; }

    /** Показывает диалог создания нового работника и возвращает результат. */
    public static Worker showCreate(Window owner) {
        WorkerFormDialog d = new WorkerFormDialog(owner, "form.createWorker", null);
        d.stage.showAndWait();
        return d.getResult();
    }

    /** Показывает диалог редактирования существующего работника и возвращает результат. */
    public static Worker showEdit(Window owner, Worker existing) {
        WorkerFormDialog d = new WorkerFormDialog(owner, "form.editWorker", existing);
        d.stage.showAndWait();
        return d.getResult();
    }
}
