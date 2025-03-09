package net.transgressoft.musicott.view.custom.alerts;

import javafx.scene.control.Alert;
import javafx.stage.Modality;

/**
 * @author Octavio Calleya
 */
public abstract class ApplicationAlertBase extends Alert {

    private static final String STYLE = "/css/dialog.css";

    protected ApplicationAlertBase(AlertType alertType) {
        super(alertType);
        getDialogPane().getStylesheets().add(STYLE);
        initModality(Modality.APPLICATION_MODAL);
//        initOwner(new Stage());   // TODO
    }
}
