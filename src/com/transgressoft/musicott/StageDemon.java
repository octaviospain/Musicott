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

package com.transgressoft.musicott;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.view.*;
import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.stage.*;
import javafx.stage.Stage;
import org.slf4j.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.model.Layout.*;
import static com.transgressoft.musicott.util.Utils.*;

/**
 * Singleton class that isolates the creation of the view's components,
 * the access to their controllers and the handling of showing/hiding views
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class StageDemon {

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

    private Provider<ErrorDemon> errorDemon;
    private Provider<MusicLibrary> musicLibrary;
    private Provider<PlayerFacade> playerFacade;

    private Stage mainStage;
    private Stage progressStage;
    private Stage preferencesStage;

    /**
     * Stores the controllers of each layout view
     */
    private Map<String, MusicottLayout> controllers = new HashMap<>();

    private HostServices hostServices;
    private Injector injector;

    @Inject
    public StageDemon(Provider<ErrorDemon> errorDemon, Provider<MusicLibrary> musicLibrary,
            Provider<PlayerFacade> playerFacade) {
        this.errorDemon = errorDemon;
        this.musicLibrary = musicLibrary;
        this.playerFacade = playerFacade;
    }

    void setInjector(Injector injector) {
        this.injector = injector;
    }

    void setApplicationHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public HostServices getApplicationHostServices() {
        return hostServices;
    }

    public RootController getRootController() {
        return (RootController) controllers.get(ROOT);
    }

    public NavigationController getNavigationController() {
        return getRootController() == null ? null : getRootController().getNavigationController();
    }

    public PlayerController getPlayerController() {
        return getRootController().getPlayerController();
    }

    /**
     * Constructs the main view of the application and shows it
     *
     * @param primaryStage The primary Stage given in the launched application
     *
     * @throws IOException If any resource was not found
     */
//    void showMusicott(Stage primaryStage) throws IOException {
//        mainStage = primaryStage;
//        Parent rootLayout = loadLayout(ROOT_LAYOUT);
//        rootLayout.setOnMouseClicked(e -> getRootController().getPlayerController().hidePlayQueue());
//        getRootController().setStage(mainStage);
//        mainStage.setScene(new Scene(rootLayout));
//        mainStage.setTitle("Musicott");
//        mainStage.getIcons().add(new Image(getClass().getResourceAsStream(MUSICOTT_APP_ICON)));
//        mainStage.setMinWidth(1200);
//        mainStage.setMinHeight(805);
//        mainStage.show();
//    }

    /**
     * Deletes the tracks selected in the table. An {@link Alert} is opened
     * asking for a confirmation of the user.
     */
    public void deleteTracks(List<Track> trackSelection) {
        if (! trackSelection.isEmpty()) {
            String alertHeader = "Delete " + trackSelection.size() + " files from Musicott?";
            Alert alert = createAlert("", alertHeader, "", AlertType.CONFIRMATION, mainStage);
            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
                new Thread(() -> {
                    playerFacade.get().deleteFromQueues(trackSelection);
                    musicLibrary.get().deleteTracks(trackSelection);
                    String message = "Deleted " + Integer.toString(trackSelection.size()) + " tracks";
                    Platform.runLater(() -> {
                        getNavigationController().setStatusMessage(message);
                        closeIndeterminateProgress();
                    });
                }).start();
                showIndeterminateProgress();
            }
            else
                alert.close();
        }
    }

    /**
     * Shows the preferences window
     */
    public void showPreferences() {
        if (preferencesStage == null) {
            preferencesStage = initStage(PREFERENCES.getPath(), "Preferences");
            PreferencesController preferencesController = (PreferencesController) controllers.get(PREFERENCES.getPath());
            preferencesStage.setOnShowing(event -> preferencesController.loadUserPreferences());
        }
        showStage(preferencesStage);
    }

    /**
     * Places a window in front of all the others showing an indeterminate progress.
     * The user is unable to interact with the application until the background task finishes.
     */
    public void showIndeterminateProgress() {
        if (progressStage == null) {
            progressStage = initStage(PROGRESS_BAR.getPath(), "");
            progressStage.initStyle(StageStyle.UNDECORATED);
        }
        showStage(progressStage);
    }

    /**
     * Closes the window with the indeterminate progress
     */
    public void closeIndeterminateProgress() {
        if (progressStage != null)
            progressStage.close();
    }

    /**
     * Shows the given stage and centers it on the screen
     *
     * @param stageToShow The Stage to be shown
     */
    void showStage(Stage stageToShow) {
        Platform.runLater(() -> {
            stageToShow.sizeToScene();
            stageToShow.centerOnScreen();
            if (stageToShow.equals(mainStage) || stageToShow.equals(progressStage))
                stageToShow.show();
            else if (! stageToShow.isShowing())
                stageToShow.showAndWait();
        });
    }

    /**
     * Loads a given layout resource and sets it into a new {@code Stage} and {@code Scene}
     *
     * @param layout The {@code *.fxml} source to be loaded
     *
     * @return The {@code Stage} with the layout
     */
    private Stage initStage(String layout, String title) {
        Stage newStage = new Stage();
        initStage(newStage, layout, title);
        newStage.initModality(Modality.APPLICATION_MODAL);
        return newStage;
    }

    private void initStage(Stage stage, String layout, String title) {
        try {
            Parent nodeLayout = loadLayout(layout);
            Scene scene = new Scene(nodeLayout);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(false);
        }
        catch (IOException exception) {
            LOG.error("Error initiating stage of layout " + layout, exception.getCause());
            errorDemon.get().showErrorDialog("Error initiating stage of layout " + layout + ":", "", exception);
        }
    }

    /**
     * Loads the given layout resource and stores its controller.
     *
     * @param layout The {@code *.fxml} source to be loaded
     *
     * @return The {@link Parent} object that is the root of the layout
     *
     * @throws IOException thrown if the {@code *.fxml} file wasn't found
     */
    private Parent loadLayout(String layout) throws IOException {
        FXGuiceInjectionBuilderFactory factory = new FXGuiceInjectionBuilderFactory(injector);
        FXMLLoader fxmlLoader = new FXMLControllerLoader(getClass().getResource(layout),null, factory, injector);
        Parent nodeLayout = fxmlLoader.load();
        controllers.put(layout, fxmlLoader.getController());
        return nodeLayout;
    }
}
