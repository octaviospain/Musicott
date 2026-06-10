package net.transgressoft.musicott.view.custom.alerts;

import net.transgressoft.musicott.view.custom.ApplicationImage;

import javafx.scene.control.Dialog;
import javafx.stage.Stage;

/**
 * Utility that attaches the application icon to any {@link Dialog} window once the
 * window becomes available. Because {@code getDialogPane().getScene().getWindow()}
 * returns {@code null} at construction time, the icon is wired via
 * {@link Dialog#setOnShown(javafx.event.EventHandler)} so it is applied the first
 * time the dialog is shown.
 *
 * <p>Use {@link #attachAppIconOnShow(Dialog)} in every dialog and alert constructor
 * to avoid duplicating the on-show hook logic.
 *
 * @author Octavio Calleya
 */
final class DialogIconHelper {

    private DialogIconHelper() {}

    /**
     * Registers an on-show hook on the given dialog that adds
     * {@link ApplicationImage#APP_ICON} to the dialog window's icon list the first
     * time the dialog is shown. Re-shows are guarded against duplicate icon additions.
     *
     * @param dialog the dialog to attach the icon to
     */
    static void attachAppIconOnShow(Dialog<?> dialog) {
        dialog.setOnShown(e -> {
            var window = dialog.getDialogPane().getScene().getWindow();
            if (window instanceof Stage stage) {
                var icon = ApplicationImage.APP_ICON.get();
                if (!stage.getIcons().contains(icon)) {
                    stage.getIcons().add(icon);
                }
            }
        });
    }
}
