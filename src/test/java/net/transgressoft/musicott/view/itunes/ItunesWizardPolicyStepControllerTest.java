package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.commons.music.itunes.ItunesImportPolicy;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Unit test for {@link ItunesWizardPolicyStepController} validating the metadata-source
 * policy step UI rendering, the all-on defaults, the {@link ItunesImportPolicy} produced
 * from the current control state, and the draft write-back on {@code onExit} using a
 * standalone pattern (no Spring context required).
 *
 * @author Octavio Calleya
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("ItunesWizardPolicyStepController")
class ItunesWizardPolicyStepControllerTest {

    ItunesWizardPolicyStepController controller;
    ItunesImportDraft draft;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItunesWizardPolicyStep.fxml"));
        loader.setControllerFactory(type -> new ItunesWizardPolicyStepController());
        Parent root = loader.load();
        controller = loader.getController();
        draft = new ItunesImportDraft();
        controller.bind(draft);
        stage.setScene(new Scene(root));
        stage.show();
        waitForFxEvents();
    }

    @Test
    @DisplayName("ItunesWizardPolicyStepController renders the metadata source radios and the two toggle switches")
    void rendersMetadataSourceRadiosAndTwoToggleSwitches() {
        verifyThat("#useFileMetadataRadio", isVisible());
        verifyThat("#useItunesDbRadio", isVisible());
        verifyThat("#holdPlayCountSwitch", isVisible());
        verifyThat("#writeMetadataSwitch", isVisible());
    }

    @Test
    @DisplayName("ItunesWizardPolicyStepController applies all-on defaults at initialize")
    void appliesAllOnDefaultsAtInitialize() {
        assertThat(controller.useFileMetadataRadio.isSelected()).isTrue();
        assertThat(controller.useItunesDbRadio.isSelected()).isFalse();
        assertThat(controller.holdPlayCountSwitch.isSelected()).isTrue();
        assertThat(controller.writeMetadataSwitch.isSelected()).isTrue();
    }

    @Test
    @DisplayName("ItunesWizardPolicyStepController is always valid")
    void isAlwaysValid() {
        assertThat(controller.invalidProperty().get()).isFalse();
    }

    @Test
    @DisplayName("ItunesWizardPolicyStepController returns ItunesImportPolicy with all-true booleans for default state")
    void returnsItunesImportPolicyWithAllTrueBooleansForDefaultState() {
        ItunesImportPolicy policy = controller.getPolicy();

        assertThat(policy.getUseFileMetadata()).isTrue();
        assertThat(policy.getHoldPlayCount()).isTrue();
        assertThat(policy.getWriteMetadata()).isTrue();
        assertThat(policy.getAcceptedFileTypes()).containsExactlyInAnyOrder(AudioFileType.values());
    }

    @Test
    @DisplayName("ItunesWizardPolicyStepController returns ItunesImportPolicy reflecting user changes")
    void returnsItunesImportPolicyReflectingUserChanges() {
        Platform.runLater(() -> {
            controller.useItunesDbRadio.setSelected(true);
            controller.writeMetadataSwitch.setSelected(false);
        });
        waitForFxEvents();

        ItunesImportPolicy policy = controller.getPolicy();

        assertThat(policy.getUseFileMetadata()).isFalse();
        assertThat(policy.getHoldPlayCount()).isTrue();
        assertThat(policy.getWriteMetadata()).isFalse();
        assertThat(policy.getAcceptedFileTypes()).containsExactlyInAnyOrder(AudioFileType.values());
    }

    @Test
    @DisplayName("ItunesWizardPolicyStepController writes control state into draft on onExit")
    void writesControlStateIntoDraftOnOnExit() {
        Platform.runLater(() -> {
            controller.useItunesDbRadio.setSelected(true);
            controller.holdPlayCountSwitch.setSelected(false);
        });
        waitForFxEvents();

        controller.onExit();

        assertThat(draft.useFileMetadata).isFalse();
        assertThat(draft.holdPlayCount).isFalse();
        assertThat(draft.writeMetadata).isTrue();
    }

    @Test
    @DisplayName("ItunesWizardPolicyStepController restores controls from draft on onEnter")
    void restoresControlsFromDraftOnOnEnter() {
        draft.useFileMetadata = false;
        draft.holdPlayCount = false;
        draft.writeMetadata = false;

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        assertThat(controller.useFileMetadataRadio.isSelected()).isFalse();
        assertThat(controller.useItunesDbRadio.isSelected()).isTrue();
        assertThat(controller.holdPlayCountSwitch.isSelected()).isFalse();
        assertThat(controller.writeMetadataSwitch.isSelected()).isFalse();
    }
}
