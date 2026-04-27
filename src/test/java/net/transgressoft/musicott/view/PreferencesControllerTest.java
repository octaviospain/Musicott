package net.transgressoft.musicott.view;

import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.musicott.config.SettingsRepository;
import net.transgressoft.musicott.services.lastfm.LastFmService;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * @author Octavio Calleya
 */
@ExtendWith (ApplicationExtension.class)
class PreferencesControllerTest {

    PreferencesController preferencesController;
    net.transgressoft.musicott.config.SettingsRepository settingsRepository;
    LastFmService lastFmService;
    BooleanProperty loggedInProperty;
    ErrorPresenter errorPresenter;
    Stage stage;

    @Start
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        lastFmService = mock(LastFmService.class);
        loggedInProperty = new SimpleBooleanProperty(false);
        when(lastFmService.loggedInProperty()).thenReturn(loggedInProperty);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PreferencesController.fxml"));
        loader.load();
        preferencesController = loader.getController();
        preferencesController.setLastFmService(lastFmService);
        preferencesController.setStageSupplier(() -> stage);
    }

    @BeforeEach
    void beforeEach() {
        errorPresenter = mock(ErrorPresenter.class);
        settingsRepository = mock(SettingsRepository.class);
        when(settingsRepository.getAcceptedAudioFileExtensions()).thenReturn(Set.of(AudioFileType.MP3));
        when(settingsRepository.getItunesImportMetadataPolicy()).thenReturn(true);
        when(settingsRepository.getItunesImportHoldPlayCountPolicy()).thenReturn(true);
        when(settingsRepository.getItunesImportWriteMetadataPolicy()).thenReturn(true);

        preferencesController.setSettingsRepository(settingsRepository);
        preferencesController.setErrorPresenter(errorPresenter);
    }

    @Test
    void lastFmSuccessfulAuthenticationAndLogoutTest(FxRobot fxRobot) {
        loggedInProperty.set(false);
        when(lastFmService.logIn()).thenReturn(CompletableFuture.completedFuture(true));

        preferencesController.show();
        waitForFxEvents();

        fxRobot.clickOn("#lastFmTab");

        verifyThat("#lastFmAuthorizationButton", hasText("Grant permission"));

        fxRobot.clickOn("#lastFmAuthorizationButton");
        waitForFxEvents();

        verifyNoInteractions(errorPresenter);
        verifyThat("#lastFmAuthorizationButton", hasText("Logout"));

        fxRobot.clickOn("#lastFmAuthorizationButton");
        waitForFxEvents();

        verifyNoInteractions(errorPresenter);
        verifyThat("#lastFmAuthorizationButton", hasText("Grant permission"));
    }

    @Test
    void lastFmFailedAuthenticationTest(FxRobot fxRobot) {
        loggedInProperty.set(false);
        when(lastFmService.logIn()).thenReturn(CompletableFuture.completedFuture(false));

        preferencesController.show();
        waitForFxEvents();

        fxRobot.clickOn("#lastFmTab");

        verifyThat("#lastFmAuthorizationButton", hasText("Grant permission"));

        fxRobot.clickOn("#lastFmAuthorizationButton");

        verifyNoInteractions(errorPresenter);
        verifyThat("#lastFmAuthorizationButton", hasText("Grant permission"));
    }

    @Test
    void lastFmFailedAuthenticationWithExceptionTest(FxRobot fxRobot) {
        loggedInProperty.set(false);
        when(lastFmService.logIn()).thenReturn(CompletableFuture.failedFuture(new CompletionException("Failed!", null)));

        preferencesController.show();
        waitForFxEvents();

        fxRobot.clickOn("#lastFmTab");

        verifyThat("#lastFmAuthorizationButton", hasText("Grant permission"));

        fxRobot.clickOn("#lastFmAuthorizationButton");

        verify(errorPresenter).show(eq("LastFM authentication failed"), any(CompletionException.class));
        verifyThat("#lastFmAuthorizationButton", hasText("Grant permission"));
    }

    @Test
    @DisplayName("closes preferences dialog without saving when escape is pressed")
    void closesPreferencesDialogWithoutSavingWhenEscapeIsPressed(FxRobot fxRobot) {
        Platform.runLater(() -> preferencesController.show());
        waitForFxEvents();

        Stage prefStage = (Stage) ReflectionTestUtils.getField(preferencesController, "stage");
        assertThat(prefStage).isNotNull();
        assertThat(prefStage.isShowing()).isTrue();

        // Fire ESC directly through the scene's key handler — exercises the same code path as a robot key press
        // without depending on focus arbitration on a headless toolkit.
        Scene scene = prefStage.getScene();
        Platform.runLater(() -> scene.getOnKeyPressed().handle(
            new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE,
                false, false, false, false)));
        waitForFxEvents();

        assertThat(prefStage.isShowing()).isFalse();
        // Settings are committed only via okButton.saveAndClose(); ESC must not trigger any setter call.
        verify(settingsRepository, never()).setAcceptedAudioFileExtensions(anyList());
    }
}