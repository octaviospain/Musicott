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
 */

package com.musicott.view.custom;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;
import com.musicott.model.Track;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Encapsulates the creation of the primary scene of Musicott application
 * 
 * @author Octavio Calleya
 *
 */
public class MusicottScene extends Scene {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	public static final String ALL_SONGS_MODE = "All songs";
	
	private SceneManager sc;
	private MusicLibrary ml;
	private Stage rootStage;
	
	private MenuItem showHideTableInfoPaneMI;
	private BorderPane contentBorderLayout, tableBorderPane;
	private VBox playlistsVBox, navigationVBox;
	private Button newPlaylistButton;
	private ImageView playlistCover;
	private NavigationMenuListView navigationMenuListView;
	private ListProperty<String> selectedMenuProperty;
	private ObservableList<String> selectedMenu;
	private PlaylistTreeView playlistTreeView;
	private TrackTableView trackTable;

	/**
	 * Default constructor for the primary scene
	 * 
	 * @param rootLayout The root parent node of the layout
	 * @param windowWidth The width of the scene
	 * @param windowHeight The height of the scene
	 * @param hostServices The JavaFx Application internet host services
	 */
	public MusicottScene(BorderPane rootLayout, double windowWidth, double windowHeight, HostServices hostServices) {
		super(rootLayout, windowWidth, windowHeight);
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();		
		
		contentBorderLayout = (BorderPane) rootLayout.getCenter();
		tableBorderPane = (BorderPane) contentBorderLayout.getCenter();
		playlistCover = (ImageView) lookup("#playlistCover");
		newPlaylistButton = (Button) lookup("#newPlaylistButton");
		
		newPlaylistButton.setOnMouseClicked(e -> {
			sc.getRootController().setNewPlaylistMode();
			playlistTreeView.getSelectionModel().clearAndSelect(-1);
		});
		
		rootStage = sc.getMainStage();
		playlistTreeView = new PlaylistTreeView(this);
		navigationMenuListView = new NavigationMenuListView(this);
		
		// Show menu mode list
		navigationMenuListView.setItems(FXCollections.observableArrayList(ALL_SONGS_MODE));
		selectedMenu = navigationMenuListView.getSelectionModel().getSelectedItems();
		selectedMenuProperty = new SimpleListProperty<>();
		selectedMenuProperty.bind(new SimpleObjectProperty<>(selectedMenu));
		
		// Table
		trackTable = new TrackTableView();
		TrackTableViewContextMenu trackTableCM = new TrackTableViewContextMenu(this);
		trackTableCM.getItems().stream()
							   .filter(m -> m.getId() != null && m.getId().equals("cmDeleteFromPlaylist"))
							   .findAny().get().disableProperty().bind(selectedMenuProperty.emptyProperty().not());
		trackTable.setContextMenu(trackTableCM);
		trackTable.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			if(event.getButton() == MouseButton.SECONDARY)
				trackTableCM.show(trackTable, event.getScreenX(), event.getScreenY());
			else if(event.getButton() == MouseButton.PRIMARY && trackTableCM.isShowing())
				trackTableCM.hide();
		});
		tableBorderPane.setCenter(trackTable);	
		MusicottMenuBar menuBar = new MusicottMenuBar(this, hostServices);
		showHideTableInfoPaneMI = menuBar.getShowHideTableInfoPaneMI();
		
		// Playlists pane
		playlistTreeView.getSelectionModel().selectedItemProperty().addListener(listener -> {
			navigationMenuListView.getSelectionModel().clearAndSelect(-1);
			sc.getRootController().showTableInfoPane(true);
			showHideTableInfoPaneMI.setDisable(false);
			showHideTableInfoPaneMI.setText(showHideTableInfoPaneMI.getText().replaceFirst("Show", "Hide"));
			if(playlistTreeView.getSelectedPlaylist() != null) {
				sc.getRootController().setPlaylistTitle(playlistTreeView.getSelectedPlaylist().getName());
				sc.getRootController().setPlaylistTitleEdit(false);
				playlistCover.imageProperty().bind(playlistTreeView.getSelectedPlaylist().playlistCoverProperty());
			}
		});

		navigationVBox = (VBox) lookup("#navigationVBox");
		navigationVBox.getChildren().add(1, navigationMenuListView);
		
		playlistsVBox = (VBox) lookup("#playlistsVBox");
		playlistsVBox.getChildren().add(1, playlistTreeView);
		
		VBox.setVgrow(playlistTreeView, Priority.ALWAYS);
		VBox.setVgrow(navigationVBox, Priority.ALWAYS);
		
		showMode(ALL_SONGS_MODE);
	}

	/**
	 * Creates the play queue pane and configures it
	 */
	public void buildPlayQueuePane() {
		// Set the play queue layout on the rootlayout
		LOG.debug("Play queue layout loaded");
		AnchorPane playQueuePane = (AnchorPane) lookup("#playQueuePane");
		ToggleButton playQueueButton = (ToggleButton) lookup("#playQueueButton");
		playQueuePane.setVisible(false);
		playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
		playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
		LOG.debug("Play queue placed"); 
		
		// The play queue pane moves if the window is resized
		rootStage.widthProperty().addListener((observable, oldValue, newValue) -> {
			playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
			playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
		});
		rootStage.heightProperty().addListener((observable, oldValue, newValue) -> {
			playQueuePane.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
			playQueuePane.setLayoutY(playQueueButton.getLayoutY()+50);
		});
		LOG.debug("Configured play queue pane to move with the root layout");
		
		// Closes the play queue pane when click on the view
		/*rootLayout.*/setOnMouseClicked(event -> {if(playQueuePane.isVisible()) {playQueuePane.setVisible(false); playQueueButton.setSelected(false);}});
//		trackTable.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) {playQueuePane.setVisible(false); playQueueButton.setSelected(false);}});
		LOG.debug("Configured play queue pane to close if the user click outside it");
	}
	
	public ObservableList<Map.Entry<Integer, Track>> getTrackSelection() {
		return trackTable.getSelectionModel().getSelectedItems();
	}
	
	public Playlist getSelectedPlaylist() {
		return playlistTreeView.getSelectedPlaylist();
	}
	
	public void addPlaylist(Playlist playlist) {
		playlistTreeView.addPlaylist(playlist);;
	}
	
	/**
	 * Changes the show mode of Musicott 
	 * @param mode
	 */
	protected void showMode(String mode) {
		if(mode.equals(ALL_SONGS_MODE))
			ml.showMode(ALL_SONGS_MODE);
		Platform.runLater(() -> sc.getRootController().showTableInfoPane(false));
		showHideTableInfoPaneMI.setDisable(true);
		playlistTreeView.getSelectionModel().clearAndSelect(-1);
		navigationMenuListView.getSelectionModel().clearAndSelect(0);
	}

	/**
	 * Handles the deletion of a playlist on the playlist tree view
	 */
	protected void deletePlaylist() {
		playlistTreeView.deletePlaylist();
	}
	
	/**
	 * Handles the deletion of the selected tracks in the table of the library
	 */
	protected void deleteFromPlaylist() {
		if(!getTrackSelection().isEmpty())
			ml.removeFromPlaylist(playlistTreeView.getSelectedPlaylist(), getTrackSelection().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
	}
	
	/**
	 * Handles edit action
	 */
	protected void doEdit() {
		if(getTrackSelection() != null & !getTrackSelection().isEmpty()) {
			if(getTrackSelection().size() > 1) {
				Alert alert = createAlert("", "Are you sure you want to edit multiple files?", "", AlertType.CONFIRMATION);
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK) {
					sc.openEditScene(getTrackSelection().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
					LOG.debug("Opened edit stage for various tracks");
				}
				else
					alert.close();
			}
			else {
				sc.openEditScene(getTrackSelection().stream().map(Map.Entry::getValue).collect(Collectors.toList()));
				LOG.debug("Opened edit stage for a single track");
			}
		}
	}
	
	/**
	 * Handles delete action
	 */
	protected void doDelete() {
		if(getTrackSelection() != null && !getTrackSelection().isEmpty()) {
			int numDeletedTracks = getTrackSelection().size();
			Alert alert = createAlert("", "Delete "+numDeletedTracks+" files from Musicott?", "", AlertType.CONFIRMATION);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				new Thread(() -> {
					ml.removeTracks(getTrackSelection().stream().map(Map.Entry::getKey).collect(Collectors.toList()));
					Platform.runLater(() -> sc.closeIndeterminatedProgressScene());
				}).start();
				sc.openIndeterminatedProgressScene();
			}
			else
				alert.close();
		}
	}

	/**
	 * Auxiliary function to create an Alert
	 * 
	 */
	protected Alert createAlert(String title, String header, String content, AlertType type) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.initOwner(rootStage);
		return alert;
	}
}