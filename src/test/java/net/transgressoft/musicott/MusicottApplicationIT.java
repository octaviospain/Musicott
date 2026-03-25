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
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(classes = MusicottApplication.class)
@ActiveProfiles("e2e")
@ExtendWith(ApplicationExtension.class)
class MusicottApplicationIT {

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
		WaitForAsyncUtils.waitForFxEvents();
	}

	@AfterEach
	void tearDown() {
		Platform.runLater(() -> {
			if (testStage.isShowing()) {
				testStage.hide();
			}
		});
		WaitForAsyncUtils.waitForFxEvents();
	}

	@Test
	@DisplayName("Application context loads with root border pane")
	void applicationStartupTest(FxRobot fxRobot) {
		assertThat(fxRobot.lookup("#rootBorderPane").tryQuery()).isPresent();
	}
}