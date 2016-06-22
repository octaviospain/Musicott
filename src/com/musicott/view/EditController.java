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
 * Copyright (C) 2005, 2006 Octavio Calleya
 */

package com.musicott.view;

import com.musicott.*;
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
 * @author Octavio Calleya
 *
 */
public class EditController {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	@FXML
	private TextField name, artist, album, albumArtist, genre, label, year, bpm, trackNum, discNum;
	@FXML
	private TextArea comments;
	@FXML
	private Label titleName, titleArtist, titleAlbum;
	@FXML
	private ImageView coverImage;
	@FXML
	private CheckBox isCompilationCheckBox;
	@FXML
	private Button cancelEditButton, okEditButton;
	
	private Map<TrackField,TextInputControl> editFieldsMap;
	private File newCoverImage;
	private Stage editStage;
	private List<Track> trackSelection;
	private Image defaultImage = StageDemon.getInstance().getDefaultCoverImage();

	public EditController() {}
	
	@FXML
	private void initialize() {
		editFieldsMap = new HashMap<>();
		editFieldsMap.put(TrackField.NAME, name);
		editFieldsMap.put(TrackField.ARTIST, artist);
		editFieldsMap.put(TrackField.ALBUM, album);
		editFieldsMap.put(TrackField.GENRE, genre);
		editFieldsMap.put(TrackField.COMMENTS, comments);
		editFieldsMap.put(TrackField.ALBUM_ARTIST, albumArtist);
		editFieldsMap.put(TrackField.LABEL, label);
		editFieldsMap.put(TrackField.TRACK_NUMBER, trackNum);
		editFieldsMap.put(TrackField.DISC_NUMBER, discNum);
		editFieldsMap.put(TrackField.YEAR, year);
		editFieldsMap.put(TrackField.BPM, bpm);
		
		titleName.textProperty().bind(name.textProperty());
		titleArtist.textProperty().bind(artist.textProperty());
		titleAlbum.textProperty().bind(album.textProperty());
		
		// Validation of the numeric fields
		EventHandler<? super KeyEvent> nonNumericFilter = event -> {if(!event.getCharacter().matches("[0-9]")) event.consume();};
		trackNum.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter); 
		trackNum.addEventFilter(KeyEvent.KEY_TYPED, event -> {if(trackNum.getText().length() == 3) event.consume();});
		discNum.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
		discNum.addEventFilter(KeyEvent.KEY_TYPED, event -> {if(discNum.getText().length() == 3) event.consume();});
		bpm.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
		bpm.addEventFilter(KeyEvent.KEY_TYPED, event -> {if(bpm.getText().length() == 3) event.consume();});
		year.addEventFilter(KeyEvent.KEY_TYPED, nonNumericFilter);
		year.addEventFilter(KeyEvent.KEY_TYPED, event -> {if(year.getText().length() == 4) event.consume();});
		
