/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.view;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.guice.factories.*;
import javafx.beans.property.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.stage.FileChooser.*;
import javafx.stage.Stage;
import org.slf4j.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import static com.transgressoft.musicott.model.AlbumsLibrary.*;
import static com.transgressoft.musicott.model.CommonObject.*;
import static com.transgressoft.musicott.util.Utils.*;

/**
 * Controller class of the window that edits the information of tracks
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 */
@Singleton
public class EditController extends InjectableController<AnchorPane> {

    public static final Image DEFAULT_COVER_IMAGE = new Image(EditController.class.getResourceAsStream(DEFAULT_COVER.toString()));

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    @FXML
    private AnchorPane rootPane;
    @FXML
    private TextField nameTextField;
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

    private File newCoverImage;
    private Map<TrackField, TextInputControl> editableFieldsMap;
    private List<Track> trackSelection = Collections.emptyList();
    private Optional<byte[]> commonCover = Optional.empty();
    private Optional<String> newChangedAlbum = Optional.empty();
    private Set<String> changedAlbums = new HashSet<>();

    private UpdateMusicLibraryTaskFactory updateTaskFactory;

    @FXML
    public void initialize() {
        editableFieldsMap = new EnumMap<>(TrackField.class);
        editableFieldsMap.put(TrackField.NAME, nameTextField);
        editableFieldsMap.put(TrackField.ARTIST, artistTextField);
        editableFieldsMap.put(TrackField.ALBUM, albumTextField);
        editableFieldsMap.put(TrackField.GENRE, genreTextField);
        editableFieldsMap.put(TrackField.COMMENTS, commentsTextField);
        editableFieldsMap.put(TrackField.ALBUM_ARTIST, albumArtistTextField);
        editableFieldsMap.put(TrackField.LABEL, labelTextField);
        editableFieldsMap.put(TrackField.TRACK_NUMBER, trackNumTextField);
        editableFieldsMap.put(TrackField.DISC_NUMBER, discNumTextField);
        editableFieldsMap.put(TrackField.YEAR, yearTextField);
        editableFieldsMap.put(TrackField.BPM, bpmTextField);

        titleNameLabel.textProperty().bind(nameTextField.textProperty());
        titleArtistLabel.textProperty().bind(artistTextField.textProperty());
        titleAlbumLabel.textProperty().bind(albumTextField.textProperty());

        setNumericValidationFilters();

        coverImage.setImage(DEFAULT_COVER_IMAGE);
        coverImage.setCacheHint(CacheHint.QUALITY);
        coverImage.setOnDragOver(this::onDragOverCoverImage);
        coverImage.setOnDragExited(this::onDragExitedOutOfCoverImage);
        coverImage.setOnDragDropped(this::onDragDroppedOnCoverImage);
        coverImage.setOnMouseClicked(event -> {
            if (event.getClickCount() <= 2)
                changeCover();
        });

        okButton.setOnAction(event -> editAndClose());
        cancelButton.setOnAction(event -> close());
        LOG.debug("EditController initialized {}");
    }

    @Override
    public void setStage(Stage stage) {
        this.stage = stage;
        stage.setTitle("Edit");
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setOnCloseRequest(e -> close());
    }

