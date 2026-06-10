package net.transgressoft.musicott.view.custom.alerts;

/**
 * Confirmation dialog shown before permanently deleting one or more audio items from the library.
 *
 * <p>Extends {@link ApplicationAlertBase} so the application icon and dark stylesheet are applied
 * automatically, matching the visual treatment of all other application dialogs.
 *
 * @author Octavio Calleya
 */
public class DeleteAudioItemsConfirmationAlert extends ApplicationAlertBase {

    public DeleteAudioItemsConfirmationAlert() {
        super(AlertType.CONFIRMATION);
        setContentText("Delete selected track(s)?");
    }
}
