package itmo.lab.client.gui.dialogs;

import itmo.lab.client.gui.i18n.I18n;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Диалог ввода даты и времени в формате {@code yyyy-MM-dd HH:mm}. */
public class DateInputDialog {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateInputDialog() {}

    /** Показывает диалог и возвращает введённую дату, или {@code null} при отмене. */
    public static String show(Window owner, String titleKey) {
        while (true) {
            TextInputDialog dlg = new TextInputDialog(LocalDateTime.now().format(FMT));
            dlg.initOwner(owner);
            dlg.setTitle(I18n.t(titleKey));
            dlg.setHeaderText(null);
            dlg.setContentText(I18n.t("dlg.datePrompt"));

            Optional<String> result = dlg.showAndWait();
            if (result.isEmpty()) return null;

            String text = result.get().trim();
            try {
                LocalDateTime.parse(text, FMT);
                return text;
            } catch (DateTimeParseException e) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR,
                        I18n.t("form.errorDate"),
                        javafx.scene.control.ButtonType.OK);
                alert.setTitle(I18n.t("dlg.error"));
                alert.setHeaderText(null);
                alert.initOwner(owner);
                alert.showAndWait();
            }
        }
    }
}
