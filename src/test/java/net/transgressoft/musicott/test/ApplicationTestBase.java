package net.transgressoft.musicott.test;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

public abstract class ApplicationTestBase<T extends Parent> extends ApplicationTest {

    protected static Stage testStage;

    @BeforeAll
    static void beforeAll() throws Exception {
        testStage = FxToolkit.registerPrimaryStage();
    }

    @BeforeEach
    protected void beforeEach() {
        Platform.runLater(() -> {
            Scene scene = new Scene(javaFxComponent(), 800, 600);
            testStage.setScene(scene);
            testStage.show();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterAll
    static void afterAll() throws Exception {
        Platform.runLater(() -> {
            if (testStage != null && testStage.isShowing()) {
                testStage.close();
            }
        });
        FxToolkit.cleanupStages();
        WaitForAsyncUtils.waitForFxEvents();
    }

    protected abstract T javaFxComponent();

    @AfterEach
    void tearDown() {
        Platform.runLater(() -> {
            if (testStage.isShowing()) {
                testStage.hide();
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }
}
