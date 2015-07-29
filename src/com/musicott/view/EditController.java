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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott.view;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.musicott.SceneManager;
import com.musicott.model.Track;
import com.musicott.model.TrackField;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class EditController {

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
	private Stage editStage;
	private ObservableList<Track> trackSelection;
	
	public EditController() {
	}
	
	@FXML
	private void initialize() {
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
	}
	
	public void setStage(Stage stage) {
		editStage = stage;
	}
	
	public void setSelection(ObservableList<Track> selection) {
		trackSelection = selection;
		setFields();
	}

	@FXML
	private void doOK() {
		if(trackSelection.size() == 1) {
			Track t = trackSelection.get(0);
			Map<TrackField, Property<?>> trackPropertiesMap = t.getPropertiesMap();
			boolean changed = false;
			
			for(TrackField field: editFieldsMap.keySet()) {
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					try {
						IntegerProperty ip = (IntegerProperty) trackPropertiesMap.get(field);
						if(ip.get() != Integer.parseInt(editFieldsMap.get(field).textProperty().getValue())) {
							ip.setValue(Integer.parseInt(editFieldsMap.get(field).textProperty().getValue()));
							changed = true;
						}
					} catch(NumberFormatException e) {}
				}
				else {
					StringProperty sp = (StringProperty) trackPropertiesMap.get(field);
					if(!sp.get().equals(editFieldsMap.get(field).textProperty().getValue())) {
						sp.setValue(editFieldsMap.get(field).textProperty().getValue());
						changed = true;
					}
				}
			}
			if(changed)
				t.setDateModified(LocalDate.now());
			
			//TODO set image
			//Changes in the elements of the list doesn't fire the ListChangeListener so save explicitly
			SceneManager.getInstance().saveLibrary();
		}
		else {
			for(int i=0; i<trackSelection.size() ;i++) {
				Track t = trackSelection.get(i);
				Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
				boolean changed = false;
				
				for(TrackField field: editFieldsMap.keySet()) {
					if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
						try {
							IntegerProperty ip = (IntegerProperty) propertyMap.get(field);
							if(!editFieldsMap.get(field).textProperty().getValue().equals("-") || !editFieldsMap.get(field).textProperty().getValue().equals("")
									&& ip.get() != Integer.parseInt(editFieldsMap.get(field).textProperty().getValue())) {
								ip.setValue(Integer.parseInt(editFieldsMap.get(field).textProperty().getValue()));
								changed = true;
							}
						} catch(NumberFormatException e) {}
					}
					else {
						StringProperty sp = (StringProperty) propertyMap.get(field);
						if(!editFieldsMap.get(field).textProperty().getValue().equals("-") && !sp.get().equals(editFieldsMap.get(field).textProperty().getValue())) {
							sp.setValue(editFieldsMap.get(field).textProperty().getValue());
							changed = true;
						}
					}
				}
				if(!isCompilationCheckBox.isIndeterminate()) {
					t.setCompilation(isCompilationCheckBox.isSelected());
					changed = true;
				}
				if(changed)
					t.setDateModified(LocalDate.now());					
				
				//TODO set image
				//Changes in the elements of the list doesn't fire the ListChangeListener so save explicitly
				SceneManager.getInstance().saveLibrary();
			}
		}
		editStage.close();
	}
		
	@FXML
	private void doCancel() {
		editStage.close();
	}
	
	private void setFields() {
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
			
			titleName.textProperty().setValue(track.getName());
			titleArtist.textProperty().setValue(track.getArtist());
			titleAlbum.textProperty().setValue(track.getAlbum());
			
			coverImage.setImage(new Image("file:"+track.getCoverFileName()));
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
				listOfSameFields.clear();
			}
		
			// Check for the same cover image and compilation value
			List<Boolean> listOfSameBools = new ArrayList<Boolean>();
			
			for(Track t: trackSelection) {
				listOfSameFields.add(t.getFileFolder()+"/"+t.getCoverFileName());
				listOfSameBools.add(t.getIsCompilation());
			}
			
			if(!matchCommonString(listOfSameFields).equals("-"))
				coverImage.setImage(new Image("file:"+trackSelection.get(0).getCoverFileName()));
			else
				coverImage.setImage(new Image("file:resources/images/default-cover-icon.png"));
			
			if(!matchCommonBool(listOfSameBools))
				isCompilationCheckBox.setIndeterminate(true);
			else
				isCompilationCheckBox.setSelected(trackSelection.get(0).getIsCompilation());
			
			titleName.textProperty().setValue(name.textProperty().get());
			titleArtist.textProperty().setValue(artist.textProperty().get());
			titleAlbum.textProperty().setValue(album.textProperty().get());
		}
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