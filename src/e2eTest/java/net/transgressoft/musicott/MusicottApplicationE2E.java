package net.transgressoft.musicott;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxWeaver;
import net.transgressoft.musicott.config.ApplicationPaths;
import net.transgressoft.musicott.view.MainController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = {MusicottApplication.class, MusicottApplicationE2E.E2eTestPaths.class})
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MusicottApplicationE2E {

	@TestConfiguration
	static class E2eTestPaths {

		@Bean
		@Primary
		public ApplicationPaths applicationPaths() throws IOException {
			Path tempDir = Files.createTempDirectory("musicott-e2e");
			tempDir.toFile().deleteOnExit();
			return new ApplicationPaths(
					tempDir.resolve("audioItems.json"),
					tempDir.resolve("playlists.json"),
					tempDir.resolve("waveforms.json")
			);
		}
	}

	static Stage testStage;

	@Autowired
	FxWeaver fxWeaver;

	@BeforeAll
	static void beforeAll() throws Exception {
		testStage = FxToolkit.registerPrimaryStage();
	}

	@BeforeEach
	void beforeEach() throws Exception {
		// FxToolkit.setupFixture blocks the test thread until the FX runnable finishes,
		// guaranteeing the scene graph is attached before the test queries it. The
		// previous Platform.runLater + waitForFxEvents pair only flushed pending FX
		// events; Stage.show on macOS Monocle returns before children become queryable,
		// so #rootBorderPane lookups intermittently failed there.
		FxToolkit.setupFixture(() -> {
			Scene scene = new Scene(fxWeaver.loadView(MainController.class), 1200, 800);
			testStage.setScene(scene);
			testStage.show();
		});
	}

	@AfterEach
	void tearDown() throws Exception {
		FxToolkit.setupFixture(() -> {
			if (testStage.isShowing()) {
				testStage.hide();
			}
			testStage.setScene(null);
		});
	}

	@AfterAll
	static void afterAll() throws Exception {
		FxToolkit.cleanupStages();
	}

	@Test
	@DisplayName("Application context loads with root border pane and no splash stage exists")
	void applicationStartupTest(FxRobot fxRobot) {
		assertThat(fxRobot.lookup("#rootBorderPane").tryQuery()).isPresent();

		// Defense in depth: gradle/test-suites.gradle sets -Dmusicott.splash.disabled=true
		// for every test JVM. MusicottApplication.start() takes the synchronous bypass
		// branch in that mode and never constructs a SplashController. A regression
		// that removes the bypass property would surface here as a showing window whose
		// scene root carries the splash-root style class. The TestFX harness installs
		// its own UNDECORATED primary stage, so we cannot discriminate by StageStyle —
		// splash-root is the only splash-specific signal in the scene graph.
		long splashWindowCount = Window.getWindows().stream()
				.map(Window::getScene)
				.filter(Objects::nonNull)
				.map(Scene::getRoot)
				.filter(Objects::nonNull)
				.filter(root -> root.getStyleClass().contains("splash-root"))
				.count();

		assertThat(splashWindowCount)
				.as("no SplashController scene may be open under the bypass property")
				.isEqualTo(0L);

		// Also confirm the bypass property arrived in this JVM — protects against a
		// silent revert of gradle/test-suites.gradle.
		assertThat(System.getProperty("musicott.splash.disabled"))
				.as("musicott.splash.disabled must reach the e2eTest JVM")
				.isEqualTo("true");
	}
}
