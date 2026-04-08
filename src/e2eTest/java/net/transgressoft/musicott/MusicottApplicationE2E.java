package net.transgressoft.musicott;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
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
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@SpringBootTest(classes = {MusicottApplication.class, MusicottApplicationE2E.E2eTestPaths.class})
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
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
	void beforeEach() {
		Platform.runLater(() -> {
			Scene scene = new Scene(fxWeaver.loadView(MainController.class), 1200, 800);
			testStage.setScene(scene);
			testStage.show();
		});
		waitForFxEvents();
	}

	@AfterEach
	void tearDown() {
		Platform.runLater(() -> {
			if (testStage.isShowing()) {
				testStage.hide();
			}
		});
		waitForFxEvents();
	}

	@Test
	@DisplayName("Application context loads with root border pane")
	void applicationStartupTest(FxRobot fxRobot) {
		assertThat(fxRobot.lookup("#rootBorderPane").tryQuery()).isPresent();
	}
}