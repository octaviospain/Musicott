package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.Genre;
import net.transgressoft.commons.music.audio.ImmutableArtist;
import net.transgressoft.commons.music.audio.ImmutableLabel;
import net.transgressoft.musicott.events.EditAudioItemsMetadataEvent;
import net.transgressoft.musicott.events.ExceptionEvent;
import net.transgressoft.musicott.events.InvalidAudioItemsForEditionEvent;
import net.transgressoft.musicott.events.OpenAudioItemEditorView;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.alerts.MultipleEditionConfirmationAlert;

import com.neovisionaries.i18n.CountryCode;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/EditController.fxml")
@Controller
public class EditController {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final Image defaultCoverImage = ApplicationImage.DEFAULT_COVER.get();

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @FXML
    private AnchorPane rootPane;
    @FXML
    private TextField titleTextField;
    @FXML
    private TextField artistTextField;
    @FXML
    private TextField albumTextField;
    @FXML
    private TextField albumArtistTextField;
    @FXML
    private TextField genreTextField;
    @FXML
    private TextField labelTextField;
    @FXML
    private TextField yearTextField;
    @FXML
    private TextField bpmTextField;
    @FXML
    private TextField trackNumTextField;
    @FXML
    private TextField discNumTextField;
    @FXML
    private TextArea commentsTextField;
    @FXML
    private Label titleNameLabel;
    @FXML
    private Label titleArtistLabel;
    @FXML
    private Label titleAlbumLabel;
    @FXML
    private ImageView coverImage;
    @FXML
    private CheckBox isCompilationCheckBox;
    @FXML
    private Button cancelButton;

    @FXML
    private Button okButton;

    private Stage stage;
    private Set<ObservableAudioItem> audioItemSelection;
    private byte[] newCoverImageBytes;
    private Optional<byte[]> commonCover = Optional.empty();
    private Alert multipleEditionConfirmationAlert;

    @FXML
    public void initialize() {
        stage = new Stage();
        stage.setTitle("Edit");
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(e -> close());

        multipleEditionConfirmationAlert = new MultipleEditionConfirmationAlert(stage);

        titleNameLabel.textProperty().bind(titleTextField.textProperty());
        titleArtistLabel.textProperty().bind(artistTextField.textProperty());
        titleAlbumLabel.textProperty().bind(albumTextField.textProperty());

        setNumericValidationFilters();

        coverImage.setCacheHint(CacheHint.QUALITY);
        coverImage.setOnDragOver(this::onDragOverCoverImage);
        coverImage.setOnDragExited(this::onDragExitedOutOfCoverImage);
        coverImage.setOnDragDropped(this::onDragDroppedOnCoverImage);
        coverImage.setOnMouseClicked(event -> {
            if (event.getClickCount() <= 2) {
                changeCoverImage();
            }
        });

        okButton.setOnAction(event -> {
            applicationEventPublisher.publishEvent(new EditAudioItemsMetadataEvent(audioItemSelection, getEditionResult(), this));
            close();
        });
        cancelButton.setOnAction(event -> close());
    }

