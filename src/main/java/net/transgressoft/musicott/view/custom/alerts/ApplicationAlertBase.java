package net.transgressoft.musicott.view.custom.alerts;

import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;

/**
 * Base class for all application alerts. Centralises common dialog setup including
 * the dark dialog stylesheet, application-modal behaviour, and ESC-closes-dialog
 * keyboard handling for every {@link Alert} subclass.
 *
 * @author Octavio Calleya
 */
public abstract class ApplicationAlertBase extends Alert {

    private static final String STYLE = "/css/dialog.css";

    protected ApplicationAlertBase(AlertType alertType) {
        super(alertType);
        getDialogPane().getStylesheets().add(STYLE);
        initModality(Modality.APPLICATION_MODAL);
        // Filter on DialogPane (not the scene) — getDialogPane().getScene() is unreliable across the dialog lifecycle.
        getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, ke -> {
            if (ke.getCode() == KeyCode.ESCAPE) {
                close();
                ke.consume();
            }
        });
    }
}
