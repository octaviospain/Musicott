package net.transgressoft.musicott;

import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.transgressoft.musicott.config.ApplicationPaths;
import net.transgressoft.musicott.view.MainController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = {MusicottApplication.class, MusicottApplicationE2E.E2eTestPaths.class})
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
// FXMLLoader.load through FxWeaver throws IllegalAccessError on macOS Monocle when
// JavaFX is loaded as platform modules from Liberica jdk+fx — likely a module/reflection
// boundary not granted by JavaFX 24's macOS native bridge. Tracked in #17.
@DisabledOnOs(value = OS.MAC, disabledReason = "FxWeaver/FXMLLoader IllegalAccessError on macOS Monocle — see #17")
class MusicottApplicationE2E {

	@TestConfiguration
	static class E2eTestPaths {

		@Bean
		@Primary
		public ApplicationPaths applicationPaths() throws IOException {
			Path tempDir = Files.createTempDirectory("musicott-e2e");
			tempDir.toFile().deleteOnExit();
			return new ApplicationPaths(
					tempDir.resolve("settings.json"),
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
		});
	}

	@Test
	@DisplayName("Application context loads with root border pane")
	void applicationStartupTest(FxRobot fxRobot) {
		assertThat(fxRobot.lookup("#rootBorderPane").tryQuery()).isPresent();
	}
}