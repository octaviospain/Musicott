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

package com.transgressoft.musicott.view;

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.services.lastfm.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
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

import static com.transgressoft.musicott.MusicottApplication.*;
import static com.transgressoft.musicott.tasks.ItunesImportTask.*;

/**
 * Controller class of the preferences window.
 *
 * @author Octavio Calleya
 * @version 0.9-b
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
	private TextField lastFmUsernameTextField;
	@FXML
	private PasswordField lastFmPasswordField;
	@FXML
	private Button chooseApplicationFolderButton;
	@FXML
	private Button okButton;
	@FXML
	private Button lastFmLoginButton;
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
	private ComboBox<String> itunesImportPolicyCheckBox;
	private CheckComboBox<String> extensionsCheckComboBox;
	private ObservableList<String> selectedExtensions;
	private Set<String> importFilterExtensions;
	private LastFmPreferences lastFmPreferences;

	private ReadOnlyBooleanProperty usingLastFmProperty = ServiceDemon.getInstance().usingLastFmProperty();

	@FXML
	public void initialize() {
		lastFmPreferences = serviceDemon.getLastFmPreferences();
		itunesImportPolicyCheckBox.setItems(FXCollections.observableArrayList(ITUNES_INFO, METADATA_INFO));

		lastFmLoginButton.disableProperty().bind(lastFmLoginButtonDisableBinding());
		lastFmLoginButton.textProperty().bind(lastFmLoginButtonTextBinding());
		lastFmUsernameTextField.disableProperty().bind(usingLastFmProperty);
		lastFmPasswordField.disableProperty().bind(usingLastFmProperty);

		wrapItunesSectionWithBorder();

		selectedExtensions = FXCollections.observableArrayList(EXTENSIONS);
		extensionsCheckComboBox = new CheckComboBox<>(selectedExtensions);
		extensionsCheckComboBox.setMinWidth(100);
		HBox.setHgrow(extensionsCheckComboBox, Priority.SOMETIMES);
		fileFormatsHBox.getChildren().add(extensionsCheckComboBox);

		chooseApplicationFolderButton.setOnAction(event -> chooseMusicottFolder());
		okButton.setOnAction(event -> saveAndClose());
		lastFmLoginButton.setOnAction(event -> lastfmLoginOrLogout());

		checkLastFmLoginAtStart();
	}

	/**
	 * Binds the lastFM login button to be disabled when the username or password fields are empty
	 *
	 * @return The {@link BooleanBinding}
	 */
	private BooleanBinding lastFmLoginButtonDisableBinding() {
		return Bindings.createBooleanBinding(
				() -> lastFmUsernameTextField.textProperty().get().isEmpty() || lastFmPasswordField.textProperty()
																								   .get()
																								   .isEmpty(),
				lastFmUsernameTextField.textProperty(), lastFmPasswordField.textProperty());
	}

	/**
	 * Binds the text of the lastFM login button whenever the application is using the service
	 *
	 * @return The {@link StringBinding}
	 */
	private StringBinding lastFmLoginButtonTextBinding() {
		return Bindings.createStringBinding(() -> {
			if (usingLastFmProperty.get())
				return LOGOUT;
			else
				return LOGIN;
		}, usingLastFmProperty);
	}

	/**
	 * Wraps the itunes preferences options within a styled border
	 *
	 * @see <a href="http://controlsfx.bitbucket.org/">ControlsFX</a>
	 */
	private void wrapItunesSectionWithBorder() {
		Node itunesSectionBorder = Borders.wrap(itunesSectionVBox).etchedBorder().title("Itunes import options")
										  .build()
										  .build();
		parentVBox.getChildren().remove(itunesSectionVBox);
		parentVBox.getChildren().add(itunesSectionBorder);
	}

	public void loadUserPreferences() {
		folderLocationTextField.setText(preferences.getMusicottUserFolder());
		loadImportPreferences();
		loadLastFmSettings();
	}

	private void loadImportPreferences() {
		importFilterExtensions = preferences.getImportFilterExtensions();
		extensionsCheckComboBox.getCheckModel().clearChecks();
		for (String extension : importFilterExtensions)
			extensionsCheckComboBox.getCheckModel().check(extension);

		if (preferences.getItunesImportMetadataPolicy() == ITUNES_DATA_POLICY)
			itunesImportPolicyCheckBox.getSelectionModel().select(ITUNES_INFO);
		else if (preferences.getItunesImportMetadataPolicy() == METADATA_POLICY)
			itunesImportPolicyCheckBox.getSelectionModel().select(METADATA_INFO);

		holdPlayCountCheckBox.setSelected(preferences.getItunesImportHoldPlaycount());
		importPlaylistsCheckBox.setSelected(preferences.getItunesImportPlaylists());
	}

	private void loadLastFmSettings() {
		String lastfmUsername = lastFmPreferences.getLastFmUsername();
		String lastfmPassword = lastFmPreferences.getLastFmPassword();
		lastFmUsernameTextField.setText(lastfmUsername == null ? "" : lastfmUsername);
		lastFmPasswordField.setText(lastfmPassword == null ? "" : lastfmPassword);
	}

	private void chooseMusicottFolder() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose Musicott folder location");
		Window preferencesWindow = okButton.getScene().getWindow();
		File folder = chooser.showDialog(preferencesWindow);
		if (folder != null)
			folderLocationTextField.setText(folder.getAbsolutePath());
	}

	private void lastfmLoginOrLogout() {
		if (lastFmLoginButton.getText().equals(LOGIN)) {
			String lastfmUsername = lastFmUsernameTextField.getText();
			String lastfmPassword = lastFmPasswordField.getText();
			stageDemon.showIndeterminateProgress();
			serviceDemon.lastFmLogIn(lastfmUsername, lastfmPassword);
		}
		else {
			serviceDemon.lastFmLogOut();
			lastFmPasswordField.clear();
		}
	}

	private void checkLastFmLoginAtStart() {
		String lastfmUsername = lastFmPreferences.getLastFmUsername();
		String lastfmPassword = lastFmPreferences.getLastFmPassword();
		if (lastfmUsername != null && lastfmPassword != null)
			serviceDemon.lastFmLogIn(lastfmUsername, lastfmPassword);
	}

	/**
	 * Saves the preferences and closes the window
	 */
	private void saveAndClose() {
		String savedMusicottUserFolder = preferences.getMusicottUserFolder();
		String newMusicottUserFolder = folderLocationTextField.getText();
		if (! savedMusicottUserFolder.equals(newMusicottUserFolder))
			changeMusicottUserFolder(newMusicottUserFolder);

		String policy = itunesImportPolicyCheckBox.getSelectionModel().getSelectedItem();
		if (policy.equals(ITUNES_INFO))
			preferences.setItunesImportMetadataPolicy(ITUNES_DATA_POLICY);
		else if (policy.equals(METADATA_INFO))
			preferences.setItunesImportMetadataPolicy(METADATA_POLICY);

		ObservableList<String> checkedItems = extensionsCheckComboBox.getCheckModel().getCheckedItems();

		String[] newExtensions = new String[checkedItems.size()];
		newExtensions = checkedItems.toArray(newExtensions);

		preferences.setImportFilterExtensions(newExtensions);
		preferences.setItunesImportHoldPlaycount(holdPlayCountCheckBox.isSelected());
		preferences.setItunesImportPlaylists(importPlaylistsCheckBox.isSelected());
		okButton.getScene().getWindow().hide();
	}

	/**
	 * Changes the directory for the application given by the user and re-saves the application files.
	 *
	 * @param newApplicationUserFolder The new directory for the application
	 */
	private void changeMusicottUserFolder(String newApplicationUserFolder) {
		String newApplicationUserFoderPath = newApplicationUserFolder + File.pathSeparator;
		File tracksFile = new File(newApplicationUserFoderPath + TRACKS_PERSISTENCE_FILE);
		if (tracksFile.exists())
			tracksFile.delete();
		File waveformsFile = new File(newApplicationUserFoderPath + WAVEFORMS_PERSISTENCE_FILE);
		if (waveformsFile.exists())
			waveformsFile.delete();
		File playlistsFile = new File(newApplicationUserFoderPath + PLAYLISTS_PERSISTENCE_FILE);
		if (playlistsFile.exists())
			playlistsFile.delete();
		preferences.setMusicottUserFolder(newApplicationUserFolder);
		MusicLibrary.getInstance().saveLibrary(true, true, true);
	}
}
