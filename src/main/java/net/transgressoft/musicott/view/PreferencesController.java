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

package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import net.transgressoft.commons.music.audio.AudioFileType;
import net.transgressoft.musicott.config.*;
import net.transgressoft.musicott.config.*;
import net.transgressoft.musicott.services.lastfm.LastFmService;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.tools.Borders;
import org.fxmisc.easybind.EasyBind;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Controller class of the preferences window.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/PreferencesController.fxml")
@Controller
public class PreferencesController {

    private static final String ITUNES_INFO = "Itunes library";
    private static final String METADATA_INFO = "File metadata";
    private static final String GRANT_PERMISSION = "Grant permission";
    private static final String LOGOUT = "Logout";

    @FXML
    private AnchorPane root;
    @FXML
    private TabPane tabPane;
    @FXML
    private Button okButton;
    @FXML
    private Button lastFmAuthorizationButton;
    @FXML
    private HBox fileFormatsHBox;
    @FXML
    private VBox parentVBox;
    @FXML
    private VBox itunesSectionVBox;
    @FXML
    private CheckBox holdPlayCountCheckBox;
    @FXML
    private CheckBox writeMetadataFromItunesCheckBox;
    @FXML
    private HBox itunesInformationHBox;
    @FXML
    private ComboBox<String> itunesImportPolicyCheckBox;
    @FXML
    private VBox itunesImportOptionsVBox;
    private CheckComboBox<AudioFileType> extensionsCheckComboBox;
    private Stage stage;

    private Supplier<Stage> stageSupplier;
    private ErrorPresenter errorPresenter;
    private SettingsRepository settingsRepository;
    private LastFmService lastFmService;

    @FXML
    public void initialize() {
        itunesImportPolicyCheckBox.setItems(FXCollections.observableArrayList(ITUNES_INFO, METADATA_INFO));
        itunesImportPolicyCheckBox.getSelectionModel().selectedItemProperty().addListener((obs, old, nw) -> {
            if (nw.equals(ITUNES_INFO))
                itunesImportOptionsVBox.setVisible(true);
            else if (nw.equals(METADATA_INFO))
                itunesImportOptionsVBox.setVisible(false);
        });
        wrapItunesSectionWithBorder();

        ObservableList<AudioFileType> selectedExtensions = FXCollections.observableArrayList(AudioFileType.getEntries());
        extensionsCheckComboBox = new CheckComboBox<>(selectedExtensions);
        extensionsCheckComboBox.setMinWidth(100);
        HBox.setHgrow(extensionsCheckComboBox, Priority.SOMETIMES);
        fileFormatsHBox.getChildren().add(extensionsCheckComboBox);

        okButton.setOnAction(event -> saveAndClose());

        if (lastFmService != null)
            enableLastFmTab();
    }

    private void enableLastFmTab() {
        if (lastFmService != null && tabPane != null && lastFmAuthorizationButton != null) {
            tabPane.getTabs().filtered(tab -> tab.getText().equals("LastFM")).stream().findFirst().ifPresent(tab -> tab.setDisable(false));

            EasyBind.subscribe(lastFmService.loggedInProperty(),
                               result -> lastFmAuthorizationButton.setText(result ? LOGOUT : GRANT_PERMISSION));

            lastFmAuthorizationButton.setOnAction(event -> {
                if (lastFmAuthorizationButton.getText().equals(GRANT_PERMISSION)) {
                    lastFmService.logIn()
                            .whenComplete((result, exception) -> {
                                if (exception != null)
                                    errorPresenter.show("LastFM authentication failed", exception);
                                else if (result)
                                    lastFmAuthorizationButton.setText(LOGOUT);
                                else
                                    lastFmAuthorizationButton.setText(GRANT_PERMISSION);
                            });
                } else {
                    lastFmService.logOut();
                    lastFmAuthorizationButton.setText(GRANT_PERMISSION);
                }
            });
        } else {
            tabPane.getTabs().filtered(tab -> tab.getText().equals("LastFM")).stream().findFirst().ifPresent(tab -> tab.setDisable(true));
        }
    }

    /**
     * Wraps the itunes preferences options within a styled border
     *
     * @see <a href="http://controlsfx.bitbucket.org/">ControlsFX</a>
     */
    private void wrapItunesSectionWithBorder() {
        var itunesSectionBorder = Borders.wrap(itunesSectionVBox).etchedBorder().title("Itunes import options").build().build();
        parentVBox.getChildren().remove(itunesSectionVBox);
        parentVBox.getChildren().add(itunesSectionBorder);
    }

    /**
     * Save preferences and closes the window
     */
    private void saveAndClose() {
        String policy = itunesImportPolicyCheckBox.getSelectionModel().getSelectedItem();
        if (policy.equals(ITUNES_INFO))
            settingsRepository.setItunesImportMetadataPolicy(false);
        else if (policy.equals(METADATA_INFO))
            settingsRepository.setItunesImportMetadataPolicy(true);

        var checkedItems = extensionsCheckComboBox.getCheckModel().getCheckedItems();

        settingsRepository.setAcceptedAudioFileExtensions(checkedItems);
        settingsRepository.setItunesImportHoldPlayCountPolicy(holdPlayCountCheckBox.isSelected());
        settingsRepository.setItunesImportWriteMetadataPolicy(writeMetadataFromItunesCheckBox.isSelected());
        okButton.getScene().getWindow().hide();
    }

    private void loadImportPreferences() {
        Set<AudioFileType> importFilterExtensions = settingsRepository.getAcceptedAudioFileExtensions();
        extensionsCheckComboBox.getCheckModel().clearChecks();
        for (AudioFileType audioFileType : importFilterExtensions)
            extensionsCheckComboBox.getCheckModel().check(audioFileType);

        if (settingsRepository.getItunesImportMetadataPolicy())
            itunesImportPolicyCheckBox.getSelectionModel().select(METADATA_INFO);
        else
            itunesImportPolicyCheckBox.getSelectionModel().select(ITUNES_INFO);

        holdPlayCountCheckBox.setSelected(settingsRepository.getItunesImportHoldPlayCountPolicy());
        writeMetadataFromItunesCheckBox.setSelected(settingsRepository.getItunesImportMetadataPolicy());
    }

    public void show() {
        Platform.runLater(() -> {
            if (stage == null) {
                stage = stageSupplier.get();
                Scene scene = new Scene(root);
                // ESC closes the Preferences dialog without persisting changes — saves only happen via the OK button.
                scene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.ESCAPE) {
                        stage.close();
                        event.consume();
                    }
                });
                stage.setScene(scene);
                stage.setTitle("Preferences");
                stage.setResizable(false);
                stage.setOnShowing(event -> loadImportPreferences());
            }
            enableLastFmTab();
            stage.show();
            stage.toFront();
        });
    }

    @Autowired
    public void setStageSupplier(Supplier<Stage> stageSupplier) {
        this.stageSupplier = stageSupplier;
    }

    @Autowired
    public void setErrorPresenter(ErrorPresenter errorPresenter) {
        this.errorPresenter = errorPresenter;
    }

    @Autowired
    public void setSettingsRepository(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Autowired (required = false)
    public void setLastFmService(LastFmService lastFmService) {
        this.lastFmService = lastFmService;
    }
}
