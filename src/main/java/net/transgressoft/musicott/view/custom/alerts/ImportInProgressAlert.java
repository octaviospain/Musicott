package net.transgressoft.musicott.view.custom.alerts;

/**
 * Warning alert shown when the user attempts to start a new import while another is already running.
 */
public class ImportInProgressAlert extends ApplicationAlertBase {

    public ImportInProgressAlert() {
        super(AlertType.WARNING);
        setTitle("Import in progress");
        setHeaderText("An import operation is already running");
        setContentText("Please wait for the current import to finish before starting a new one.");
    }
}