    /**
     * Shows the edit window. If the number of track to edit is greater than 1,
     * an {@code Alert} is opened asking for a confirmation of the user.
     *
     * @param trackSelection The tracks to edit
     */
    public void editTracks(List<Track> trackSelection) {
        this.trackSelection = trackSelection;
        if (! trackSelection.isEmpty()) {
            if (trackSelection.size() > 1) {
                String alertHeader = "Are you sure you want to edit multiple files?";
                Alert alert = createAlert("", alertHeader, "", AlertType.CONFIRMATION, stage);
                Optional<ButtonType> result = alert.showAndWait();

                if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
                    setEditFieldsValues();
                    stage.showAndWait();
                }
                else
                    alert.close();
            }
            else {
                setEditFieldsValues();
                stage.showAndWait();
            }
        }
    }

    private void setNumericValidationFilters() {
        EventHandler<KeyEvent> nonNumericFilter = event -> {
            if (! event.getCharacter().matches("[0-9]"))
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

    private void onDragOverCoverImage(DragEvent event) {
        Dragboard dragboard = event.getDragboard();

        if (isFileDraggedAccepted(event)) {
            File imageFileDragged = dragboard.getFiles().get(0);
            Optional<Image> newImage = Utils.getImageFromFile(imageFileDragged);
            newImage.ifPresent(coverImage::setImage);
        }
        event.consume();
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
        boolean[] success = {false};

        if (isFileDraggedAccepted(event)) {
            newCoverImage = dragboard.getFiles().get(0);
            Optional<Image> newImage = Utils.getImageFromFile(newCoverImage);
            newImage.ifPresent(image -> {
                coverImage.setImage(image);
                success[0] = true;
            });
        }

        event.setDropCompleted(success[0]);
        event.consume();
    }

    private void onDragExitedOutOfCoverImage(DragEvent event) {
        if (newCoverImage == null) {
            if (commonCover.isPresent())
                coverImage.setImage(new Image(new ByteArrayInputStream(commonCover.get())));
            else
                coverImage.setImage(DEFAULT_COVER_IMAGE);
        }
        event.consume();
    }

    public ReadOnlyBooleanProperty showingProperty() {
        return stage.showingProperty();
    }

    /**
     * Fills the text inputs on the edit window for each {@link TrackField}.
     * If there is not a common value for the selected tracks for each {@code TrackField},
     * a dash ({@code -}) is placed in the {@link TextField}.
     */
    private void setEditFieldsValues() {
        editableFieldsMap.entrySet().forEach(this::setFieldValue);
        newCoverImage = null;
        newChangedAlbum = Optional.empty();
        changedAlbums.clear();
        coverImage.setImage(DEFAULT_COVER_IMAGE);
        commonCover = commonCover();
        commonCover.ifPresent(coverBytes -> coverImage.setImage(new Image(new ByteArrayInputStream(coverBytes))));

        if (! commonCompilation())
            isCompilationCheckBox.setIndeterminate(true);
        else
            isCompilationCheckBox.setSelected(trackSelection.get(0).isPartOfCompilation());
    }

    /**
     * Sets the value of the {@link TrackField} in the proper text
     * input control on the edit window for the given track selection.
     *
     * @param fieldEntry An {@link Entry} with the {@link TrackField} and the {@link TextInputControl} mapped to it
     */
    private void setFieldValue(Entry<TrackField, TextInputControl> fieldEntry) {
        TrackField trackField = fieldEntry.getKey();
        TextInputControl inputControl = fieldEntry.getValue();

        List<String> selectionValues = trackSelection.stream()
                                                     .map(trackToEdit -> getTrackPropertyValue(trackField, trackToEdit))
                                                     .collect(Collectors.toList());
        inputControl.textProperty().setValue(commonString(selectionValues));
    }

    /**
     * Returns a {@code String} of the track property value.
     *
     * @param trackField  The {@link TrackField}
     * @param trackToEdit The {@link Track}
     *
     * @return
     */
    private String getTrackPropertyValue(TrackField trackField, Track trackToEdit) {
        Map<TrackField, Property> propertyMap = trackToEdit.getPropertyMap();
        Property<?> trackProperty = propertyMap.get(trackField);

        String value;
        if (TrackField.isIntegerField(trackField)) {
            IntegerProperty ip = (IntegerProperty) trackProperty;
            if (ip.get() == 0)
                value = "";
            else
                value = String.valueOf(ip.get());
        }
        else
            value = String.valueOf(trackProperty.getValue());
        return value;
    }

    /**
     * Performs the editing of the track selection with the new values for their track fields.
     */
    private void editAndClose() {
        trackSelection.forEach(this::editTrack);
        UpdateMusicLibraryTask updateTask = updateTaskFactory.create(trackSelection, changedAlbums, newChangedAlbum);
        updateTask.setDaemon(true);
        updateTask.start();
        stage.close();
    }

    /**
     * Closes the edit window.
     */
    private void close() {
        LOG.info("Edit stage cancelled");
        newCoverImage = null;
        newChangedAlbum = Optional.empty();
        commonCover = Optional.empty();
        stage.close();
    }

    /**
     * Edits a {@link Track} with the values of the input controls of the
     * window for each {@link TrackField}.
     *
     * @param track The {@code Track} to edit
     */
    private void editTrack(Track track) {
        Map<TrackField, Property> trackPropertiesMap = track.getPropertyMap();
        final boolean[] changed = {false};

        editableFieldsMap.entrySet().forEach(entry -> {
            Property property = trackPropertiesMap.get(entry.getKey());
            changed[0] = editTrackTrackField(entry, property);
            if (changed[0] && entry.getKey().equals(TrackField.ALBUM))
                newChangedAlbum = Optional.of(track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum());
        });

        if (! isCompilationCheckBox.isIndeterminate()) {
            track.setIsPartOfCompilation(isCompilationCheckBox.isSelected());
            changed[0] = true;
        }

        track.setCoverImage(newCoverImage);

        if (changed[0]) {
            track.setLastDateModified(LocalDateTime.now());
            LOG.info("Track {} edited to {}", track.getTrackId(), track);
        }
    }

    /**
     * Updates the new value of a {@link Track} property with the one entered
     * by the user on the {@link TextInputControl} of the window.
     *
     * @param entry         The {@link Entry} with the {@link TrackField} and its related text input
     * @param trackProperty The {@link Property} of the track to edit
     *
     * @return {@code true} if the property was changed, {@code false} otherwise
     */
    private boolean editTrackTrackField(Entry<TrackField, TextInputControl> entry, Property<?> trackProperty) {
        boolean changed = false;
        TrackField field = entry.getKey();
        String newValue = entry.getValue().textProperty().getValue();

        if (TrackField.isIntegerField(field)) {
            IntegerProperty ip = (IntegerProperty) trackProperty;
            try {
                int newNumericValue = Integer.parseInt(newValue);
                if (! "-".equals(newValue) || (! newValue.isEmpty() && ip.get() != newNumericValue)) {
                    ip.setValue(newNumericValue);
                    changed = true;
                }
            }
            catch (NumberFormatException e) {}
        }
        else {
            StringProperty sp = (StringProperty) trackProperty;
            if (! "-".equals(newValue) && ! sp.get().equals(newValue)) {
                sp.setValue(newValue);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Changes the showing image of the {@link ImageView} placed on the top
     * of the edit window that will be saved for all the track selection.
     */
    private void changeCover() {
        LOG.debug("Choosing cover image");
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open file(s)...");
        ExtensionFilter filter = new ExtensionFilter("Image files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg");
        chooser.getExtensionFilters().addAll(filter);
        newCoverImage = chooser.showOpenDialog(stage);
        if (newCoverImage != null) {
            Optional<Image> newImage = Utils.getImageFromFile(newCoverImage);
            newImage.ifPresent(coverImage::setImage);
        }
    }

    /**
     * Returns the common cover image of the track selection, or an empty {@link Optional} otherwise
     *
     * @return The {@code byte}s of the common cover image, or an empty {@code Optional}
     */
    private Optional<byte[]> commonCover() {
        Optional<byte[]>[] optionalCoverBytes = new Optional[]{Optional.empty()};
        Optional<String> sameAlbum = commonAlbum();
        sameAlbum.ifPresent(
                trackAlbum -> trackSelection.stream().filter(track -> track.getCoverImage().isPresent()).findFirst()
                                            .ifPresent(track -> optionalCoverBytes[0] = Optional
                                                    .of(track.getCoverImage().get())));
        return optionalCoverBytes[0];
    }

    /**
     * Returns the common album of the track selection, or an empty
     * {@link Optional} otherwise.
     *
     * @return
     */
    private Optional<String> commonAlbum() {
        changedAlbums = trackSelection.stream().map(track ->
                                track.getAlbum().isEmpty() ? UNK_ALBUM : track.getAlbum())
                                      .collect(Collectors.toSet());
        String firstAlbum = trackSelection.get(0).getAlbum();
        return changedAlbums.size() <= 1 ? Optional.of(firstAlbum) : Optional.empty();
    }

    /**
     * @return {@code true} if al the tracks have the same compilation value, {@code false} otherwise.
     */
    private boolean commonCompilation() {
        Boolean isCommon = trackSelection.get(0).isPartOfCompilation();
        return trackSelection.stream().allMatch(t -> isCommon.equals(t.isPartOfCompilation()));
    }

    /**
     * Returns the common {@code String} of a list, if any, or a dash (-) otherwise.
     *
     * @param list The {@link List} with the {@code String}s
     *
     * @return
     */
    private String commonString(List<String> list) {
        String commonString;
        if (list.stream().allMatch(st -> st.equalsIgnoreCase(list.get(0))))
            commonString = list.get(0);
        else
            commonString = "-";
        return commonString;
    }

    @Inject (optional = true)
    public void setUpdateTaskFactory(UpdateMusicLibraryTaskFactory factory) {
        updateTaskFactory = factory;
    }
}
