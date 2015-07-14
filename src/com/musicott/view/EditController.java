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
	
	private Map<TrackField,TextInputControl> fieldMap;
	private Stage editStage;
	private ObservableList<Track> trackSelection;
	
	public EditController() {
	}
	
	@FXML
	private void initialize() {
		fieldMap = new HashMap<TrackField,TextInputControl>();
		fieldMap.put(TrackField.NAME, name);
		fieldMap.put(TrackField.ARTIST, artist);
		fieldMap.put(TrackField.ALBUM, album);
		fieldMap.put(TrackField.GENRE, genre);
		fieldMap.put(TrackField.COMMENTS, comments);
		fieldMap.put(TrackField.ALBUM_ARTIST, albumArtist);
		fieldMap.put(TrackField.LABEL, label);
		fieldMap.put(TrackField.TRACK_NUMBER, trackNum);
		fieldMap.put(TrackField.DISC_NUMBER, discNum);
		fieldMap.put(TrackField.YEAR, year);
		fieldMap.put(TrackField.BPM, bpm);
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
			Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
			
			for(TrackField field: fieldMap.keySet()) {
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					try {
						IntegerProperty ip = (IntegerProperty) propertyMap.get(field);
						ip.setValue(Integer.parseInt(fieldMap.get(field).textProperty().getValue()));
					} catch(NumberFormatException e) {}
				}
				else {
					StringProperty sp = (StringProperty) propertyMap.get(field);
					sp.setValue(fieldMap.get(field).textProperty().getValue());
				}
			}
			
			//TODO set image
			//Changes in the elements of the list doesn't fire the ListChangeListener so save explicitly
			SceneManager.getInstance().saveLibrary();
		}
		else {
			for(int i=0; i<trackSelection.size() ;i++) {
				Track t = trackSelection.get(i);
				Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
				
				for(TrackField field: fieldMap.keySet()) {
					if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
						try {
							IntegerProperty ip = (IntegerProperty) propertyMap.get(field);
							if(!fieldMap.get(field).textProperty().getValue().equals("-") || !fieldMap.get(field).textProperty().getValue().equals(""))
								ip.setValue(Integer.parseInt(fieldMap.get(field).textProperty().getValue()));
						} catch(NumberFormatException e) {}
					}
					else {
						StringProperty sp = (StringProperty) propertyMap.get(field);
						if(!fieldMap.get(field).textProperty().getValue().equals("-"))
							sp.setValue(fieldMap.get(field).textProperty().getValue());
					}
				}
				
				if(!isCompilationCheckBox.isIndeterminate())
					t.setCompilation(isCompilationCheckBox.isSelected());
				
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
			
			for(TrackField field: fieldMap.keySet()) {
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					try {
						IntegerProperty ip = (IntegerProperty) propertyMap.get(field);
						if(ip.get() == 0 || ip.get() == -1)
							fieldMap.get(field).textProperty().setValue("");
						else
							fieldMap.get(field).textProperty().setValue(ip.get()+"");
					} catch(NumberFormatException e) {}
				}
				else {
					StringProperty sp = (StringProperty) propertyMap.get(field);
					fieldMap.get(field).textProperty().setValue(sp.get());
				}
			}
			
			titleName.textProperty().setValue(track.getName());
			titleArtist.textProperty().setValue(track.getArtist());
			titleAlbum.textProperty().setValue(track.getAlbum());
			
			if(track.getHasCover())
				coverImage.setImage(new Image("file:"+track.getFileFolder()+"/"+track.getCoverFileName()));
			else
				coverImage.setImage(new Image("file:resources/images/default-cover-icon.png"));
		}
		else {
			List<String> listOfSameFields = new ArrayList<String>();
			for(TrackField field: fieldMap.keySet()) {
				if(field == TrackField.TRACK_NUMBER || field == TrackField.DISC_NUMBER || field == TrackField.YEAR || field == TrackField.BPM) {
					for(Track t: trackSelection) {
						Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
						IntegerProperty ip= (IntegerProperty) propertyMap.get(field);
						if(ip.get() == 0 || ip.get() == -1)
							listOfSameFields.add("");
						else
							listOfSameFields.add(""+ip.get());
					}
					fieldMap.get(field).textProperty().setValue(matchCommonString(listOfSameFields));
				}
				else {
					for(Track t: trackSelection) {
						Map<TrackField, Property<?>> propertyMap = t.getPropertiesMap();
						StringProperty ip= (StringProperty) propertyMap.get(field);
						listOfSameFields.add(ip.get());
					}
					fieldMap.get(field).textProperty().setValue(matchCommonString(listOfSameFields));
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
				coverImage.setImage(new Image("file:"+trackSelection.get(0).getFileFolder()+"/"+trackSelection.get(0).getCoverFileName()));
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