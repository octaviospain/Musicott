package net.transgressoft.musicott.view.custom.alerts;

import net.transgressoft.musicott.services.SimpleWebRedirectionService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.api.FxRobot;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test verifying the full rendered state of {@link AboutWindowAlert}: the logo
 * sits on top of a FlowPane carrying the version line, GitHub hyperlink, and license line; no
 * stray "Message" content-text label leaks from {@link Alert.AlertType#INFORMATION}; the version
 * string is sourced from Spring's {@link BuildProperties} bean; and the dialog window carries
 * the application icon.
 */
@JavaFxSpringTest(classes = AboutWindowAlertITConfiguration.class)
@DisplayName("AboutWindowAlert")
class AboutWindowAlertIT extends ApplicationTestBase<Pane> {

    @Autowired
    AlertFactory alertFactory;

    Alert aboutAlert;

    @Override
    protected Pane javaFxComponent() {
        // The test stage needs some Parent as scene root; the alert renders in its own
        // owned window once initOwner(testStage) is set in beforeEach().
        return new Pane();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        Platform.runLater(() -> {
            aboutAlert = (Alert) alertFactory.aboutWindowAlert();
            aboutAlert.initOwner(testStage);
            aboutAlert.show();
        });
        waitForFxEvents();
    }

    @AfterEach
    void closeAlert() {
        if (aboutAlert != null) {
            Platform.runLater(aboutAlert::close);
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("renders the about dialog with logo on top, runtime version, and application icon")
    void rendersAboutDialogWithLogoOnTopRuntimeVersionAndApplicationIcon(FxRobot fxRobot) {
        DialogPane dialogPane = aboutAlert.getDialogPane();

        assertThat(dialogPane.getHeaderText()).isNull();
        assertThat(aboutAlert.getTitle()).isEqualTo("About Musicott");

        List<Label> labels = collectLabels(dialogPane, new ArrayList<>());
        assertThat(labels)
                .filteredOn(label -> "Message".equals(label.getText()))
                .as("dialog pane should not contain any Label with text == 'Message'")
                .isEmpty();

        assertThat(dialogPane.getContent()).isInstanceOf(VBox.class);
        VBox content = (VBox) dialogPane.getContent();
        assertThat(content.getChildren()).hasSize(2);
        assertThat(content.getChildren().get(0))
                .as("logo must be the first child so it renders on top of the text")
                .isInstanceOf(ImageView.class);
        assertThat(content.getChildren().get(1)).isInstanceOf(FlowPane.class);

        FlowPane flowPane = (FlowPane) content.getChildren().get(1);
        assertThat(flowPane.getChildren()).hasSize(3);
        assertThat(flowPane.getChildren().get(0)).isInstanceOf(Label.class);
        assertThat(flowPane.getChildren().get(1)).isInstanceOf(Hyperlink.class);
        assertThat(flowPane.getChildren().get(2)).isInstanceOf(Label.class);

        Label versionLabel = (Label) flowPane.getChildren().get(0);
        String expectedVersionPrefix = "Version 1.0.0-test.\nCopyright © 2015-" + Year.now().getValue();
        assertThat(versionLabel.getText()).startsWith(expectedVersionPrefix);
        assertThat(versionLabel.getText()).endsWith("\nOctavio Calleya.");

        Hyperlink githubLink = (Hyperlink) flowPane.getChildren().get(1);
        assertThat(githubLink.getText()).isEqualTo(SimpleWebRedirectionService.GITHUB_URL);

        Label licenseLabel = (Label) flowPane.getChildren().get(2);
        assertThat(licenseLabel.getText()).contains("includes software");

        Stage dialogStage = (Stage) dialogPane.getScene().getWindow();
        assertThat(dialogStage.getIcons())
                .as("about dialog window should carry the application icon")
                .isNotEmpty();
    }

    static List<Label> collectLabels(Parent root, List<Label> acc) {
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child instanceof Label label) {
                acc.add(label);
            } else if (child instanceof Parent parent) {
                collectLabels(parent, acc);
            }
        }
        return acc;
    }
}

@JavaFxSpringTestConfiguration
class AboutWindowAlertITConfiguration {

    @Bean
    SimpleWebRedirectionService webRedirectionService() {
        // Mock — the real constructor takes javafx.application.HostServices which is unavailable
        // under headless TestFX. The IT never clicks the hyperlink, so a Mockito mock is sufficient.
        // Pattern precedent: ErrorDialogControllerTest.java.
        return mock(SimpleWebRedirectionService.class);
    }

    @Bean
    BuildProperties buildProperties() {
        Properties props = new Properties();
        props.setProperty("group", "net.transgressoft");
        props.setProperty("artifact", "musicott");
        props.setProperty("name", "musicott");
        props.setProperty("version", "1.0.0-test");
        return new BuildProperties(props);
    }

    @Bean
    AlertFactory alertFactory(SimpleWebRedirectionService webRedirectionService,
                              ObjectProvider<BuildProperties> buildPropertiesProvider) {
        return new AlertFactory(webRedirectionService, buildPropertiesProvider);
    }
}
