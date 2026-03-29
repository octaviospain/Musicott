package net.transgressoft.musicott;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;
import net.transgressoft.musicott.view.MainController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

@SpringBootTest(classes = MusicottApplication.class)
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
class MusicottApplicationE2E {

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