package net.transgressoft.musicott.view.custom.alerts;

import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 */
public class MultipleEditionConfirmationAlert extends ApplicationAlertBase {
    
    public MultipleEditionConfirmationAlert(Stage stage) {
        super(AlertType.CONFIRMATION);
        setContentText("");
        setHeaderText("Are you sure you want to edit multiple files?");
        initOwner(stage);
    }
}
