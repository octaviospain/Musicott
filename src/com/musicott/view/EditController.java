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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

package com.musicott.view;

import com.musicott.model.*;
import com.musicott.tasks.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.stage.*;
import javafx.stage.FileChooser.*;
import org.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

/**
 * Controller class for the window that edit the information of tracks
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class EditController implements MusicottController {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private final Image COVER_IMAGE = new Image(getClass().getResourceAsStream(DEFAULT_COVER_IMAGE));

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
	private Stage editStage;
	private Map<TrackField, TextInputControl> editFieldsMap;
	private List<Track> trackSelection;

	@FXML
	private void initialize() {
		editFieldsMap = new EnumMap<>(TrackField.class);
		editFieldsMap.put(TrackField.NAME, nameTextField);
		editFieldsMap.put(TrackField.ARTIST, artistTextField);
		editFieldsMap.put(TrackField.ALBUM, albumTextField);
		editFieldsMap.put(TrackField.GENRE, genreTextField);
		editFieldsMap.put(TrackField.COMMENTS, commentsTextField);
		editFieldsMap.put(TrackField.ALBUM_ARTIST, albumArtistTextField);
		editFieldsMap.put(TrackField.LABEL, labelTextField);
		editFieldsMap.put(TrackField.TRACK_NUMBER, trackNumTextField);
		editFieldsMap.put(TrackField.DISC_NUMBER, discNumTextField);
		editFieldsMap.put(TrackField.YEAR, yearTextField);
		editFieldsMap.put(TrackField.BPM, bpmTextField);
		
		titleNameLabel.textProperty().bind(nameTextField.textProperty());
		titleArtistLabel.textProperty().bind(artistTextField.textProperty());
		titleAlbumLabel.textProperty().bind(albumTextField.textProperty());

		setNumericValidationFilters();
		
		coverImage.setImage(COVER_IMAGE);
		coverImage.setCacheHint(CacheHint.QUALITY);
		coverImage.setOnMouseClicked(event -> {
			if(event.getClickCount() <= 2)
				changeCover();
		});

		okButton.setOnAction(event -> editAndClose());
		cancelButton.setOnAction(event -> close());
	}

	private void setNumericValidationFilters() {
		EventHandler<KeyEvent> nonNumericFilter = event -> {
			if(!event.getCharacter().matches("[0-9]"))
				event.consume();
		};
		trackNumTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
		trackNumTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
			if(trackNumTextField.getText().length() == 3)
				event.consume();
		});
		discNumTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
		discNumTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
			if(discNumTextField.getText().length() == 3)
				event.consume();
		});
		bpmTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
		bpmTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
			if(bpmTextField.getText().length() == 3)
				event.consume();
		});
		yearTextField.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
		yearTextField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
			if(yearTextField.getText().length() == 4)
				event.consume();
		});
	}

	/**
	 * Closes the edit window.
	 */
	private void close() {
		LOG.info("Edit stage cancelled");
		newCoverImage = null;
		editStage.close();
	}

	public void setStage(Stage stage) {
		editStage = stage;
		editStage.setOnCloseRequest(e -> close());
		editStage.setOnShowing(e -> setEditFieldsValues());
	}

	/**
	 * Fills the text inputs on the edit window for each {@link TrackField}.
	 * If there is not a common value for the selected tracks for each <tt>TrackField</tt>,
	 * a dash (<tt>-</tt>) is placed in the {@link TextField}.
	 */
	private void setEditFieldsValues() {
		ObservableList<Entry<Integer, Track>> selectionEntries = stageDemon.getRootController().getSelectedItems();
		trackSelection = selectionEntries.stream().map(Map.Entry::getValue).collect(Collectors.toList());

		editFieldsMap.entrySet().forEach(this::setFieldValue);

		coverImage.setImage(COVER_IMAGE);
		Optional<byte[]> commonCover = commonCover();
		commonCover.ifPresent(coverBytes -> coverImage.setImage(new Image(new ByteArrayInputStream(coverBytes))));

		if(!commonCompilation())
			isCompilationCheckBox.setIndeterminate(true);
		else
			isCompilationCheckBox.setSelected(trackSelection.get(0).getIsCompilation());
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
	 * Returns a <tt>String</tt> of the track property value.
	 *
	 * @param trackField The {@link TrackField}
	 * @param trackToEdit The {@link Track}
	 * @return
	 */
	private String getTrackPropertyValue(TrackField trackField, Track trackToEdit) {
		Map<TrackField, Property<?>> propertyMap = trackToEdit.getPropertiesMap();
		Property<?> trackProperty = propertyMap.get(trackField);
		
		String value;
		if(TrackField.isIntegerField(trackField)) {
			IntegerProperty ip = (IntegerProperty) trackProperty;
			if(ip.get() == 0)
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
		UpdateMetadataTask updateTask = new UpdateMetadataTask(trackSelection, newCoverImage);
		updateTask.setDaemon(true);
		updateTask.start();
		newCoverImage = null;
		editStage.close();
	}

	/**
	 * Edits a {@link Track} with the values of the input controls of the
	 * window for each {@link TrackField}.
	 *
	 * @param track The <tt>Track</tt> to edit
	 */
	private void editTrack(Track track) {
		Map<TrackField, Property<?>> trackPropertiesMap = track.getPropertiesMap();
		final boolean[] changed = {false};

		editFieldsMap.entrySet().forEach(entry -> {
			Property<?> property = trackPropertiesMap.get(entry.getKey());
			changed[0] = editTrackTrackField(entry, property);
		});

		if(!isCompilationCheckBox.isIndeterminate()) {
			track.setCompilation(isCompilationCheckBox.isSelected());
			changed[0] = true;
		}
		if(changed[0]) {
			track.setDateModified(LocalDateTime.now());
			LOG.info("Track {} edited to {}", track.getTrackID(), track);
		}
	}

	/**
	 * Updates the new value of a {@link Track} property with the one entered
	 * by the user on the {@link TextInputControl} of the window.
	 *
	 * @param entry The {@link Entry} with the {@link TrackField} and its related text input
	 * @param trackProperty The {@link Property} of the track to edit
	 * @return <tt>true</tt> if the property was changed, <tt>false</tt> otherwise
	 */
	private boolean editTrackTrackField(Entry<TrackField, TextInputControl> entry, Property<?> trackProperty) {
		boolean changed = false;
		TrackField field = entry.getKey();
		String newValue = entry.getValue().textProperty().getValue();

		if(TrackField.isIntegerField(field)) {
			IntegerProperty ip = (IntegerProperty) trackProperty;
			try {
				int newNumericValue = Integer.parseInt(newValue);
				if(!"-".equals(newValue) || (!newValue.isEmpty() && ip.get() != newNumericValue)) {
					ip.setValue(newNumericValue);
					changed = true;
				}
			} catch (NumberFormatException e) {

			}
		}
		else {
			StringProperty sp = (StringProperty) trackProperty;
			if(!"-".equals(newValue) && !sp.get().equals(newValue)) {
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
		ExtensionFilter filter = new ExtensionFilter ("Image files (*.png, *.jpg, *.jpeg)","*.png", "*.jpg", "*.jpeg");
		chooser.getExtensionFilters().addAll(filter);
		newCoverImage = chooser.showOpenDialog(editStage);
		byte[] newCoverBytes;
		if(newCoverImage != null) {
			try {
				newCoverBytes = Files.readAllBytes(Paths.get(newCoverImage.getPath()));
				coverImage.setImage(new Image(new ByteArrayInputStream(newCoverBytes)));
			} catch (IOException exception) {
				LOG.error("Error setting image", exception);
				errorDemon.showErrorDialog("Error setting image", "", exception, editStage.getScene());
			}
		}
	}

	/**
	 * Returns the common cover image of the track selection, or an empty {@link Optional} otherwise
	 *
	 * @return The <tt>byte</tt>s of the common cover image, or an empty <tt>Optional</tt>
	 */
	private Optional<byte[]> commonCover() {
		Optional<byte[]>[] coverBytes = new Optional[]{Optional.empty()};
		Optional<String> sameAlbum = commonAlbum();
		sameAlbum.ifPresent(trackAlbum ->
			trackSelection.stream().filter(Track::hasCover)
					.findFirst()
					.ifPresent(track -> coverBytes[0] = Optional.of(track.getCoverBytes()))
		);
		return coverBytes[0];
	}

	/**
	 * Returns the common album of the track selection, or an empty
	 * {@link Optional} otherwise.
	 *
	 * @return
	 */
	private Optional<String> commonAlbum() {
		String firstAlbum = trackSelection.get(0).getAlbum();
		boolean areCommon = trackSelection.stream().allMatch(track -> track.getAlbum().equals(firstAlbum));
		return areCommon ? Optional.of(firstAlbum) : Optional.empty();
	}

	/**
	 * @return <tt>true</tt> if al the tracks have the same compilation value, <tt>false</tt> otherwise.
	 */
	private boolean commonCompilation() {
		Boolean isCommon = trackSelection.get(0).getIsCompilation();
		return trackSelection.stream().allMatch(t -> isCommon.equals(t.getIsCompilation()));
	}

	/**
	 * Returns the common <tt>String</tt> of a list, if any, or a dash (-) otherwise.
	 *
	 * @param list The {@link List} with the <tt>String</tt>s
	 * @return
	 */
	private String commonString(List<String> list) {
		String commonString;
		if(list.stream().allMatch(st -> st.equalsIgnoreCase(list.get(0))))
			commonString = list.get(0);
		else
			commonString = "-";
		return commonString;
	}
}
