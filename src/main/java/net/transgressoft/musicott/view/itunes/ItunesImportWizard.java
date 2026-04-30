package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesImportPolicy;
import net.transgressoft.musicott.service.MediaImportService;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Spring component that owns the four-step iTunes import wizard. Each invocation of
 * {@link #show(Window)} builds a fresh {@link ItunesImportDraft}, loads the four step
 * controllers via {@link FxWeaver}, wraps each in a per-step {@link WizardPane} that
 * mediates the controller's lifecycle hooks, runs the wizard modally, and — on FINISH —
 * dispatches {@link MediaImportService#importSelectedPlaylists} with the policy the
 * user built. CANCEL or close discards the draft and does not start an import.
 *
 * @author Octavio Calleya
 */
@Component
public class ItunesImportWizard {

    private static final Logger logger = LoggerFactory.getLogger(ItunesImportWizard.class);
    private static final String DIALOG_STYLE = "/css/dialog.css";

    private final FxWeaver fxWeaver;
    private final MediaImportService mediaImportService;

    @Autowired
    public ItunesImportWizard(FxWeaver fxWeaver, MediaImportService mediaImportService) {
        this.fxWeaver = fxWeaver;
        this.mediaImportService = mediaImportService;
    }

    /**
     * Runs the wizard modally, owned by {@code owner}. Blocks until the user clicks Import,
     * Cancel, or closes the dialog. On Import, dispatches the import via
     * {@link MediaImportService#importSelectedPlaylists}; otherwise the collected draft is
     * discarded.
     */
    public void show(Window owner) {
        ItunesImportDraft draft = new ItunesImportDraft();

        FxControllerAndView<ItunesWizardLibraryStepController, Parent> libraryPair =
                fxWeaver.load(ItunesWizardLibraryStepController.class);
        FxControllerAndView<ItunesWizardPlaylistsStepController, Parent> playlistsPair =
                fxWeaver.load(ItunesWizardPlaylistsStepController.class);
        FxControllerAndView<ItunesWizardPolicyStepController, Parent> policyPair =
                fxWeaver.load(ItunesWizardPolicyStepController.class);
        FxControllerAndView<ItunesWizardConfirmStepController, Parent> confirmPair =
                fxWeaver.load(ItunesWizardConfirmStepController.class);

        libraryPair.getController().bind(draft);
        playlistsPair.getController().bind(draft);
        policyPair.getController().bind(draft);
        confirmPair.getController().bind(draft);

        WizardPane libraryPane = new LibraryStepPane(
                libraryPair.getController(), libraryPair.getView().orElseThrow());
        WizardPane playlistsPane = new PlaylistsStepPane(
                playlistsPair.getController(), playlistsPair.getView().orElseThrow());
        WizardPane policyPane = new PolicyStepPane(
                policyPair.getController(), policyPair.getView().orElseThrow());
        WizardPane confirmPane = new ConfirmStepPane(
                confirmPair.getController(), confirmPair.getView().orElseThrow());

        Wizard wizard = new Wizard(owner, "Import from iTunes");
        wizard.setFlow(new Wizard.LinearFlow(libraryPane, playlistsPane, policyPane, confirmPane));

        Optional<ButtonType> result = wizard.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.FINISH) {
            ItunesImportPolicy policy = policyPair.getController().getPolicy();
            mediaImportService.importSelectedPlaylists(draft.selectedPlaylists, policy);
        }
    }

    /**
     * Renames the wizard's terminal button text from the controlsfx default "Finish" to
     * "Import". ControlsFX adds the FINISH button to every page's own button list during a
     * transition (see {@code Wizard.updatePage → addButtonIfMissing}); the same button list
     * is used as the dialog pane on each page, so the user-visible FINISH button is whichever
     * one belongs to the currently shown page. To guarantee the user sees "Import" regardless
     * of which page surfaces the button (controlsfx hides FINISH on non-terminal pages but
     * still pre-installs it), every pane runs this rename on first entry. The
     * {@code alreadyRenamed} flag prevents redundant work on Back-navigation re-entries.
     */
    private static void renameFinishButton(WizardPane pane, boolean alreadyRenamed) {
        if (alreadyRenamed) {
            return;
        }
        Button finishButton = (Button) pane.lookupButton(ButtonType.FINISH);
        if (finishButton != null) {
            finishButton.setText("Import");
        }
        else {
            logger.warn("FINISH button not found on wizard pane — leaving default label");
        }
    }

    /**
     * First step pane. Rebinds the wizard's per-page validity to this step's controller and
     * performs the one-shot rename of the wizard's terminal button text from "Finish" to
     * "Import" against this pane's freshly-installed FINISH button.
     */
    private static final class LibraryStepPane extends WizardPane {

        private final ItunesWizardLibraryStepController controller;
        private boolean finishRenamed = false;

        LibraryStepPane(ItunesWizardLibraryStepController controller, Parent content) {
            this.controller = controller;
            setHeaderText("Choose iTunes library");
            setContent(content);
            getStylesheets().add(DIALOG_STYLE);
        }

        @Override
        public void onEnteringPage(Wizard wizard) {
            super.onEnteringPage(wizard);
            wizard.invalidProperty().unbind();
            wizard.invalidProperty().bind(controller.invalidProperty());
            renameFinishButton(this, finishRenamed);
            finishRenamed = true;
        }
    }

    private static final class PlaylistsStepPane extends WizardPane {

        private final ItunesWizardPlaylistsStepController controller;
        private boolean finishRenamed = false;

        PlaylistsStepPane(ItunesWizardPlaylistsStepController controller, Parent content) {
            this.controller = controller;
            setHeaderText("Choose playlists to import");
            setContent(content);
            getStylesheets().add(DIALOG_STYLE);
        }

        @Override
        public void onEnteringPage(Wizard wizard) {
            super.onEnteringPage(wizard);
            wizard.invalidProperty().unbind();
            wizard.invalidProperty().bind(controller.invalidProperty());
            renameFinishButton(this, finishRenamed);
            finishRenamed = true;
            controller.onEnter();
        }

        @Override
        public void onExitingPage(Wizard wizard) {
            controller.onExit();
        }
    }

    private static final class PolicyStepPane extends WizardPane {

        private final ItunesWizardPolicyStepController controller;
        private boolean finishRenamed = false;

        PolicyStepPane(ItunesWizardPolicyStepController controller, Parent content) {
            this.controller = controller;
            setHeaderText("Choose how metadata is imported");
            setContent(content);
            getStylesheets().add(DIALOG_STYLE);
        }

        @Override
        public void onEnteringPage(Wizard wizard) {
            super.onEnteringPage(wizard);
            wizard.invalidProperty().unbind();
            wizard.invalidProperty().bind(controller.invalidProperty());
            renameFinishButton(this, finishRenamed);
            finishRenamed = true;
            controller.onEnter();
        }

        @Override
        public void onExitingPage(Wizard wizard) {
            controller.onExit();
        }
    }

    private static final class ConfirmStepPane extends WizardPane {

        private final ItunesWizardConfirmStepController controller;
        private boolean finishRenamed = false;

        ConfirmStepPane(ItunesWizardConfirmStepController controller, Parent content) {
            this.controller = controller;
            setHeaderText("Review and import");
            setContent(content);
            getStylesheets().add(DIALOG_STYLE);
        }

        @Override
        public void onEnteringPage(Wizard wizard) {
            super.onEnteringPage(wizard);
            wizard.invalidProperty().unbind();
            wizard.invalidProperty().bind(controller.invalidProperty());
            renameFinishButton(this, finishRenamed);
            finishRenamed = true;
            controller.onEnter();
        }
    }
}
