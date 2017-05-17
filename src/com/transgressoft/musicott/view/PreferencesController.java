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
import com.transgressoft.musicott.*;
import com.transgressoft.musicott.services.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import javafx.beans.binding.Binding;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.stage.Stage;
import org.controlsfx.control.*;
import org.controlsfx.tools.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.tasks.parse.itunes.ItunesParseTask.*;
import static org.fxmisc.easybind.EasyBind.*;

/**
 * Controller class of the preferences window.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class PreferencesController extends InjectableController<AnchorPane> implements MusicottLayout {

    private static final String[] EXTENSIONS = {"mp3", "m4a", "wav", "flac"};
    private static final String ITUNES_INFO = "Itunes library";
    private static final String METADATA_INFO = "File metadata";
    private static final String LOGIN = "Login";
    private static final String LOGOUT = "Logout";

    private final ServiceDemon serviceDemon;
    private final TaskDemon taskDemon;
    private final ErrorDialogController errorDialog;

    @FXML
    private AnchorPane root;
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
    private MainPreferences preferences;
    private LastFmPreferences lastFmPreferences;
    private ReadOnlyBooleanProperty usingLastFmProperty;

    @Inject
    public PreferencesController(ServiceDemon serviceDemon, TaskDemon taskDemon, ErrorDialogController errorDialog) {
        this.serviceDemon = serviceDemon;
        this.taskDemon = taskDemon;
        this.errorDialog = errorDialog;
    }

    @FXML
    public void initialize() {
        itunesImportPolicyCheckBox.setItems(FXCollections.observableArrayList(ITUNES_INFO, METADATA_INFO));

        lastFmLoginButton.disableProperty().bind(lastFmLoginButtonDisableBinding());
        lastFmLoginButton.textProperty().bind(map(usingLastFmProperty, using -> using ? LOGOUT : LOGIN));
        lastFmUsernameTextField.disableProperty().bind(usingLastFmProperty);
        lastFmPasswordField.disableProperty().bind(usingLastFmProperty);

        wrapItunesSectionWithBorder();

        ObservableList<String> selectedExtensions = FXCollections.observableArrayList(EXTENSIONS);
        extensionsCheckComboBox = new CheckComboBox<>(selectedExtensions);
        extensionsCheckComboBox.setMinWidth(100);
        HBox.setHgrow(extensionsCheckComboBox, Priority.SOMETIMES);
        fileFormatsHBox.getChildren().add(extensionsCheckComboBox);

        chooseApplicationFolderButton.setOnAction(event -> chooseMusicottFolder());
        okButton.setOnAction(event -> saveAndClose());
        lastFmLoginButton.setOnAction(event -> lastfmLoginOrLogout());

        checkLastFmLoginAtStart();
    }

    @Override
    public void setStage(Stage stage) {
        super.setStage(stage);
        stage.setTitle("Preferences");
        stage.setResizable(false);
        stage.setOnShowing(event -> loadUserPreferences());
    }

    /**
     * Binds the lastFM login button to be disabled when the username or password fields are empty
     *
     * @return The {@link BooleanBinding}
     */
    private Binding<Boolean> lastFmLoginButtonDisableBinding() {
        return combine(lastFmUsernameTextField.textProperty(), lastFmPasswordField.textProperty(),
                                (user, pass) -> user.isEmpty() || pass.isEmpty());
    }

    /**
     * Wraps the itunes preferences options within a styled border
     *
     * @see <a href="http://controlsfx.bitbucket.org/">ControlsFX</a>
     */
    private void wrapItunesSectionWithBorder() {
        Node itunesSectionBorder = Borders.wrap(itunesSectionVBox).etchedBorder().title("Itunes import options").build()
                                          .build();
        parentVBox.getChildren().remove(itunesSectionVBox);
        parentVBox.getChildren().add(itunesSectionBorder);
    }

    private void chooseMusicottFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Musicott folder location");
        Window preferencesWindow = okButton.getScene().getWindow();
        File folder = chooser.showDialog(preferencesWindow);
        if (folder != null)
            folderLocationTextField.setText(folder.getAbsolutePath());
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

    private void lastfmLoginOrLogout() {
        if (lastFmLoginButton.getText().equals(LOGIN)) {
            String lastfmUsername = lastFmUsernameTextField.getText();
            String lastfmPassword = lastFmPasswordField.getText();
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
     * Changes the directory for the application given by the user and re-saves the application files.
     *
     * @param newApplicationUserFolder The new directory for the application
     */
    private void changeMusicottUserFolder(String newApplicationUserFolder) {
        String newApplicationUserFolderPath = newApplicationUserFolder + File.pathSeparator;
        File tracksFile = new File(newApplicationUserFolderPath + TRACKS_PERSISTENCE_FILE);
        if (tracksFile.exists() && ! tracksFile.delete())
            errorDialog.show("Unable to delete tracks file", tracksFile.getAbsolutePath());
        File waveformsFile = new File(newApplicationUserFolderPath + WAVEFORMS_PERSISTENCE_FILE);
        if (waveformsFile.exists() && ! waveformsFile.delete())
            errorDialog.show("Unable to delete waveforms file", waveformsFile.getAbsolutePath());
        File playlistsFile = new File(newApplicationUserFolderPath + PLAYLISTS_PERSISTENCE_FILE);
        if (playlistsFile.exists() && ! playlistsFile.delete())
            errorDialog.show("Unable to delete playlists file", playlistsFile.getAbsolutePath());
        preferences.setMusicottUserFolder(newApplicationUserFolder);
        taskDemon.saveLibrary(true, true, true);
    }

    private void loadUserPreferences() {
        folderLocationTextField.setText(preferences.getMusicottUserFolder());
        loadImportPreferences();
        loadLastFmSettings();
    }

    private void loadImportPreferences() {
        Set<String> importFilterExtensions = preferences.getImportFilterExtensions();
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

    @Inject
    public void setUsingLastFmProperty(@UsingLastFmProperty ReadOnlyBooleanProperty p) {
        usingLastFmProperty = p;
    }

    @Inject
    public void setPreferences(MainPreferences preferences) {
        this.preferences = preferences;
    }

    @Inject
    public void setLastFmPreferences(LastFmPreferences lastFmPreferences) {
        this.lastFmPreferences = lastFmPreferences;
    }
}
