package itmo.lab.client.gui.dialogs;

import itmo.lab.client.gui.i18n.I18n;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

/** Диалог ввода числового ID (целое число ≥ 1). */
public class IdInputDialog {

    private IdInputDialog() {}

    /** Показывает диалог и возвращает введённый ID, или {@code null} при отмене. */
    public static Integer show(Window owner, String titleKey, String promptKey) {
        while (true) {
            TextInputDialog dlg = new TextInputDialog("1");
            dlg.initOwner(owner);
            dlg.setTitle(I18n.t(titleKey));
            dlg.setHeaderText(null);
            dlg.setContentText(I18n.t(promptKey));

            Optional<String> result = dlg.showAndWait();
            if (result.isEmpty()) return null;

            try {
                int val = Integer.parseInt(result.get().trim());
                if (val >= 1) return val;
            } catch (NumberFormatException ignored) {}

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    I18n.t("dlg.idPrompt") + " — введите целое число ≥ 1",
                    javafx.scene.control.ButtonType.OK);
            alert.setTitle(I18n.t("dlg.error"));
            alert.setHeaderText(null);
            alert.initOwner(owner);
            alert.showAndWait();
        }
    }
}
