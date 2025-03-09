package net.transgressoft.musicott.services;

import javafx.application.HostServices;
import org.springframework.stereotype.Component;

/**
 * @author Octavio Calleya
 */
@Component("webRedirectionService")
public class SimpleWebRedirectionService {

    public static final String GITHUB_URL = "https://github.com/octaviospain/Musicott/issues";

    private final HostServices hostServices;

    public SimpleWebRedirectionService(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public void navigateToGithub() {
        hostServices.showDocument(GITHUB_URL);
    }
}
