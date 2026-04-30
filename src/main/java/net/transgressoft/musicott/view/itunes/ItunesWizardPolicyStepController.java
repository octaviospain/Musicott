package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.commons.music.itunes.ItunesImportPolicy;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import net.rgielen.fxweaver.core.FxmlView;
import org.controlsfx.control.ToggleSwitch;
import org.springframework.stereotype.Controller;

import java.util.Set;

/**
 * Step controller for the metadata-source policy. The user picks between reading metadata
 * directly from the audio files and reusing the metadata recorded in the iTunes XML library,
 * and toggles whether existing play counts are preserved and whether the imported metadata
 * is written back to the audio files. The wizard's per-page validity gate is always
 * {@code false} on this step: every combination of choices is acceptable, and the controls
 * are pre-filled to the previously hardcoded import defaults.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/ItunesWizardPolicyStep.fxml")
@Controller
public class ItunesWizardPolicyStepController {

    private final SimpleBooleanProperty invalid = new SimpleBooleanProperty(this, "invalid", false);

    @FXML
    ToggleGroup metadataSourceGroup;
    @FXML
    RadioButton useFileMetadataRadio;
    @FXML
    RadioButton useItunesDbRadio;
    @FXML
    ToggleSwitch holdPlayCountSwitch;
    @FXML
    ToggleSwitch writeMetadataSwitch;

    private ItunesImportDraft draft;

    @FXML
    public void initialize() {
        useFileMetadataRadio.setSelected(true);
        holdPlayCountSwitch.setSelected(true);
        writeMetadataSwitch.setSelected(true);
    }

    /**
     * Binds this step controller to the supplied wizard draft. Called by the wizard owner
     * once per wizard session, before the first page is rendered.
     */
    void bind(ItunesImportDraft draft) {
        this.draft = draft;
    }

    /**
     * Called by the wizard owner when this step becomes the current page. Re-applies the
     * draft's three policy booleans to the controls so a Back-navigation restores any
     * previously selected combination.
     */
    void onEnter() {
        if (draft == null) {
            return;
        }
        if (draft.useFileMetadata) {
            useFileMetadataRadio.setSelected(true);
        }
        else {
            useItunesDbRadio.setSelected(true);
        }
        holdPlayCountSwitch.setSelected(draft.holdPlayCount);
        writeMetadataSwitch.setSelected(draft.writeMetadata);
    }

    /**
     * Called by the wizard owner when this step is leaving the current page (Next or Back).
     * Writes the three control states into the draft so the recap step and the wizard
     * owner see the user's current choices.
     */
    void onExit() {
        if (draft == null) {
            return;
        }
        draft.useFileMetadata = useFileMetadataRadio.isSelected();
        draft.holdPlayCount = holdPlayCountSwitch.isSelected();
        draft.writeMetadata = writeMetadataSwitch.isSelected();
    }

    public ReadOnlyBooleanProperty invalidProperty() {
        return invalid;
    }

    /**
     * Builds an {@link ItunesImportPolicy} from the current control state. The accepted
     * file types carry forward the previously hardcoded set ({@link AudioFileType} entries) —
     * file-format gating is not part of this wizard.
     */
    public ItunesImportPolicy getPolicy() {
        return new ItunesImportPolicy(
                useFileMetadataRadio.isSelected(),
                holdPlayCountSwitch.isSelected(),
                writeMetadataSwitch.isSelected(),
                Set.of(AudioFileType.values())
        );
    }
}
