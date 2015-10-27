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
 */

package com.musicott.view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.model.Track;
import com.musicott.model.TrackField;
import com.musicott.task.UpdateMetadataTask;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.CacheHint;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;

/**
 * @author Octavio Calleya
 *
 */
public class EditInfoController {
	
	private final Logger LOG = LoggerFactory.getLogger(EditInfoController.class.getName());

	@FXML
	private TextField name;
	@FXML
	private TextField artist;
	@FXML
	private TextField album;
	@FXML
	private TextField albumArtist;
	@FXML
	private TextField genre;
	@FXML
	private TextField label;
	@FXML
	private TextField year;
	@FXML
	private TextField bpm;
	@FXML
	private TextArea comments;
	@FXML
	private TextField trackNum;
	@FXML
	private TextField discNum;
	@FXML
	private Label titleName;
	@FXML
	private Label titleArtist;
	@FXML
	private Label titleAlbum;
	@FXML
	private ImageView coverImage;
	@FXML
	private CheckBox isCompilationCheckBox;
	@FXML
	private Button cancelEditButton;
	@FXML
	private Button okEditButton;
	
	private Map<TrackField,TextInputControl> editFieldsMap;
	private File newCoverImage;
	private Image defaultImage;
	private Stage editStage;
	private List<Track> trackSelection;
	
	public EditInfoController() {
	}
	
	@FXML
	private void initialize() {
		defaultImage = new Image(EditInfoController.class.getResourceAsStream("/images/default-cover-icon.png"));
		editFieldsMap = new HashMap<TrackField,TextInputControl>();
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
		
		coverImage.setImage(defaultImage);
		coverImage.setCacheHint(CacheHint.QUALITY);
		coverImage.setOnMouseClicked(event -> {
			if(event.getClickCount() <= 2) {
				LOG.debug("Choosing cover image");
				FileChooser chooser = new FileChooser();
				chooser.setTitle("Open file(s)...");
				chooser.getExtensionFilters().addAll(new ExtensionFilter("Image files (*.png, *.jpg, *.jpeg)","*.png", "*.jpg", "*.jpeg"));
				newCoverImage = chooser.showOpenDialog(editStage);
				byte[] newCoverBytes;
				try {
					newCoverBytes = Files.readAllBytes(Paths.get(newCoverImage.getPath()));
					if(newCoverImage != null)
						coverImage.setImage(new Image(new ByteArrayInputStream(newCoverBytes)));
				} catch (IOException e) {
					ErrorHandler.getInstance().addError(e, ErrorType.COMMON);
					ErrorHandler.getInstance().showErrorDialog(editStage.getScene(), ErrorType.COMMON);
					LOG.error("Error setting image: "+e.getMessage());
				}
			}
		});
	}
	
	public void setStage(Stage stage) {
		editStage = stage;
	}
	
	public void setSelection(List<Track> selection) {
		trackSelection = selection;
		setFields();
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
/*Numeric fields*/	if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					try {
						IntegerProperty ip = (IntegerProperty) trackPropertiesMap.get(field);
						if(!newValue.equals("-") || (!newValue.equals("") && ip.get() != Integer.parseInt(newValue))) {
							ip.setValue(Integer.parseInt(newValue));
							changed = true;
						}
					} catch(NumberFormatException e) {}
				}
/*String fields*/	else {
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
		editStage.close();
		if(newCoverImage != null) {	// Fires the hasCover property to change the cover in the main scene
			int currentPlayingTrackID = SceneManager.getInstance().getPlayQueueController().getCurrentTrack().getTrackID();
			for(Track t: trackSelection)
				if(currentPlayingTrackID == t.getTrackID()) {
					t.setHasCover(false);
					t.setHasCover(true);
					break;
				}
		}
		UpdateMetadataTask wmTask = new UpdateMetadataTask(trackSelection, newCoverImage);
		Thread t = new Thread(wmTask, "Write Metadata Thread");
		t.setDaemon(true);
		t.start();
	}
		
	@FXML
	private void doCancel() {
		LOG.info("Edit stage cancelled");
		editStage.close();
	}
	
	private void setFields() {
		newCoverImage = null;
		if(trackSelection.size() == 1) {
			Track track = trackSelection.get(0);
			Map<TrackField, Property<?>> propertyMap = track.getPropertiesMap();
			
			for(TrackField field: editFieldsMap.keySet()) {
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					try {
						IntegerProperty ip = (IntegerProperty) propertyMap.get(field);
						if(ip.get() == 0 || ip.get() == -1)
							editFieldsMap.get(field).textProperty().setValue("");
						else
							editFieldsMap.get(field).textProperty().setValue(ip.get()+"");
					} catch(NumberFormatException e) {}
				}
				else {
					StringProperty sp = (StringProperty) propertyMap.get(field);
					editFieldsMap.get(field).textProperty().setValue(sp.get());
				}
			}
			if(track.hasCover())
				coverImage.setImage(new Image(new ByteArrayInputStream(track.getCoverBytes())));
			else
				coverImage.setImage(defaultImage);
		}
		else {
			List<String> listOfSameFields = new ArrayList<String>();
			for(TrackField field: editFieldsMap.keySet()) {
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					for(Track t: trackSelection) {
						Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
						IntegerProperty ip= (IntegerProperty) propertyMap.get(field);
						if(ip.get() == 0 || ip.get() == -1)
							listOfSameFields.add("");
						else
							listOfSameFields.add(""+ip.get());
					}
					editFieldsMap.get(field).textProperty().setValue(matchCommonString(listOfSameFields));
				}
				else {
					for(Track t: trackSelection) {
						Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
						StringProperty ip= (StringProperty) propertyMap.get(field);
						listOfSameFields.add(ip.get());
					}
					editFieldsMap.get(field).textProperty().setValue(matchCommonString(listOfSameFields));
				}
			}
		
			// Check for the same cover image and compilation value
			List<Boolean> listOfSameBools = new ArrayList<Boolean>();
			List<byte[]> listOfSameCover = new ArrayList<byte[]>();
			
			for(Track t: trackSelection) {
				if(t.hasCover())
					listOfSameCover.add(t.getCoverBytes());
				listOfSameBools.add(t.getIsCompilation());
			}
			
			if(matchCommonBytes(listOfSameCover))
				coverImage.setImage(new Image(new ByteArrayInputStream(trackSelection.get(0).getCoverBytes())));
			else
				coverImage.setImage(defaultImage);
			
			if(!matchCommonBool(listOfSameBools))
				isCompilationCheckBox.setIndeterminate(true);
			else
				isCompilationCheckBox.setSelected(trackSelection.get(0).getIsCompilation());
		}
	}
	
	private boolean matchCommonBytes(List<byte[]> list) {
		byte[] bt = list.get(0);
		if(bt == null)
			return false;
		for(byte[] b: list)
			if(b == null || !b.equals(bt))
				return false;
		return true;
	}
	
	private boolean matchCommonBool(List<Boolean> list) {
		boolean isCommon = true;
		Boolean b = trackSelection.get(0).getIsCompilation();
		for(Boolean bl: list)
			if(!b.equals(bl)) {
				isCommon = false;
				break;
			}
		return isCommon;
	}

	private String matchCommonString (List<String> list) {
		String s = list.get(0);
		for(String st: list)
			if(!s.equalsIgnoreCase(st)) {
					s = "-";
					break;
			}
		return s;
	}
}