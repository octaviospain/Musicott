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
import java.util.*;

import static com.musicott.MainApp.*;
import static com.musicott.tasks.ItunesImportTask.*;

/**
 * Controller class of the preferences window
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class PreferencesController implements MusicottController {

	private static final String[] EXTENSIONS = {"mp3", "m4a", "wav", "flac"};
	private static final String ITUNES_INFO = "Itunes library";
	private static final String METADATA_INFO = "File metadata";
	private static final String LOGIN = "Login";
	private static final String LOGOUT = "Logout";
	
	@FXML
	private TextField folderLocationTextField;
	@FXML
	private TextField lastfmUsernameTextField;
	@FXML
	private PasswordField lastfmPasswordField;
	@FXML
	private Button chooseApplicationFolderButton;
	@FXML
	private Button okButton;
	@FXML
	private Button lastfmLoginButton;
	@FXML
	private HBox fileFormatsHBox;
	@FXML
	private VBox parentVBox;
	@FXML
	private VBox itunesSectionVBox;
	@FXML
	private CheckBox holdPlayCountCheckBox;
	@FXML
	private CheckBox importPlaylistsCheckBox;
	@FXML
	private ChoiceBox<String> itunesImportPolicyCheckBox;
	private CheckComboBox<String> extensionsCheckComboBox;
	private ObservableList<String> selectedExtensions;
	private Set<String> importFilterExtensions;

	private Stage preferencesStage;
	
	@FXML
	public void initialize() {		
		itunesImportPolicyCheckBox.setItems(FXCollections.observableArrayList(ITUNES_INFO, METADATA_INFO));

		lastfmLoginButton.disableProperty().bind(lastfmLoginButtonBinding());
		lastfmUsernameTextField.disableProperty().bind(lastfmUsernameFieldBinding());
		lastfmPasswordField.disableProperty().bind(lastfmPasswordFieldBinding());

		wrapItunesSectionWithBorder();

		selectedExtensions = FXCollections.observableArrayList(EXTENSIONS);
		extensionsCheckComboBox = new CheckComboBox<>(selectedExtensions);
		extensionsCheckComboBox.setMinWidth(100);
		HBox.setHgrow(extensionsCheckComboBox, Priority.SOMETIMES);
		fileFormatsHBox.getChildren().add(extensionsCheckComboBox);

		chooseApplicationFolderButton.setOnAction(event -> chooseMusicottFolder());
		okButton.setOnAction(event -> saveAndClose());
		lastfmLoginButton.setOnAction(event -> lastfmLoginOrLogout());
	}

	/**
	 * Binds the lastFM login button to be disabled when the username or password fields are empty
	 *
	 * @return The {@link BooleanBinding}
	 */
	private BooleanBinding lastfmLoginButtonBinding() {
		return Bindings.createBooleanBinding(
				() -> lastfmUsernameTextField.textProperty().get().isEmpty() ||
						lastfmPasswordField.textProperty().get().isEmpty(),
				lastfmUsernameTextField.textProperty(), lastfmPasswordField.textProperty());
	}

	/**
	 * Binds the lastFM username field to be disabled when the user is already loged in,
	 * until he or she logs out.
	 *
	 * @return The {@link BooleanBinding}
	 */
	private BooleanBinding lastfmUsernameFieldBinding() {
		return Bindings.createBooleanBinding(
				() -> lastfmLoginButton.textProperty().get().equals(LOGOUT), lastfmLoginButton.textProperty());
	}

	/**
	 * Binds the lastFM password field to be disabled when the user is already loged in,
	 * until he or she logs out.
	 *
	 * @return The {@link BooleanBinding}
	 */
	private BooleanBinding lastfmPasswordFieldBinding() {
		return Bindings.createBooleanBinding(
				() -> lastfmLoginButton.textProperty().get().equals(LOGOUT), lastfmLoginButton.textProperty());
	}

	/**
	 * Wraps the itunes preferences options within a styled border
	 *
	 * @see <a href="http://controlsfx.bitbucket.org/">ControlsFX</a>
	 */
	private void wrapItunesSectionWithBorder() {
		Node itunesSectionBorder = Borders.wrap(itunesSectionVBox).etchedBorder().title("Itunes import options").build().build();
		parentVBox.getChildren().remove(itunesSectionVBox);
		parentVBox.getChildren().add(itunesSectionBorder);
	}

	public void setStage(Stage stage) {
		preferencesStage = stage;
		preferencesStage.setOnShowing(event -> loadUserPreferences());
	}

	public void setLoginTextOnLoginButton() {
		lastfmLoginButton.setText(LOGIN);
	}

	public void setLogoutTextOnLoginButton() {
		lastfmLoginButton.setText(LOGOUT);
	}
	
	private void loadUserPreferences() {
		folderLocationTextField.setText(preferences.getMusicottUserFolder());
		loadImportPreferences();
		loadLastFmSettings();
	}

	private void loadImportPreferences() {
		importFilterExtensions = preferences.getImportFilterExtensions();
		extensionsCheckComboBox.getCheckModel().clearChecks();
		for(String extension: importFilterExtensions)
			extensionsCheckComboBox.getCheckModel().check(extension);

		if(preferences.getItunesImportMetadataPolicy() == ITUNES_DATA_POLICY)
			itunesImportPolicyCheckBox.getSelectionModel().select(ITUNES_INFO);
		else if(preferences.getItunesImportMetadataPolicy() == METADATA_POLICY)
			itunesImportPolicyCheckBox.getSelectionModel().select(METADATA_INFO);

		holdPlayCountCheckBox.setSelected(preferences.getItunesImportHoldPlaycount());
		importPlaylistsCheckBox.setSelected(preferences.getItunesImportPlaylists());
	}

	private void loadLastFmSettings() {
		String lastfmUsername = services.getLastFMUsername();
		String lastfmPassword = services.getLastFMPassword();
		lastfmUsernameTextField.setText(lastfmUsername == null ? "" : lastfmUsername);
		lastfmPasswordField.setText(lastfmPassword == null ? "" : lastfmPassword);
		if(services.usingLastFM())
			lastfmLoginButton.setText(LOGOUT);
		else
			lastfmLoginButton.setText(LOGIN);
	}

	private void chooseMusicottFolder() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose Musicott folder location");
		File folder = chooser.showDialog(preferencesStage);
		if(folder != null)
			folderLocationTextField.setText(folder.getAbsolutePath());
	}

	private void lastfmLoginOrLogout() {
		if(lastfmLoginButton.getText().equals(LOGIN)) {
			String lastfmUsername = lastfmUsernameTextField.getText();
			String lastfmPassword = lastfmUsernameTextField.getText();
			services.lastFMLogIn(lastfmUsername, lastfmPassword);
		}
		else {
			services.lastFMLogOut();
			lastfmPasswordField.clear();
			lastfmLoginButton.setText(LOGOUT);
		}
	}

	/**
	 * Saves the preferences and closes the window
	 */
	private void saveAndClose() {
		String savedMusicottUserFolder = preferences.getMusicottUserFolder();
		String newMusicottUserFolder = folderLocationTextField.getText();
		if(!savedMusicottUserFolder.equals(newMusicottUserFolder))
			changeMusicottUserFolder(newMusicottUserFolder);

		String policy = itunesImportPolicyCheckBox.getSelectionModel().getSelectedItem();
		if(policy.equals(ITUNES_INFO))
			preferences.setItunesImportMetadataPolicy(ITUNES_DATA_POLICY);
		else if(policy.equals(METADATA_INFO))
			preferences.setItunesImportMetadataPolicy(METADATA_POLICY);

		ObservableList<String> checkedItems = extensionsCheckComboBox.getCheckModel().getCheckedItems();

		String[] newExtensions = new String[checkedItems.size()];
		newExtensions = checkedItems.toArray(newExtensions);

		preferences.setImportFilterExtensions(newExtensions);
		preferences.setItunesImportHoldPlaycount(holdPlayCountCheckBox.isSelected());
		preferences.setItunesImportPlaylists(importPlaylistsCheckBox.isSelected());
		preferencesStage.close();
	}

	/**
	 * Changes the directory for the application given by the user and re-saves the application files.
	 *
	 * @param newApplicationUserFolder The new directory for the application
	 */
	private void changeMusicottUserFolder(String newApplicationUserFolder) {
		String newApplicationUserFoderPath = newApplicationUserFolder + File.pathSeparator;
		File tracksFile = new File(newApplicationUserFoderPath + TRACKS_PERSISTENCE_FILE);
		if(tracksFile.exists())
			tracksFile.delete();
		File waveformsFile = new File(newApplicationUserFoderPath + WAVEFORMS_PERSISTENCE_FILE);
		if(waveformsFile.exists())
			waveformsFile.delete();
		File playlistsFile = new File(newApplicationUserFoderPath + PLAYLISTS_PERSISTENCE_FILE);
		if(playlistsFile.exists())
				playlistsFile.delete();
		preferences.setMusicottUserFolder(newApplicationUserFolder);
		MusicLibrary.getInstance().saveLibrary(true, true, true);
	}
}