    private void setNumericValidationFilters() {
        EventHandler<KeyEvent> nonNumericFilter = event -> {
            if (! event.getCharacter().matches("\\d"))
                event.consume();
        };
        trackNumTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
        trackNumTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (trackNumTextField.getText().length() == 3)
                event.consume();
        });
        discNumTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
        discNumTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (discNumTextField.getText().length() == 3)
                event.consume();
        });
        bpmTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
        bpmTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (bpmTextField.getText().length() == 3)
                event.consume();
        });
        yearTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
        yearTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (yearTextField.getText().length() == 4)
                event.consume();
        });
    }

    private void changeCoverImage() {
        logger.debug("Choosing cover image");
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open file(s)...");
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Image files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg");
        chooser.getExtensionFilters().addAll(filter);
        File newCoverImageFile = chooser.showOpenDialog(stage);
        if (newCoverImageFile != null) {
            try {
                newCoverImageBytes = Files.readAllBytes(Paths.get(newCoverImageFile.getPath()));
                Optional<Image> optionalImage = Optional.of(new Image(new ByteArrayInputStream(newCoverImageBytes)));
                optionalImage.ifPresent(coverImage::setImage);
            }
            catch (IOException exception) {
                applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
                logger.error("Error changing cover image", exception);
            }
        }
    }

    private void onDragOverCoverImage(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (isFileDraggedAccepted(event)) {
            File imageFileDragged = dragboard.getFiles().get(0);
            imageFromDraggedFile(imageFileDragged).ifPresentOrElse(coverImage::setImage, () -> coverImage.setImage(defaultCoverImage));
        }
        event.consume();
    }

    private Optional<Image> imageFromDraggedFile(File draggedImage) {
        Optional<Image> image;
        try {
            newCoverImageBytes = Files.readAllBytes(Paths.get(draggedImage.getPath()));
            image = Optional.of(new Image(new ByteArrayInputStream(newCoverImageBytes)));
        }
        catch (IOException exception) {
            image = Optional.empty();
            newCoverImageBytes = null;
            applicationEventPublisher.publishEvent(new ExceptionEvent(exception, this));
            logger.error("Error trying to set image", exception);
        }
        return image;
    }

    private boolean isFileDraggedAccepted(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean isAccepted = false;
        if (dragboard.hasFiles() && dragboard.getFiles().size() == 1) {
            event.acceptTransferModes(TransferMode.MOVE);
            File imageFileDragged = dragboard.getFiles().get(0);
            String fileName = imageFileDragged.getName().toLowerCase();
            isAccepted = fileName.endsWith(".png") || fileName.endsWith(".jpeg") || fileName.endsWith(".jpg");
        }
        return isAccepted;
    }

    private void onDragDroppedOnCoverImage(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        if (isFileDraggedAccepted(event)) {
            File newCoverImageFile = dragboard.getFiles().get(0);
            imageFromDraggedFile(newCoverImageFile).ifPresentOrElse(image -> {
                coverImage.setImage(image);
                event.setDropCompleted(true);
            }, () -> event.setDropCompleted(false));
        }

        event.consume();
    }

    private void onDragExitedOutOfCoverImage(DragEvent event) {
        if (newCoverImageBytes == null) {
            if (commonCover.isPresent())
                coverImage.setImage(new Image(new ByteArrayInputStream(commonCover.get())));
            else
                coverImage.setImage(defaultCoverImage);
        }
        event.consume();
    }

    @EventListener
    public void editAudioItemsEventListener(OpenAudioItemEditorView event) {
        List<ObservableAudioItem> invalidAudioItems = event.audioItems.stream()
            .filter(track -> ! track.getPath().toFile().exists())
            .collect(Collectors.toList());

        if (! invalidAudioItems.isEmpty())
            applicationEventPublisher.publishEvent(new InvalidAudioItemsForEditionEvent(invalidAudioItems, this));
        else
            editAudioItems(event.audioItems);
    }

    /**
     * Shows the edit window. If the number of {@link ObservableAudioItem}s to edit is greater than 1,
     * an {@code Alert} is opened asking for a confirmation of the user.
     *
     * @param audioItemSelection The audio items to edit
     */
    public void editAudioItems(Set<ObservableAudioItem> audioItemSelection) {
        if (! audioItemSelection.isEmpty()) {
            this.audioItemSelection = audioItemSelection;
            if (audioItemSelection.size() > 1) {
                multipleEditionConfirmationAlert.showAndWait().ifPresent(result -> {
                    if (result.getButtonData().isDefaultButton()) {
                        updateFields();
                        stage.showAndWait();
                    } else {
                        close();
                    }
                });
            } else {
                updateFields();
                stage.showAndWait();
            }
        }
    }

    /**
     * Fills the text inputs on the edit window for each editable field.
     * If there is not a common value for the selected tracks for each field,
     * a dash ({@code -}) is placed in the {@link TextField}.
     */
    private void updateFields() {
        newCoverImageBytes = null;

        titleTextField.textProperty().set(commonTitle());
        artistTextField.textProperty().set(commonArtist());
        albumTextField.textProperty().set(commonAlbum());
        genreTextField.textProperty().set(commonGenre());
        commentsTextField.textProperty().set(commonComments());
        albumArtistTextField.textProperty().set(commonAlbumArtist());
        labelTextField.textProperty().set(commonLabel());
        trackNumTextField.textProperty().set(commonTrackNumber());
        discNumTextField.textProperty().set(commonDiscNumber());
        yearTextField.textProperty().set(commonYear());
        bpmTextField.textProperty().set(commonBpm());

        commonCover = commonCoverImage();
        commonCover.ifPresentOrElse(
                coverBytes -> coverImage.setImage(new Image(new ByteArrayInputStream(coverBytes))),
                () -> coverImage.setImage(defaultCoverImage)
        );

        if (! commonCompilation())
            isCompilationCheckBox.setIndeterminate(true);
        else
            isCompilationCheckBox.setSelected(audioItemSelection.stream().findFirst().get().getAlbum().isCompilation());
    }

    private String commonString(List<String> list) {
        String commonString;
        if (list.stream().allMatch(st -> st.equalsIgnoreCase(list.get(0))))
            commonString = list.get(0);
        else
            commonString = "-";
        return commonString;
    }

    private String commonTitle() {
        return commonString(audioItemSelection.stream().map(ObservableAudioItem::getTitle).collect(toList()));
    }

    private String commonArtist() {
        return commonString(audioItemSelection.stream().map(t -> t.getArtist().getName()).collect(toList()));
    }

    private String commonAlbum() {
        Set<String> changedAlbums = audioItemSelection.stream()
                .filter(audioItem -> ! audioItem.getAlbum().getName().isEmpty())
                .map(audioItem -> audioItem.getAlbum().getName())
                .collect(Collectors.toSet());
        String firstAlbum = audioItemSelection.stream().findFirst().get().getAlbum().getName();
        return changedAlbums.size() <= 1 ? firstAlbum : "-";
    }

    private String commonGenre() {
        return commonString(audioItemSelection.stream().map(t -> t.getGenre().name()).collect(toList()));
    }

    private String commonComments() {
        return commonString(audioItemSelection.stream().map(ObservableAudioItem::getComments).collect(toList()));
    }

    private String commonAlbumArtist() {
        return commonString(audioItemSelection.stream().map(t -> t.getAlbum().getAlbumArtist().getName()).collect(toList()));
    }

    private String commonLabel() {
        return commonString(audioItemSelection.stream().map(t -> t.getAlbum().getLabel().getName()).collect(toList()));
    }

    private String commonTrackNumber() {
        return commonString(audioItemSelection.stream().map(t -> String.valueOf(t.getTrackNumber())).collect(toList()));
    }

    private String commonDiscNumber() {
        return commonString(audioItemSelection.stream().map(t -> String.valueOf(t.getDiscNumber())).collect(toList()));
    }

    private String commonYear() {
        return commonString(audioItemSelection.stream().map(t -> String.valueOf(t.getAlbum().getYear())).collect(toList()));
    }

    private String commonBpm() {
        return commonString(audioItemSelection.stream().map(t -> String.valueOf(t.getBpm())).collect(toList()));
    }

    private Optional<byte[]> commonCoverImage() {
        Optional<byte[]> optionalCoverBytes = Optional.empty();
        if (! commonAlbum().isEmpty()) {
            Optional<ObservableAudioItem> audioItem = audioItemSelection.stream()
                    .filter(t -> t.getCoverImageProperty().get().isPresent())
                    .findFirst();
            if (audioItem.isPresent())
                optionalCoverBytes = Optional.ofNullable(audioItem.get().getCoverImageBytes());
        }
        return optionalCoverBytes;
    }

    private boolean commonCompilation() {
        Boolean isCommon = audioItemSelection.stream().findFirst().get().getAlbum().isCompilation();
        return audioItemSelection.stream().allMatch(t -> isCommon.equals(t.getAlbum().isCompilation()));
    }

    private AudioItemMetadataChange getEditionResult() {
       String title = getEditionFieldResult(titleTextField);
       Artist artist = getEditionFieldResult(artistTextField) != null ? ImmutableArtist.of(getEditionFieldResult(artistTextField)) : null;
       String albumName = getEditionFieldResult(albumTextField);
       Artist albumArtist = getEditionFieldResult(albumArtistTextField) != null ? ImmutableArtist.of(getEditionFieldResult(albumArtistTextField)) : null;
       boolean isCompilation = isCompilationCheckBox.isIndeterminate() ? null : isCompilationCheckBox.isSelected();
       Short year = getEditionFieldResult(yearTextField) != null ? Short.valueOf(getEditionFieldResult(yearTextField)) : null;
       net.transgressoft.commons.music.audio.Label label = getEditionFieldResult(labelTextField) != null ? ImmutableLabel.of(getEditionFieldResult(labelTextField)) : null;
       Genre genre = getEditionFieldResult(genreTextField) != null ? Genre.parseGenre(getEditionFieldResult(genreTextField)) : null;
       String comments = getEditionFieldResult(commentsTextField);
       Short trackNum = getEditionFieldResult(trackNumTextField) != null ? Short.valueOf(getEditionFieldResult(trackNumTextField)) : null;
       Short discNum = getEditionFieldResult(discNumTextField) != null ? Short.valueOf(getEditionFieldResult(discNumTextField)) : null;
       Float bpm = getEditionFieldResult(bpmTextField) != null ? Float.valueOf(getEditionFieldResult(bpmTextField)) : null;

       return new AudioItemMetadataChange(title, artist, albumName, albumArtist, isCompilation, year, label, newCoverImageBytes, genre, comments, trackNum, discNum, bpm);
    }

    private String getEditionFieldResult(TextInputControl textField) {
        return textField.getText().equals("-") ? null : textField.getText();
    }

    private void close() {
        audioItemSelection = null;
        newCoverImageBytes = null;
        commonCover = Optional.empty();
        stage.close();
    }

    public record AudioItemMetadataChange(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            boolean isCompilation,
            Short year,
            net.transgressoft.commons.music.audio.Label label,
            byte[] coverImageBytes,
            Genre genre,
            String comments,
            Short trakNum,
            Short discNum,
            Float bpm) {
    }
}
