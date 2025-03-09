package net.transgressoft.musicott.view.custom.alerts;

/**
 * @author Octavio Calleya
 */
public class EmptyImportAlert extends ApplicationAlertBase {

    public EmptyImportAlert() {
        super(AlertType.WARNING);
        setTitle("There are no valid files to import in the selected directory.");
        setHeaderText("No files found to import");
        setContentText("There are no valid files to import in the selected folder. Change the folder or the import options in preferences");
    }
}
