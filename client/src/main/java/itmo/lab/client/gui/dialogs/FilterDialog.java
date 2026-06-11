package itmo.lab.client.gui.dialogs;

import itmo.lab.client.gui.WorkerFilter;
import itmo.lab.client.gui.i18n.I18n;
import itmo.lab.data.Position;
import itmo.lab.data.Status;
import itmo.lab.data.Worker;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.*;
import java.util.stream.Collectors;

/** Диалог расширенного фильтра работников по всем полям коллекции. */
public class FilterDialog {

    private FilterDialog() {}

    private record ListBox(VBox box, ListView<String> list) {}

    /** Показывает диалог фильтра и возвращает новый фильтр, или {@code null} при отмене. */
    public static WorkerFilter show(Window owner, WorkerFilter current, List<Worker> allWorkers) {
        WorkerFilter[] result = {null};

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(I18n.t("filter.title"));
        stage.setResizable(true);

        WorkerFilter init = current != null ? current : new WorkerFilter();

        TextField nameField = new TextField(init.name);

        TextField idMinField = numField(init.idMin);
        TextField idMaxField = numField(init.idMax);

        TextField salaryMinField = numField(init.salaryMin);
        TextField salaryMaxField = numField(init.salaryMax);

        TextField coordXMinField = numField(init.coordXMin);
        TextField coordXMaxField = numField(init.coordXMax);

        TextField coordYMinField = numField(init.coordYMin);
        TextField coordYMaxField = numField(init.coordYMax);

        DatePicker dateMinPicker = new DatePicker(init.dateMin);
        DatePicker dateMaxPicker = new DatePicker(init.dateMax);
        dateMinPicker.setPrefWidth(140);
        dateMaxPicker.setPrefWidth(140);

        Map<Position, CheckBox> posBoxes = new LinkedHashMap<>();
        for (Position p : Position.values()) {
            CheckBox cb = new CheckBox(p.name());
            cb.setSelected(init.positions.contains(p));
            posBoxes.put(p, cb);
        }
        HBox posRow = new HBox(10);
        posRow.getChildren().addAll(posBoxes.values());
        posRow.setAlignment(Pos.CENTER_LEFT);

        Map<Status, CheckBox> statusBoxes = new LinkedHashMap<>();
        for (Status s : Status.values()) {
            CheckBox cb = new CheckBox(s.name());
            cb.setSelected(init.statuses.contains(s));
            statusBoxes.put(s, cb);
        }
        VBox statusCol = new VBox(4);
        statusCol.getChildren().addAll(statusBoxes.values());

        List<String> allOrgs = allWorkers.stream()
                .map(w -> w.getOrganization() != null ? w.getOrganization().getFullName() : null)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        ListBox orgLB = makeListBox(allOrgs, init.organizations, I18n.t("filter.orgSearch"), 100);

        List<String> allOwners = allWorkers.stream()
                .map(Worker::getOwnerLogin)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        ListBox ownerLB = makeListBox(allOwners, init.owners, I18n.t("filter.ownerSearch"), 100);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        int row = 0;
        grid.add(lbl("filter.name"), 0, row); grid.add(nameField, 1, row++);
        grid.add(lbl("filter.idMin"), 0, row); grid.add(rangebox(idMinField, idMaxField), 1, row++);
        grid.add(lbl("filter.salaryMin"), 0, row); grid.add(rangebox(salaryMinField, salaryMaxField), 1, row++);
        grid.add(lbl("filter.coordX"), 0, row); grid.add(rangebox(coordXMinField, coordXMaxField), 1, row++);
        grid.add(lbl("filter.coordY"), 0, row); grid.add(rangebox(coordYMinField, coordYMaxField), 1, row++);
        grid.add(lbl("filter.dateMin"), 0, row); grid.add(rangebox(dateMinPicker, dateMaxPicker), 1, row++);
        grid.add(lbl("filter.position"), 0, row); grid.add(posRow, 1, row++);
        grid.add(lbl("filter.status"), 0, row); grid.add(statusCol, 1, row++);
        grid.add(lbl("filter.organization"), 0, row); grid.add(orgLB.box(), 1, row++);
        grid.add(lbl("filter.owner"), 0, row); grid.add(ownerLB.box(), 1, row);

        ColumnConstraints cc0 = new ColumnConstraints();
        cc0.setMinWidth(120);
        ColumnConstraints cc1 = new ColumnConstraints();
        cc1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(cc0, cc1);

        Button applyBtn = new Button(I18n.t("filter.apply"));
        Button resetBtn = new Button(I18n.t("filter.reset"));
        Button cancelBtn = new Button(I18n.t("form.cancel"));

        applyBtn.setDefaultButton(true);
        applyBtn.setOnAction(e -> {
            WorkerFilter f = new WorkerFilter();
            f.name = nameField.getText().trim();
            f.idMin = parseInt(idMinField.getText());
            f.idMax = parseInt(idMaxField.getText());
            f.salaryMin = parseDouble(salaryMinField.getText());
            f.salaryMax = parseDouble(salaryMaxField.getText());
            f.coordXMin = parseDouble(coordXMinField.getText());
            f.coordXMax = parseDouble(coordXMaxField.getText());
            f.coordYMin = parseDouble(coordYMinField.getText());
            f.coordYMax = parseDouble(coordYMaxField.getText());
            f.dateMin = dateMinPicker.getValue();
            f.dateMax = dateMaxPicker.getValue();
            posBoxes.forEach((p, cb)  -> { if (cb.isSelected()) f.positions.add(p); });
            statusBoxes.forEach((s, cb) -> { if (cb.isSelected()) f.statuses.add(s); });
            f.organizations.addAll(orgLB.list().getSelectionModel().getSelectedItems());
            f.owners.addAll(ownerLB.list().getSelectionModel().getSelectedItems());
            result[0] = f;
            stage.close();
        });
        resetBtn.setOnAction(e  -> { result[0] = new WorkerFilter(); stage.close(); });
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(8, applyBtn, resetBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 12, 10, 12));

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox root = new VBox(scroll, buttons);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        stage.setScene(new Scene(root, 540, 680));
        stage.showAndWait();
        return result[0];
    }

    private static ListBox makeListBox(List<String> items, Set<String> preselected,
                                       String prompt, double height) {
        ObservableList<String> src = FXCollections.observableArrayList(items);
        FilteredList<String> filtered = new FilteredList<>(src, s -> true);

        TextField search = new TextField();
        search.setPromptText(prompt);
        search.textProperty().addListener((obs, old, text) -> {
            String lower = text == null ? "" : text.trim().toLowerCase();
            filtered.setPredicate(s -> lower.isEmpty() || s.toLowerCase().contains(lower));
        });

        ListView<String> list = new ListView<>(filtered);
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setPrefHeight(height);

        for (String s : preselected) {
            int idx = filtered.indexOf(s);
            if (idx >= 0) list.getSelectionModel().select(idx);
        }

        return new ListBox(new VBox(4, search, list), list);
    }

    private static TextField numField(Number val) {
        TextField f = new TextField(val != null ? String.valueOf(val) : "");
        f.setPrefWidth(90);
        return f;
    }

    private static HBox rangebox(Control from, Control to) {
        HBox box = new HBox(6, from, new Label("—"), to);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Label lbl(String key) {
        Label l = new Label(I18n.t(key) + ":");
        l.setAlignment(Pos.CENTER_RIGHT);
        return l;
    }

    private static Integer parseInt(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; }
    }
}
