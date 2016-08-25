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

package com.transgressoft.musicott;

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.view.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * Singleton class that isolates the creation of the view's components,
 * the access to their controllers and the handling of showing/hiding views
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class StageDemon {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private static StageDemon instance;
	private static ErrorDemon errorDemon = ErrorDemon.getInstance();
	private static MusicLibrary musicLibrary = MusicLibrary.getInstance();

	private Stage mainStage;
	private Stage editStage;
	private Stage progressStage;
	private Stage preferencesStage;

	/**
	 * Stores the controllers of each layout view
	 */
	private Map<String, MusicottController> controllers = new HashMap<>();

	private HostServices hostServices;

	private StageDemon() {}

	public static StageDemon getInstance() {
		if (instance == null)
			instance = new StageDemon();
		return instance;
	}

	Stage getMainStage() {
		return mainStage;
	}

	void setApplicationHostServices(HostServices hostServices) {
		this.hostServices = hostServices;
	}

	public HostServices getApplicationHostServices() {
		return hostServices;
	}

	public RootController getRootController() {
		return (RootController) controllers.get(ROOT_LAYOUT);
	}

	public NavigationController getNavigationController() {
		return (NavigationController) controllers.get(NAVIGATION_LAYOUT);
	}

	public PlayQueueController getPlayQueueController() {
		return (PlayQueueController) controllers.get(PLAYQUEUE_LAYOUT);
	}

	public PlayerController getPlayerController() {
		return (PlayerController) controllers.get(PLAYER_LAYOUT);
	}

	/**
	 * Constructs the main view of the application and shows it
	 *
	 * @param primaryStage The primary Stage given in the launched application
	 *
	 * @throws IOException If any resource was not found
	 */
	void showMusicott(Stage primaryStage) throws IOException {
		mainStage = primaryStage;
		VBox navigationLayout = (VBox) loadLayout(NAVIGATION_LAYOUT);
		LOG.debug("Navigation layout loaded");
		GridPane playerGridPane = (GridPane) loadLayout(PLAYER_LAYOUT);
		LOG.debug("Player layout loaded");
		AnchorPane playQueuePane = (AnchorPane) loadLayout(PLAYQUEUE_LAYOUT);
		getPlayerController().setPlayQueuePane(playQueuePane);
		LOG.debug("Play queue layout loaded");
		BorderPane rootLayout = (BorderPane) loadLayout(ROOT_LAYOUT);
		getRootController().setNavigationPane(navigationLayout);
		LOG.debug("Root layout loaded");

		BorderPane contentBorderLayout = (BorderPane) rootLayout.lookup("#contentBorderLayout");
		contentBorderLayout.setBottom(playerGridPane);
		contentBorderLayout.setLeft(navigationLayout);
		getNavigationController().setNavigationMode(NavigationMode.ALL_TRACKS);

		MusicottMenuBar menuBar = new MusicottMenuBar(mainStage);
		String os = System.getProperty("os.name");
		if (os != null && os.startsWith("Mac"))
			menuBar.macMenuBar();
		else {
			menuBar.defaultMenuBar();
			VBox headerVBox = (VBox) rootLayout.lookup("#headerVBox");
			headerVBox.getChildren().add(0, menuBar);
		}

		// Hide play queue pane clicking outside of it
		navigationLayout.setOnMouseClicked(e -> getPlayerController().hidePlayQueue());
		contentBorderLayout.setOnMouseClicked(e -> getPlayerController().hidePlayQueue());
		rootLayout.setOnMouseClicked(e -> getPlayerController().hidePlayQueue());

		Scene mainScene = new Scene(rootLayout);
		mainStage.setScene(mainScene);
		mainStage.setTitle("Musicott");
		mainStage.getIcons().add(new Image(getClass().getResourceAsStream(MUSICOTT_APP_ICON)));
		mainStage.setMinWidth(1200);
		mainStage.setMinHeight(805);
		mainStage.show();
	}

	/**
	 * Shows the edit window. If the size of track selection is greater than 1,
	 * an <tt>Alert</tt> is opened asking for a confirmation of the user.
	 */
	public void editTracks() {
		List<Track> trackSelection = getRootController().getSelectedTracks();
		if (! trackSelection.isEmpty()) {
			boolean[] edit = {true};
			if (trackSelection.size() > 1) {
				String alertHeader = "Are you sure you want to edit multiple files?";
				Alert alert = createAlert("", alertHeader, "", AlertType.CONFIRMATION);
				Optional<ButtonType> result = alert.showAndWait();

				result.ifPresent(value -> {
					if (value.getButtonData().isCancelButton()) {
						edit[0] = false;
					}
				});
			}

			if (edit[0]) {
				if (editStage == null) {
					editStage = initStage(EDIT_LAYOUT, "Edit");
					((EditController) controllers.get(EDIT_LAYOUT)).setStage(editStage);
				}

				showStage(editStage);
				LOG.debug("Showing edit stage");
			}
		}
	}

	/**
	 * Deletes the tracks selected in the table. An {@link Alert} is opened
	 * asking for a confirmation of the user.
	 */
	public void deleteTracks() {
		List<Track> trackSelection = getRootController().getSelectedTracks();

		if (! trackSelection.isEmpty()) {
			int numDeletedTracks = trackSelection.size();
			String alertHeader = "Delete " + numDeletedTracks + " files from Musicott?";
			Alert alert = createAlert("", alertHeader, "", AlertType.CONFIRMATION);
			alert.getDialogPane().getStylesheets().add(getClass().getResource(DIALOG_STYLE).toExternalForm());
			Optional<ButtonType> result = alert.showAndWait();

			if (result.isPresent() && result.get().getButtonData().isDefaultButton()) {
				new Thread(() -> {
					List<Integer> tracksToDelete = trackSelection.stream().map(Track::getTrackId).collect(Collectors.toList());
					PlayerFacade.getInstance().deleteFromQueues(tracksToDelete);
					musicLibrary.deleteTracks(tracksToDelete);
					Platform.runLater(this::closeIndeterminateProgress);
				}).start();
				showIndeterminateProgress();
			}
			else {
				alert.close();
			}
		}
	}

	/**
	 * Shows the preferences window
	 */
	public void showPreferences() {
		if (preferencesStage == null) {
			preferencesStage = initStage(PREFERENCES_LAYOUT, "Preferences");
			PreferencesController preferencesController = (PreferencesController) controllers.get(PREFERENCES_LAYOUT);
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
			progressStage = initStage(PROGRESS_LAYOUT, "");
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
	 * Creates an {@link Alert} given a title, a header text, the content to be shown
	 * in the description, and the {@link AlertType} of the requested <tt>Alert</tt>
	 *
	 * @param title   The title of the <tt>Alert</tt> stage
	 * @param header  The header text of the <tt>Alert</tt>
	 * @param content The content text of the <tt>Alert</tt>
	 * @param type    The type of the <tt>Alert</tt>
	 *
	 * @return The <tt>Alert</tt> object
	 */
	public Alert createAlert(String title, String header, String content, AlertType type) {
		Alert alert = new Alert(type);
		alert.getDialogPane().getStylesheets().add(getClass().getResource(DIALOG_STYLE).toExternalForm());
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.initOwner(mainStage);
		return alert;
	}

	/**
	 * Shows the given stage and centers it on the screen
	 *
	 * @param stageToShow The Stage to be shown
	 */
	private void showStage(Stage stageToShow) {
		if (stageToShow.equals(mainStage) || stageToShow.equals(progressStage))
			stageToShow.show();
		else if (! stageToShow.isShowing())
			stageToShow.showAndWait();
		stageToShow.centerOnScreen();
	}

	/**
	 * Loads a given layout resource and sets it into a new <tt>Stage</tt> and <tt>Scene</tt>
	 *
	 * @param layout The <tt>*.fxml</tt> source to be loaded
	 *
	 * @return The <tt>Stage</tt> with the layout
	 */
	private Stage initStage(String layout, String title) {
		Stage newStage;
		try {
			Parent nodeLayout = loadLayout(layout);
			Scene scene = new Scene(nodeLayout);
			newStage = new Stage();
			newStage.setTitle(title);
			newStage.initModality(Modality.APPLICATION_MODAL);
			newStage.setScene(scene);
			newStage.setResizable(false);
		}
		catch (IOException exception) {
			LOG.error("Error initiating stage of layout " + layout, exception.getCause());
			errorDemon.showErrorDialog("Error initiating stage of layout " + layout + ":", "", exception);
			newStage = null;
		}
		return newStage;
	}

	/**
	 * Loads the given layout resource
	 *
	 * @param layout The <tt>*.fxml</tt> source to be loaded
	 *
	 * @return The {@link Parent} object that is the root of the layout
	 *
	 * @throws IOException thrown if the <tt>*.fxml</tt> file wasn't found
	 */
	private Parent loadLayout(String layout) throws IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource(layout));
		Parent nodeLayout = loader.load();
		controllers.put(layout, loader.getController());
		return nodeLayout;
	}
}
