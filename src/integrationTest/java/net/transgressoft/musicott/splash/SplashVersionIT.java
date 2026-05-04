package net.transgressoft.musicott.splash;

import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test verifying the splash version label is populated from
 * {@code /META-INF/build-info.properties} via {@link BuildVersionReader}
 * when the splash is constructed under a live Spring context. The splash itself
 * does not consume the {@link BuildProperties} bean — it reads the classpath
 * resource directly so the version label is available before Spring boots in
 * production. The bean is provided here only for parity with the rest of the
 * Spring-aware test harness.
 *
 * <p>The test lives in the {@code splash} package so it can read the package-private
 * {@code versionLabel} field on {@link SplashController} directly, mirroring the
 * existing {@link SplashControllerUIT} placement.
 */
@JavaFxSpringTest(classes = SplashVersionITConfiguration.class)
@DisplayName("SplashVersion")
class SplashVersionIT extends ApplicationTestBase<Pane> {

    SplashController splash;

    @Override
    protected Pane javaFxComponent() {
        return new Pane();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        Platform.runLater(() -> {
            splash = new SplashController();
            splash.show();
        });
        waitForFxEvents();
    }

    @AfterEach
    void closeSplash() {
        if (splash != null) {
            Platform.runLater(splash::hide);
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("renders a non-blank version label populated from classpath build-info.properties")
    void rendersANonBlankVersionLabelPopulatedFromClasspathBuildInfoProperties() {
        // The splash reads via BuildVersionReader (classpath read), not via the Spring
        // BuildProperties bean — the splash exists before Spring boots in production.
        // Under the IT harness Spring IS up, but the splash still uses the classpath
        // path. The label must be non-blank because gradle integrationTest runs
        // processResources first.
        assertThat(splash.versionLabel.getText()).isNotBlank();
        assertThat(splash.versionLabel.getText()).isNotEqualTo("dev");
    }
}

@JavaFxSpringTestConfiguration
class SplashVersionITConfiguration {

    @Bean
    BuildProperties buildProperties() {
        // Provided so the Spring context boots cleanly. The splash itself does NOT
        // consume this bean — it reads via BuildVersionReader (classpath). This bean
        // is here only for parity with AboutWindowAlertIT precedent.
        Properties props = new Properties();
        props.setProperty("group", "net.transgressoft");
        props.setProperty("artifact", "musicott");
        props.setProperty("name", "musicott");
        props.setProperty("version", "1.0.0-test");
        return new BuildProperties(props);
    }
}
