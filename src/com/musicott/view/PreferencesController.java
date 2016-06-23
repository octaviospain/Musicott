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

import com.musicott.*;
import com.musicott.model.*;
import com.musicott.services.*;
import javafx.beans.binding.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.controlsfx.control.*;
import org.controlsfx.tools.*;

import java.io.*;

import static com.musicott.MainApp.*;
import static com.musicott.tasks.ItunesImportTask.*;

/**
 * @author Octavio Calleya
 *
 */
public class PreferencesController {
	
	@FXML
	private TextField folderLocationTextField, lastfmUsernameTextField;
	@FXML
	private PasswordField lastfmPasswordField;
	@FXML
	private Button openLocationButton, okButton, lastfmLoginButton;
	@FXML
	private HBox fileFormatsHBox;
	@FXML
	private VBox parentVBox, itunesSectionVBox;
	@FXML
	private ChoiceBox<String> itunesImportPolicyCheckBox;
	@FXML
	private CheckBox holdPlayCountCheckBox, importPlaylistsCheckBox;
	private CheckComboBox<String> extensionsCheckComboBox;
	private ObservableList<String> selectedExtensions;
	private String[] importFilterExtensions;

	private final String[] EXTENSIONS = {"mp3", "m4a", "wav", "flac"};
	private final String ITUNES_INFO = "Itunes info";
	private final String METADATA_INFO = "File metadata info";
	
	private Services services = Services.getInstance();
	private MainPreferences preferences = MainPreferences.getInstance();
	private Stage preferencesStage;

	public PreferencesController () {}
	
	@FXML
	public void initialize() {		
		itunesImportPolicyCheckBox.setItems(FXCollections.observableArrayList(ITUNES_INFO, METADATA_INFO));
		//	The login button is disabled if the fields are empty
		lastfmLoginButton.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			return lastfmUsernameTextField.textProperty().get().equals("") || lastfmPasswordField.textProperty().get().equals("");
		}, lastfmUsernameTextField.textProperty(), lastfmPasswordField.textProperty()));
		// Username and password fields are disabled while the user is loged in, until is logged out pushing the button.
		lastfmUsernameTextField.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			return lastfmLoginButton.textProperty().get().equals("Logout");
		}, lastfmLoginButton.textProperty()));
		lastfmPasswordField.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			return lastfmLoginButton.textProperty().get().equals("Logout");
		}, lastfmLoginButton.textProperty()));
		
		Node itunesSectionBorder = Borders.wrap(itunesSectionVBox).etchedBorder().title("Itunes import options").build().build();
		parentVBox.getChildren().remove(itunesSectionVBox);
		parentVBox.getChildren().add(itunesSectionBorder);
		selectedExtensions = FXCollections.observableArrayList(EXTENSIONS);
		extensionsCheckComboBox = new CheckComboBox<>(selectedExtensions);
		extensionsCheckComboBox.setMinWidth(100);
		HBox.setHgrow(extensionsCheckComboBox, Priority.SOMETIMES);
		fileFormatsHBox.getChildren().add(extensionsCheckComboBox);
	}
	
	public void setStage(Stage stage) {
		preferencesStage = stage;
		preferencesStage.setOnShowing(e -> load());
	}
	
	public void endLogin(boolean loginSuccesful) {
		lastfmLoginButton.setText(loginSuccesful ? "Logout" : "Login");
	}
	
	private void load() {;
		folderLocationTextField.setText(preferences.getMusicottUserFolder());
		if(preferences.getItunesImportMetadataPolicy() == TUNES_DATA_POLICY)
			itunesImportPolicyCheckBox.getSelectionModel().select(ITUNES_INFO);
		else if(preferences.getItunesImportMetadataPolicy() == METADATA_POLICY)
			itunesImportPolicyCheckBox.getSelectionModel().select(METADATA_INFO);
		holdPlayCountCheckBox.setSelected(preferences.getItunesImportHoldPlaycount());
		importPlaylistsCheckBox.setSelected(preferences.getItunesImportPlaylists());
		importFilterExtensions = preferences.getExtensionsFileFilter().getExtensions();
		extensionsCheckComboBox.getCheckModel().clearChecks();
		for(String extension: importFilterExtensions)
			extensionsCheckComboBox.getCheckModel().check(extension);
		String lfmUserName = services.getLastFMUsername();
		String lfmPassword = services.getLastFMPassword();
		lastfmUsernameTextField.setText(lfmUserName == null ? "" : lfmUserName);
		lastfmPasswordField.setText(lfmPassword == null ? "" : lfmPassword);
		if(services.usingLastFM())
			lastfmLoginButton.setText("Logout");
		else
			lastfmLoginButton.setText("Login");
	}
	
	private void changeMusicottUserFolder() {
		String musicottUserPath = preferences.getMusicottUserFolder();
		if(!musicottUserPath.equals(folderLocationTextField.getText())) {
			File tracksFile = new File(musicottUserPath + File.pathSeparator + TRACKS_PERSISTENCE_FILE);
			if(tracksFile.exists())
				tracksFile.delete();
			File waveformsFile = new File(musicottUserPath + File.pathSeparator + WAVEFORMS_PERSISTENCE_FILE);
			if(waveformsFile.exists())
				waveformsFile.delete();
			preferences.setMusicottUserFolder(folderLocationTextField.getText());
			MusicLibrary.getInstance().saveLibrary(true, true, true);
		}
	}
	
	@FXML
	private void doSelectMusicottFolder() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose Musicott folder location");
		File folder = chooser.showDialog(preferencesStage);
		if(folder != null)
			folderLocationTextField.setText(folder.getAbsolutePath());
	}
	
	@FXML
	private void doLastFMLoginLogout() {
		if(lastfmLoginButton.getText().equals("Login")) {
			services.lastFMLogIn(lastfmUsernameTextField.getText(), lastfmPasswordField.getText());
		}
		else {
			services.lastFMLogOut();
			lastfmPasswordField.clear();
			lastfmLoginButton.setText("Login");
		}
	}
	
	@FXML
	private void doOK() {
		changeMusicottUserFolder();
		String policy = itunesImportPolicyCheckBox.getSelectionModel().getSelectedItem();
		if(policy.equals(ITUNES_INFO))
			preferences.setItunesImportMetadataPolicy(TUNES_DATA_POLICY);
		else if(policy.equals(METADATA_INFO))
			preferences.setItunesImportMetadataPolicy(METADATA_POLICY);
		preferences.setItunesImportHoldPlaycount(holdPlayCountCheckBox.isSelected());
		preferences.setItunesImportPlaylists(importPlaylistsCheckBox.isSelected());
		ObservableList<String> checkedItems = extensionsCheckComboBox.getCheckModel().getCheckedItems();
		String[] newExtensions;
		if(checkedItems.isEmpty())
			newExtensions = new String[] {};
		else {
			newExtensions = new String[checkedItems.size()];
			for(int i=0; i<checkedItems.size() ; i++)
				newExtensions[i] = checkedItems.get(i);
		}
		preferences.setImportFilterExtensions(newExtensions);
		preferencesStage.close();
	}
}
