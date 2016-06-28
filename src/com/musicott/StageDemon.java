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

package com.musicott;

import com.musicott.model.*;
import com.musicott.view.*;
import com.musicott.view.custom.*;
import javafx.application.*;
import javafx.collections.*;
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
import java.util.Map.*;
import java.util.stream.*;

import static com.musicott.view.MusicottController.*;

/**
 * Singleton class that isolates the creation of the view's components,
 * the access to their controllers and the handling of showing/hiding views
 *
 * @author Octavio Calleya
 * @version 0.9
 */
public class StageDemon {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private static StageDemon instance;
	private static ErrorDemon errorDemon;
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
		if(instance == null) {
			instance = new StageDemon();
			errorDemon = ErrorDemon.getInstance();
		}
		return instance;
	}
	
	public Stage getMainStage() {
		return mainStage;
	}

	protected Stage getPreferencesStage() {
		return preferencesStage;
	}

	public void setApplicationHostServices(HostServices hostServices) {
		this.hostServices= hostServices;
	}

	public HostServices getApplicationHostServices() {
		return hostServices;
	}

	public RootController getRootController() {
		return (RootController) controllers.get(ROOT_LAYOUT);
	}

	public EditController getEditController() {
		return (EditController) controllers.get(EDIT_LAYOUT);
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
	
	public PreferencesController getPreferencesController() {
		return (PreferencesController) controllers.get(PREFERENCES_LAYOUT);
	}

	/**
	 * Shows the edit window. If the size of track selection is greater than 1,
	 * an <tt>Alert</tt> is opened asking for a confirmation of the user.
	 */
	public void editTracks() {
		ObservableList<Entry<Integer, Track>> trackSelection = getRootController().getSelectedItems();
		if(!trackSelection.isEmpty()) {
			boolean[] edit = {true};
			if(trackSelection.size() > 1) {
				String alertHeader = "Are you sure you want to edit multiple files?";
				Alert alert = createAlert("", alertHeader, "", AlertType.CONFIRMATION);
				Optional<ButtonType> result = alert.showAndWait();

				result.ifPresent(value -> {
					if(value.getButtonData().isCancelButton())
						edit[0] = false;
				});
			}

			if(edit[0]) {
				if(editStage == null) {
					editStage = initStage(EDIT_LAYOUT, "Edit");
					getEditController().setStage(editStage);
				}

				showStage(editStage);
				LOG.debug("Showing edit stage");
			}
		}
	}

	/**
	 * Deletes the tracks selected in the table. If the size of the track selection
	 * is greater than 1, an <tt>Alert</tt> is opened asking for a confirmation of the user.
	 */
	public void deleteTracks() {
		ObservableList<Map.Entry<Integer, Track>> trackSelection = getRootController().getSelectedItems();

		if(!trackSelection.isEmpty()) {
			int numDeletedTracks = trackSelection.size();
			String alertHeader = "Delete " + numDeletedTracks + " files from Musicott?";
			Alert alert = createAlert("", alertHeader, "", AlertType.CONFIRMATION);
			Optional<ButtonType> result = alert.showAndWait();

			result.ifPresent(a -> {
				boolean delete = a.getButtonData().isDefaultButton();

				if (delete) {
					new Thread(() -> {
						List<Integer> tracksToDelete = trackSelection.stream()
								.map(Map.Entry::getKey)
								.collect(Collectors.toList());
						musicLibrary.deleteTracks(tracksToDelete);
						Platform.runLater(this::closeIndeterminateProgress);
					}).start();
					showIndeterminateProgress();
				} else
					alert.close();
			});
		}
	}

	/**
	 * Constructs the main view of the application and shows it
	 *
	 * @param primaryStage The primary Stage given in the launched application
	 * @throws IOException If any resource was not found
	 */
	protected void showMusicott(Stage primaryStage) throws IOException {
		mainStage = primaryStage;
		VBox navigationLayout = (VBox) loadLayout(NAVIGATION_LAYOUT);
		LOG.debug("Navigation layout loaded");
		GridPane playerGridPane = (GridPane) loadLayout(PLAYER_LAYOUT);
		LOG.debug("Player layout loaded");
		AnchorPane playQueuePane = (AnchorPane) loadLayout(PLAYQUEUE_LAYOUT);
		getPlayerController().setPlayQueuePane(playQueuePane);
		LOG.debug("Playqueue layout loaded");
		BorderPane rootLayout = (BorderPane) loadLayout(ROOT_LAYOUT);
		getRootController().setNavigationPaneVBox(navigationLayout);
		LOG.debug("Root layout loaded");

		BorderPane contentBorderLayout = (BorderPane) rootLayout.lookup("#contentBorderLayout");
		contentBorderLayout.setBottom(playerGridPane);
		contentBorderLayout.setLeft(navigationLayout);
		getNavigationController().showMode(NavigationMode.ALL_SONGS_MODE);

		MusicottMenuBar menuBar = new MusicottMenuBar();
		String os = System.getProperty ("os.name");
		if(os != null && os.startsWith ("Mac"))
			menuBar.macMenuBar();
		else {
			menuBar.defaultMenuBar();
			VBox headerVBox = (VBox) rootLayout.lookup("#headerVBox");
			headerVBox.getChildren().add(0, menuBar);
		}

		// Hide play queue pane clicking outside of it
		navigationLayout.setOnMouseClicked(e -> getPlayerController().showPlayQueue(false));
		contentBorderLayout.setOnMouseClicked(e -> getPlayerController().showPlayQueue(false));
		rootLayout.setOnMouseClicked(e -> getPlayerController().showPlayQueue(false));

		Scene mainScene = new Scene(rootLayout, 1200, 775);
		mainStage.setScene(mainScene);
		mainStage.setTitle("Musicott");
		mainStage.getIcons().add(new Image (getClass().getResourceAsStream(MUSICOTT_ICON)));
		mainStage.setMinWidth(1200);
		mainStage.setMinHeight(790);
		mainStage.setMaxWidth(1800);
		mainStage.show();
	}

	/**
	 * Shows the preferences window
	 */
	public void showPreferences() {
		if(preferencesStage == null) {
			preferencesStage = initStage(PREFERENCES_LAYOUT, "Preferences");
			PreferencesController preferencesController = (PreferencesController) controllers.get(PREFERENCES_LAYOUT);
			preferencesController.setStage(preferencesStage);
		}
		showStage(preferencesStage);
	}

	/**
	 * Places a window in front of all the others showing an indeterminate progress.
	 * The user is unable to interact with the application until the background task finishes.
	 */
	public void showIndeterminateProgress() {
		if(progressStage == null)
				progressStage = initStage(PROGRESS_LAYOUT, "");
		showStage(progressStage);
	}

	/**
	 * Closes the window with the indeterminate progress
	 */
	public void closeIndeterminateProgress() {
		progressStage.close();
	}

	/**
	 * Creates an {@link Alert} given a title, a header text, the content to be shown
	 * in the description, and the {@link AlertType} of the requested <tt>Alert</tt>
	 *
	 * @param title The title of the <tt>Alert</tt> stage
	 * @param header The header text of the <tt>Alert</tt>
	 * @param content The content text of the <tt>Alert</tt>
	 * @param type The type of the <tt>Alert</tt>
	 * @return
	 */
	public Alert createAlert(String title, String header, String content, AlertType type) {
		Alert alert = new Alert(type);
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
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
		if(stageToShow.equals(mainStage))
			stageToShow.show();
		else if(!stageToShow.isShowing())
			stageToShow.showAndWait();
		stageToShow.centerOnScreen();
	}

	/**
	 * Loads a given layout resource and sets it into a new <tt>Stage</tt> and <tt>Scene</tt>
	 *
	 * @param layout The <tt>*.fxml</tt> source to be loaded
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
		catch (IOException e) {
			LOG.error("Error initiating stage of layout " + layout, e);
			errorDemon.showErrorDialog("Error initiating stage of layout " + layout, null, e);
			newStage = null;
		}
		return newStage;
	}

	/**
	 * Loads the given layout resource
	 *
	 * @param layout The <tt>*.fxml</tt> source to be loaded
	 * @return The {@link Parent} object that is the root of the layout
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