		coverImage.setImage(defaultImage);
		coverImage.setCacheHint(CacheHint.QUALITY);
		coverImage.setOnMouseClicked(event -> {
			if(event.getClickCount() <= 2) {
				LOG.debug("Choosing cover image");
				FileChooser chooser = new FileChooser();
				chooser.setTitle("Open file(s)...");
				chooser.getExtensionFilters().addAll(new ExtensionFilter ("Image files (*.png, *.jpg, *.jpeg)","*.png", "*.jpg", "*.jpeg"));
				newCoverImage = chooser.showOpenDialog(editStage);
				byte[] newCoverBytes;
				if(newCoverImage != null) {
					try {
						newCoverBytes = Files.readAllBytes(Paths.get(newCoverImage.getPath()));
						coverImage.setImage(new Image(new ByteArrayInputStream(newCoverBytes)));
					} catch (IOException e) {
						LOG.error("Error setting image", e);
						ErrorDemon.getInstance().showErrorDialog("Error setting image", null, e, editStage.getScene());
					}
				}
			}
		});
	}
	
	public void setStage(Stage stage) {
		editStage = stage;
		editStage.setOnCloseRequest(e -> doCancel());
		editStage.setOnShowing(e -> setFields());
	}

	@FXML
	private void doOK() {
		boolean changed;
		String newValue;
		for(int i=0; i<trackSelection.size() ;i++) {
			Track track = trackSelection.get(i);
			Map<TrackField, Property<?>> trackPropertiesMap = track.getPropertiesMap();
			changed = false;
			
			for(TrackField field: editFieldsMap.keySet()) {
				newValue = editFieldsMap.get(field).textProperty().getValue();
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					IntegerProperty ip = (IntegerProperty) trackPropertiesMap.get(field);
					try {
						if(!newValue.equals("-") || (!newValue.equals("") && ip.get() != Integer.parseInt(newValue))) {
							ip.setValue(Integer.parseInt(newValue));
							changed = true;
						}
					} catch (NumberFormatException e) {}
				}
				else {
					StringProperty sp = (StringProperty) trackPropertiesMap.get(field);
					if(!newValue.equals("-") && !sp.get().equals(newValue)) {
						sp.setValue(newValue);
						changed = true;
					}
				}
			}
			if(!isCompilationCheckBox.isIndeterminate()) {
				track.setCompilation(isCompilationCheckBox.isSelected());
				changed = true;
			}
			if(changed) {
				track.setDateModified(LocalDateTime.now());
				LOG.info("Track {} edited to {}", track.getTrackID(), track);
			}
		}
		UpdateMetadataTask updateTask = new UpdateMetadataTask(trackSelection, newCoverImage);
		updateTask.setDaemon(true);
		updateTask.start();
		newCoverImage = null;
		editStage.close();
	}
		
	@FXML
	private void doCancel() {
		LOG.info("Edit stage cancelled");
		newCoverImage = null;
		editStage.close();
	}
	
	private void setFields() {
		ObservableList<Entry<Integer, Track>> selectionEntries = StageDemon.getInstance().getRootController().getSelectedItems();
		trackSelection = selectionEntries.stream().map(Map.Entry::getValue).collect(Collectors.toList());
		List<String> valuesList = new ArrayList<>();
		for(TrackField field: editFieldsMap.keySet()) {
			for(Track t: trackSelection) {
				Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
				Property<?> trackProperty = propertyMap.get(field);
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					IntegerProperty ip = (IntegerProperty) trackProperty;
					if(ip.get() == 0)
						valuesList.add("");
					else
						valuesList.add("" + ip.get());
				}
				else
					valuesList.add("" + trackProperty.getValue());
			}
			editFieldsMap.get(field).textProperty().setValue(matchCommonString(valuesList));
			valuesList.clear();
		}
		// Check for the same cover image and compilation value
		
		byte[] commonCover = matchCommonCover();
		if(commonCover != null)
			coverImage.setImage(new Image(new ByteArrayInputStream(commonCover)));
		else
			coverImage.setImage(defaultImage);

		if(!matchCommonCompilation())
			isCompilationCheckBox.setIndeterminate(true);
		else
			isCompilationCheckBox.setSelected(trackSelection.get(0).getIsCompilation());
	}
	
	private byte[] matchCommonCover() {
		byte[] coverBytes = null;
		String sameAlbum = trackSelection.get(0).getAlbum();
		for(Track t: trackSelection)
			if(!t.getAlbum().equalsIgnoreCase(sameAlbum)) {
				sameAlbum = null;
				break;
			}
		if(sameAlbum != null)
			for(Track t: trackSelection)
				if(t.hasCover())
					coverBytes = t.getCoverBytes();
		return coverBytes;
	}
	
	private boolean matchCommonCompilation() {
		Boolean isCommon = trackSelection.get(0).getIsCompilation();
		if(trackSelection.stream().allMatch(t -> isCommon.equals(t.getIsCompilation())))
			return true;
		else
			return false;
	}

	private String matchCommonString(List<String> list) {
		String commonString;
		if(list.stream().allMatch(st -> st.equalsIgnoreCase(list.get(0))))
			commonString = list.get(0);
		else
			commonString = "-";
		return commonString;
	}
}