package net.transgressoft.musicott.view;

import net.transgressoft.musicott.services.SimpleWebRedirectionService;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Collections;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.hasChild;
import static org.testfx.util.NodeQueryUtils.hasText;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * @author Octavio Calleya
 */
@ExtendWith ({ApplicationExtension.class, MockitoExtension.class})
class ErrorDialogControllerTest {

    @Mock
    SimpleWebRedirectionService webRedirectionService;

    ErrorDialogController errorDialogController;

    @Start
    void start(Stage stage) {}

    @BeforeEach
    void beforeEach() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ErrorDialogController.fxml"));
        loader.load();
        errorDialogController = loader.getController();
        errorDialogController.setStageSupplier(Stage::new);

        webRedirectionService = mock(SimpleWebRedirectionService.class);
        errorDialogController.setWebRedirectionService(webRedirectionService);
    }

    @Test
    void showWithContentTest(FxRobot fxRobot) throws Exception {
        errorDialogController.showWithExpandableContent("Bazinga!", "Dolor ipsum amet", Collections.singleton("Line message"));
        waitForFxEvents();

        verifyThat("#titleLabel", hasText("Bazinga!"));
        verifyThat("#contentLabel", hasText("Dolor ipsum amet"));
        verifyThat("#seeDetailsHyperlink", hasText("See details"));

        fxRobot.clickOn("#seeDetailsToggleButton");
        waitForFxEvents();

        verifyThat("#rootBorderPane", hasChild("#textAreaVBox"));

        fxRobot.clickOn("#seeDetailsToggleButton");
        waitForFxEvents();

        verifyThat("#rootBorderPane", not(hasChild("#textAreaVBox")));

        errorDialogController.show("Error!");
        waitForFxEvents();

        verifyThat("#titleLabel", hasText("Error!"));
        verifyThat("#contentBorderPane", not(hasChild("#contentLabel")));
        verifyThat("#contentBorderPane", not(hasChild("#seeDetailsHyperlink")));
        verifyThat("#contentBorderPane", not(hasChild("#detailsTextArea")));
        verifyThat("#reportInGithubHyperlink", hasText("Improve Musicott reporting this error on github."));

        fxRobot.clickOn("#reportInGithubHyperlink");
        waitForFxEvents();

        verify(webRedirectionService).navigateToGithub();

        fxRobot.clickOn("#okButton");
    }
}