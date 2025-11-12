package net.transgressoft.musicott.view.custom.alerts;

import javafx.scene.control.Alert;
import net.transgressoft.musicott.services.SimpleWebRedirectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Octavio Calleya
 */
@Component
public class AlertFactory {

    private final SimpleWebRedirectionService webRedirectionService;

    @Autowired
    public AlertFactory(SimpleWebRedirectionService webRedirectionService) {
        this.webRedirectionService = webRedirectionService;
    }

    public Alert aboutWindowAlert() {
        return new AboutWindowAlert(webRedirectionService);
    }

    public Alert importConfirmationAlert(int importSize) {
        return new ImportConfirmationAlert(importSize);
    }

    public Alert emptyImportAlert() {
        return new EmptyImportAlert();
    }
}
