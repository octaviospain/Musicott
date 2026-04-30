package net.transgressoft.musicott.view.itunes;

import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.musicott.service.MediaImportService;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.nio.file.Path;

/**
 * Step controller for picking the iTunes library XML file. Renders a button that opens a
 * {@link FileChooser} and a label showing the currently selected path; on selection,
 * parses the chosen file via {@link MediaImportService#parseLibrary(Path)} and stores
 * the parsed library on the supplied {@link ItunesImportDraft}. Exposes a
 * {@link ReadOnlyBooleanProperty} that the wizard binds to its per-page validity gate —
 * the property is {@code true} until a file has been successfully parsed.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/ItunesWizardLibraryStep.fxml")
@Controller
public class ItunesWizardLibraryStepController {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final MediaImportService mediaImportService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BooleanProperty invalid = new SimpleBooleanProperty(this, "invalid", true);

    @FXML
    Button chooseFileButton;
    @FXML
    Label selectedFileLabel;

    private ItunesImportDraft draft;

    @Autowired
    public ItunesWizardLibraryStepController(MediaImportService mediaImportService,
                                             ApplicationEventPublisher applicationEventPublisher) {
        this.mediaImportService = mediaImportService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @FXML
    public void initialize() {
        chooseFileButton.setOnAction(e -> openFileChooser());
    }

    /**
     * Binds this step controller to the supplied wizard draft. Called by the wizard owner
     * once per wizard session, before the first page is rendered. Restores the selected-path
     * label and validity state if the draft already carries a parsed library (e.g., when
     * the user navigates back to step 1 after having advanced).
     */
    void bind(ItunesImportDraft draft) {
        this.draft = draft;
        if (draft.libraryPath != null) {
            selectedFileLabel.setText(draft.libraryPath.toString());
            invalid.set(draft.parsedLibrary == null);
        }
    }

    public ReadOnlyBooleanProperty invalidProperty() {
        return invalid;
    }

    /**
     * Direct entry point for tests and integration code that bypasses the modal
     * {@link FileChooser}. Parses the supplied path through {@link MediaImportService} and
     * updates the draft on success, or publishes an {@link ExceptionEvent} on failure
     * while leaving the draft untouched and the step invalid.
     */
    public void acceptLibraryFile(Path xmlPath) {
        try {
            var library = mediaImportService.parseLibrary(xmlPath);
            draft.libraryPath = xmlPath;
            draft.parsedLibrary = library;
            selectedFileLabel.setText(xmlPath.toString());
            invalid.set(false);
        }
        catch (Throwable t) {
            logger.error("Failed to parse iTunes library at {}", xmlPath, t);
            applicationEventPublisher.publishEvent(new ExceptionEvent(t, this));
            invalid.set(true);
        }
    }

    private void openFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose iTunes library XML");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("iTunes library", "*.xml"));
        var window = chooseFileButton.getScene() != null ? chooseFileButton.getScene().getWindow() : null;
        File file = chooser.showOpenDialog(window);
        if (file != null) {
            acceptLibraryFile(file.toPath());
        }
    }
}
