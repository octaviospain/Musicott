package net.transgressoft.musicott.view.custom.alerts;

import net.transgressoft.musicott.services.SimpleWebRedirectionService;
import net.transgressoft.musicott.view.custom.ApplicationImage;

import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.info.BuildProperties;

import java.time.Year;

import static net.transgressoft.musicott.services.SimpleWebRedirectionService.GITHUB_URL;

/**
 * @author Octavio Calleya
 */
public class AboutWindowAlert extends ApplicationAlertBase {

    private static final String ABOUT_MUSICOTT_SECOND_LINE = "\nLicensed under GNU GPLv3. This product includes "
        + "software developed by other open source projects.";

    /**
     * @param webRedirectionService service used by the GitHub hyperlink action
     * @param buildProperties Spring's BuildProperties bean — may be null when the
     *                        bootBuildInfo task did not run (e.g. an IDE invocation
     *                        that bypassed Gradle resource processing). Falls back
     *                        to "dev" as the displayed version string.
     */
    public AboutWindowAlert(SimpleWebRedirectionService webRedirectionService, BuildProperties buildProperties) {
        super(AlertType.INFORMATION);
        setHeaderText(null);

        String version = (buildProperties != null) ? buildProperties.getVersion() : "dev";
        String firstLine = "Version " + version + ".\nCopyright © 2015-"
            + Year.now().getValue() + "\nOctavio Calleya.";

        Label aboutLabel1 = new Label(firstLine);
        Label aboutLabel2 = new Label(ABOUT_MUSICOTT_SECOND_LINE);
        Hyperlink githubLink = new Hyperlink(GITHUB_URL);
        githubLink.setOnAction(event -> webRedirectionService.navigateToGithub());
        FlowPane flowPane = new FlowPane();
        flowPane.getChildren().addAll(aboutLabel1, githubLink, aboutLabel2);
        ImageView logo = new ImageView(ApplicationImage.ABOUT_IMAGE.get());
        VBox content = new VBox(logo, flowPane);
        content.setAlignment(Pos.TOP_CENTER);
        getDialogPane().contentProperty().set(content);
        setGraphic(null);

        setTitle("About Musicott");
        setOnShown(event -> {
            if (getDialogPane().getScene().getWindow() instanceof Stage stage) {
                stage.getIcons().add(ApplicationImage.APP_ICON.get());
            }
        });
    }
}
