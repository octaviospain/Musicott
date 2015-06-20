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

import com.musicott.model.Track;
import com.musicott.model.Track.EditFieldIterator;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
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
	
	private Map<Integer,TextInputControl> fieldMap;
	private Stage editStage;
	private ObservableList<Track> trackSelection;
	
	public EditController() {
	}
	
	@FXML
	private void initialize() {
		fieldMap = new HashMap<Integer,TextInputControl>();
		fieldMap.put(0, name);
		fieldMap.put(1, artist);
		fieldMap.put(2, album);
		fieldMap.put(3, albumArtist);
		fieldMap.put(4, genre);
		fieldMap.put(5, label);
		fieldMap.put(6, year);
		fieldMap.put(7, bpm);
		fieldMap.put(8, trackNum);
		fieldMap.put(9, discNum);
		fieldMap.put(10, comments);
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
			try {
			t.getName().set(name.textProperty().getValue());
			t.getArtist().set(artist.textProperty().getValue());
			t.getAlbum().set(album.textProperty().getValue());
			t.getAlbumArtist().set(albumArtist.textProperty().getValue());
			t.getGenre().set(genre.textProperty().getValue());
			t.getLabel().set(label.textProperty().getValue());
			if(!year.textProperty().getValue().equals(""))
				t.getYear().set(Integer.parseInt(year.textProperty().getValue()));
			if(!bpm.textProperty().getValue().equals(""))
				t.getBPM().set(Integer.parseInt(bpm.textProperty().getValue()));
			if(!trackNum.textFormatterProperty().getValue().equals(""))
				t.getTrackNumber().set(Integer.parseInt(trackNum.textProperty().getValue()));
			if(!discNum.textProperty().getValue().equals(""))
				t.getDiscNumber().set(Integer.parseInt(discNum.textProperty().getValue()));
			t.getComments().set(comments.textProperty().getValue());
			t.getIsCompilation().set(isCompilationCheckBox.isSelected());
			//TODO set image
			}
			catch(NumberFormatException e) {
				//TODO fix non integer values
			}
		}
		else {
			for(int i=0; i<trackSelection.size() ;i++) {
				Track t = trackSelection.get(i);
				try {
					if(!name.textProperty().getValue().equals("-"))
						t.getName().set(name.textProperty().getValue());
					if(!artist.textProperty().getValue().equals("-"))
						t.getArtist().set(artist.textProperty().getValue());
					if(!album.textProperty().getValue().equals("-"))
						t.getAlbum().set(album.textProperty().getValue());
					if(!albumArtist.textProperty().getValue().equals("-"))
						t.getAlbumArtist().set(albumArtist.textProperty().getValue());
					if(!genre.textProperty().getValue().equals("-"))
						t.getGenre().set(genre.textProperty().getValue());
					if(!label.textProperty().getValue().equals("-"))
						t.getLabel().set(label.textProperty().getValue());
					if(!year.textProperty().getValue().equals("-") || year.textProperty().getValue().equals(""))
						t.getYear().set(Integer.parseInt(year.textProperty().getValue()));
					if(!bpm.textProperty().getValue().equals("-") || bpm.textProperty().getValue().equals(""))
						t.getBPM().set(Integer.parseInt(bpm.textProperty().getValue()));
					if(!trackNum.textProperty().getValue().equals("-") || trackNum.textProperty().getValue().equals(""))
						t.getTrackNumber().set(Integer.parseInt(trackNum.textProperty().getValue()));
					if(!discNum.textProperty().getValue().equals("-") || discNum.textProperty().getValue().equals(""))
						t.getDiscNumber().set(Integer.parseInt(discNum.textProperty().getValue()));
					if(!comments.textProperty().getValue().equals("-"))
						t.getComments().set(comments.textProperty().getValue());
					t.getIsCompilation().set(isCompilationCheckBox.isSelected());
					//TODO set image
				} catch (NumberFormatException e) {
					// TODO show error dialog and closes or fix for non accept integer inputs
				}
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
			name.textProperty().setValue(track.getName().get());
			artist.textProperty().setValue(track.getArtist().get());
			album.textProperty().setValue(track.getAlbum().get());
			albumArtist.textProperty().setValue(track.getAlbumArtist().get());
			genre.textProperty().setValue(track.getGenre().get());
			label.textProperty().setValue(track.getLabel().get());
			if(track.getYear().get() == 0)
				year.textProperty().setValue("");
			else
				year.textProperty().setValue(track.getYear().get()+"");
			if(track.getBPM().get() == -1)
				bpm.textProperty().setValue("");
			else
				bpm.textProperty().setValue(track.getBPM().get()+"");
			if(track.getTrackNumber().get() == 0)
				trackNum.textProperty().setValue("");
			else
				trackNum.textProperty().setValue(track.getTrackNumber().get()+"");
			if(track.getDiscNumber().get() == 0)
				discNum.textProperty().setValue("");
			else
				discNum.textProperty().setValue(track.getDiscNumber().get()+"");
			comments.textProperty().setValue(track.getComments().get());
			titleName.setText(track.getName().get());
			titleArtist.setText(track.getArtist().get());
			titleAlbum.setText(track.getAlbum().get());
			isCompilationCheckBox.setSelected(track.getIsCompilation().get());
		//	coverImage.setImage(); //TODO
		}
		else {
			for(int i=0; i<fieldMap.size(); i++) {
				List<String> listOfSameFields = new ArrayList<String>();
				if(i == fieldMap.size()-1) { 				// comments case
					TextArea ta = (TextArea) fieldMap.get(i);
					for(Track t: trackSelection) {
						StringProperty sp = (StringProperty) ((EditFieldIterator) t.editFieldsIterator()).get(i);
						listOfSameFields.add(sp.get());
					}
					ta.textProperty().setValue((matchCommonString(listOfSameFields)));
				}
				else
					if(i<6) {								// string fields
						for(Track t: trackSelection) {
							StringProperty sp = (StringProperty) ((EditFieldIterator) t.editFieldsIterator()).get(i);
							listOfSameFields.add(sp.get());
						}
						if(i == 0)
							titleName.setText(matchCommonString(listOfSameFields));
						if(i == 1)
							titleArtist.setText(matchCommonString(listOfSameFields));
						if(i == 2)
							titleAlbum.setText(matchCommonString(listOfSameFields));
						TextField tf = (TextField) fieldMap.get(i);
						tf.textProperty().setValue((matchCommonString(listOfSameFields)));
					}
					else {									// integer fields
						TextField tf = (TextField) fieldMap.get(i);
						for(Track t: trackSelection) {
							IntegerProperty sp = (IntegerProperty) ((EditFieldIterator) t.editFieldsIterator()).get(i);
							if(sp.get() == 0 || sp.get() == -1)
								listOfSameFields.add("");
							else
								listOfSameFields.add(""+sp.get());
						}
						tf.textProperty().setValue((matchCommonString(listOfSameFields)));
					}
			}
			isCompilationCheckBox.setIndeterminate(true);
			//TODO set default image or common cover image
		}
	}

	private String matchCommonString (List<String> list) {
		String s = list.get(0);
		for(int i=0; i<list.size() ;i++) {
			if(!s.equalsIgnoreCase(list.get(i))) {
					s = "-";
					break;
			}
		}
		return s;
	}
}