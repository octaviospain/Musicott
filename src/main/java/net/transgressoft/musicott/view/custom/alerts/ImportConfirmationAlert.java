package net.transgressoft.musicott.view.custom.alerts;

/**
 * @author Octavio Calleya
 */
public class ImportConfirmationAlert extends ApplicationAlertBase {

    private static final String CONFIRMATION_TEXT = "Import %d files?";

    public ImportConfirmationAlert(int importSize) {
        super(AlertType.CONFIRMATION);
        setContentText(String.format(CONFIRMATION_TEXT, importSize));
    }
}
