package net.transgressoft.musicott.view.custom.alerts;

import net.transgressoft.musicott.services.SimpleWebRedirectionService;
import net.transgressoft.musicott.view.custom.ApplicationImage;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;

import static net.transgressoft.musicott.services.SimpleWebRedirectionService.GITHUB_URL;

/**
 * @author Octavio Calleya
 */
public class AboutWindowAlert extends ApplicationAlertBase {

    private static final String ABOUT_MUSICOTT_FIRST_LINE = "Version 0.11.\nCopyright © 2015-2020\nOctavio Calleya.";

    private static final String ABOUT_MUSICOTT_SECOND_LINE = "\nLicensed under GNU GPLv3. This product includes\0"
        + "software developed by other open source projects.";

    public AboutWindowAlert(SimpleWebRedirectionService webRedirectionService) {
        super(AlertType.INFORMATION);

        Label aboutLabel1 = new Label(ABOUT_MUSICOTT_FIRST_LINE);
        Label aboutLabel2 = new Label(ABOUT_MUSICOTT_SECOND_LINE);
        Hyperlink githubLink = new Hyperlink(GITHUB_URL);
        githubLink.setOnAction(event -> webRedirectionService.navigateToGithub());
        FlowPane flowPane = new FlowPane();
        flowPane.getChildren().addAll(aboutLabel1, githubLink, aboutLabel2);
        getDialogPane().contentProperty().set(flowPane);
        setGraphic(new ImageView(ApplicationImage.ABOUT_IMAGE.get()));

        setContentText("");
        setTitle("About Musicott");
    }
}
