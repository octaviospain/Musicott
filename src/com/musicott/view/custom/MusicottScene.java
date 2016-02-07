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

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.MainPreferences;
import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Playlist;
import com.musicott.model.Track;
import com.musicott.player.PlayerFacade;
import com.musicott.task.TaskPoolManager;
import com.musicott.util.Utils;

import de.codecentric.centerdevice.MenuToolkit;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination.Modifier;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
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
	private HostServices hostServices;
	
	private Menu fileMN, editMN, controlsMN, viewMN, aboutMN;
	private MenuItem openFileMI, importFolderMI, importItunesMI, preferencesMI, editMI, deleteMI, prevMI, nextMI, volIncrMI, volDecrMI, selCurrMI, aboutMI;
	private MenuItem newPlaylistMI, showHideNavigationPaneMI, showHideTableInfoPaneMI;
	
	private BorderPane rootLayout, contentBorderLayout, tableBorderPane;
	private VBox navigationPaneVBox, playlistInfoVBox;
	private Button newPlaylistButton;
	private TextField playlistTitleTextField;
	private Label playlistTitleLabel, playlistTracksNumberLabel, playlistSizeLabel;
	private ImageView playlistCover;
	private ListView<String> showMenuListView;
	private ListProperty<String> selectedMenuProperty;
	private ObservableList<String> selectedMenu;
	private ObservableList<Map.Entry<Integer, Track>> trackSelection;
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
	public MusicottScene(Parent rootLayout, double windowWidth, double windowHeight, HostServices hostServices) {
		super(rootLayout, windowWidth, windowHeight);
		this.rootLayout = (BorderPane) rootLayout;
		this.hostServices = hostServices;
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();		
		
		contentBorderLayout = (BorderPane) this.rootLayout.getCenter();
		tableBorderPane = (BorderPane) contentBorderLayout.getCenter();
		navigationPaneVBox = (VBox) contentBorderLayout.getLeft();
		playlistInfoVBox = (VBox) this.rootLayout.lookup("#playlistInfoVBox");
		playlistTitleLabel = (Label) this.rootLayout.lookup("#playlistTitleLabel");
		playlistTracksNumberLabel = (Label) this.rootLayout.lookup("#playlistTracksNumberLabel");
		playlistSizeLabel = (Label) this.rootLayout.lookup("#playlistSizeLabel");
		playlistCover = (ImageView) this.rootLayout.lookup("#playlistCover");
		newPlaylistButton = (Button) navigationPaneVBox.getChildren().get(0);
		newPlaylistButton.setOnMouseClicked(e -> {
			sc.getRootController().showTableInfoPane(true);
			doAddNewPlaylist();
			playlistTreeView.getSelectionModel().clearAndSelect(-1);
		});
		
		playlistTracksNumberLabel.textProperty().bind(Bindings.createStringBinding(() -> ml.getShowingTracksProperty().sizeProperty().get()+" songs", ml.getShowingTracksProperty()));
		playlistSizeLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			String sizeString = Utils.byteSizeString(ml.getShowingTracksProperty().stream().mapToLong(t -> (long)t.getValue().getSize()).sum(), 2);
			if(sizeString.equals("0 B"))
				sizeString = "";
			return sizeString; 
		},  ml.getShowingTracksProperty()));
		playlistTitleLabel.setOnMouseClicked(event -> {
			if(event.getClickCount() == 2) {	// 2 clicks = edit playlist name
				setPlaylistTitleEdit(true);
				playlistTitleTextField.setOnKeyPressed(e -> {
					Playlist playlist = playlistTreeView.getSelectedPlaylist();
					String newTitle = playlistTitleTextField.getText();
					if(e.getCode() == KeyCode.ENTER && newTitle.equals(playlist.getName())) {
						setPlaylistTitleEdit(false);
						event.consume();
					} else if(e.getCode() == KeyCode.ENTER && !newTitle.equals("") && !ml.containsPlaylist(newTitle)) {
						playlist.setName(newTitle);
						ml.saveLibrary(false, false, true);
						setPlaylistTitleEdit(false);
						event.consume();
					}
				});
			}
		});
		
		playlistTitleTextField = new TextField();
		playlistTitleTextField.setPrefWidth(150);
		playlistTitleTextField.setPrefHeight(25);
		playlistTitleTextField.setPadding(new Insets(-10, 0, -10, 0));
		playlistTitleTextField.setFont(new Font("System", 20));
		playlistTitleLabel.textProperty().bind(playlistTitleTextField.textProperty());
		VBox.setMargin(playlistTitleTextField, new Insets(30, 0, 5, 15));
		
		rootStage = sc.getMainStage();
		playlistTreeView = new PlaylistTreeView();
		showMenuListView = new ListView<String>();
		trackTable = new TrackTableView();
		trackSelection = trackTable.getSelectionModel().getSelectedItems();
		tableBorderPane.setCenter(trackTable);	
		
		// Show menu mode list
		showMenuListView.setPrefHeight(200);
		showMenuListView.setPrefWidth(150);
		showMenuListView.setItems(FXCollections.observableArrayList(ALL_SONGS_MODE));
		selectedMenu = showMenuListView.getSelectionModel().getSelectedItems();
		selectedMenuProperty = new SimpleListProperty<>();
		selectedMenuProperty.bind(new SimpleObjectProperty<>(selectedMenu));
		showMenuListView.getSelectionModel().selectedItemProperty().addListener(listener -> {
			if(selectedMenu.get(0).equals(ALL_SONGS_MODE))
				ml.showMode(ALL_SONGS_MODE);
			showHideTableInfoPaneMI.setDisable(true);
			sc.getRootController().showTableInfoPane(false);
			playlistTreeView.getSelectionModel().clearAndSelect(-1);
		});
		
		// Playlists pane
		playlistTreeView.setPrefWidth(200);
		playlistTreeView.getSelectionModel().selectedItemProperty().addListener(listener -> {
			showMenuListView.getSelectionModel().clearAndSelect(-1);
			sc.getRootController().showTableInfoPane(true);
			showHideTableInfoPaneMI.setDisable(false);
			showHideTableInfoPaneMI.setText(showHideTableInfoPaneMI.getText().replaceFirst("Show", "Hide"));
			if(playlistTreeView.getSelectedPlaylist() != null) {
				playlistTitleTextField.setText(playlistTreeView.getSelectedPlaylist().getName());
				setPlaylistTitleEdit(false);
				playlistCover.imageProperty().bind(playlistTreeView.getSelectedPlaylist().playlistCoverProperty());
			}
		});
		
		VBox playlistVBox = new VBox();
		Label playlistsLabel = new Label("Playlists");
		playlistsLabel.setTextFill(Paint.valueOf("rgb(158, 158, 158)"));
		Font labelFont = new Font("System", 10);
		playlistsLabel.setFont(labelFont);
		playlistVBox.getChildren().addAll(playlistsLabel, playlistTreeView);
		VBox.setMargin(playlistsLabel, new Insets(10, 0, 5, 15));
		
		VBox showMenuVBox = new VBox();
		showMenuVBox.setAlignment(Pos.TOP_LEFT);
		Label showMenuLabel = new Label("Musicott library");
		showMenuLabel.setTextFill(Paint.valueOf("rgb(158, 158, 158)"));
		showMenuLabel.setFont(labelFont);
		showMenuVBox.getChildren().addAll(showMenuLabel, showMenuListView);
		VBox.setMargin(showMenuLabel, new Insets(5, 0, 5, 15));
		
		navigationPaneVBox.getChildren().add(0, playlistVBox);
		navigationPaneVBox.getChildren().add(0, showMenuVBox);
		VBox.setVgrow(playlistTreeView, Priority.ALWAYS);
		VBox.setVgrow(showMenuVBox, Priority.ALWAYS);
		
		buildMenu();
		buildContextMenu();
		buildPlaylistContextMenu();
		showMenuListView.getSelectionModel().clearAndSelect(0);
	}

	/**
	 * Creates the play queue pane and configures it
	 */
	public void buildPlayQueuePane(Node playQueuePane) {
		// Set the play queue layout on the rootlayout
		playQueuePane.setVisible(false);
		rootLayout.getChildren().add(playQueuePane);
		LOG.debug("Play queue layout loaded");
		
		ToggleButton playQueueButton = (ToggleButton) rootLayout.lookup("#playQueueButton");
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
		rootLayout.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) {playQueuePane.setVisible(false); playQueueButton.setSelected(false);}});
		trackTable.setOnMouseClicked(event -> {if(playQueuePane.isVisible()) {playQueuePane.setVisible(false); playQueueButton.setSelected(false);}});
		LOG.debug("Configured play queue pane to close if the user click outside it");
	}
	
	/**
	 * Builds a native os x menubar or a default one
	 */
	private void buildMenu() {
		String os = System.getProperty ("os.name");
		Modifier keyModifierOS = os != null && os.startsWith ("Mac") ? KeyCodeCombination.META_DOWN : KeyCodeCombination.CONTROL_DOWN;
		
		MenuBar menuBar = new MenuBar();

		fileMN = new Menu("File"); editMN = new Menu("Menu"); controlsMN = new Menu("Controls"); viewMN = new Menu("View"); aboutMN = new Menu("About");
		openFileMI = new MenuItem("Open File(s)..."); importFolderMI = new MenuItem("Import Folder..."); importItunesMI = new MenuItem("Import from iTunes Library...");
		preferencesMI = new MenuItem("Preferences"); editMI = new MenuItem("Edit"); deleteMI = new MenuItem("Delete"); prevMI = new MenuItem("Previous");
		nextMI = new MenuItem("Next"); volIncrMI = new MenuItem("Increase volume"); volDecrMI = new MenuItem("Decrease volume");
		selCurrMI = new MenuItem("Select Current Track"); aboutMI = new MenuItem("About"); newPlaylistMI = new MenuItem("Add new playlist");
		showHideNavigationPaneMI = new MenuItem("Hide navigation pane"); showHideTableInfoPaneMI = new MenuItem("Hide table info pane");
		
		fileMN.getItems().addAll(openFileMI, importFolderMI, importItunesMI);
		editMN.getItems().addAll(editMI, deleteMI, new SeparatorMenuItem(), newPlaylistMI);
		controlsMN.getItems().addAll(prevMI, nextMI, new SeparatorMenuItem(), volIncrMI, volDecrMI, new SeparatorMenuItem(), selCurrMI);
		viewMN.getItems().addAll(showHideNavigationPaneMI, showHideTableInfoPaneMI);
		aboutMN.getItems().add(aboutMI);		

		// Key acceleratos. Command down for os x and control down for windows and linux
		openFileMI.setAccelerator(new KeyCodeCombination(KeyCode.O, keyModifierOS));
		importFolderMI.setAccelerator(new KeyCodeCombination(KeyCode.O, keyModifierOS, KeyCodeCombination.SHIFT_DOWN));
		importItunesMI.setAccelerator(new KeyCodeCombination(KeyCode.I, keyModifierOS, KeyCodeCombination.SHIFT_DOWN));
		preferencesMI.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, keyModifierOS));
		editMI.setAccelerator(new KeyCodeCombination(KeyCode.I, keyModifierOS));
		deleteMI.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, keyModifierOS));
		prevMI.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, keyModifierOS));
		nextMI.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, keyModifierOS));
		volIncrMI.setAccelerator(new KeyCodeCombination(KeyCode.UP, keyModifierOS));
		volDecrMI.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, keyModifierOS));
		selCurrMI.setAccelerator(new KeyCodeCombination(KeyCode.L, keyModifierOS));
		newPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, keyModifierOS));
		showHideNavigationPaneMI.setAccelerator(new KeyCodeCombination(KeyCode.R, keyModifierOS, KeyCodeCombination.SHIFT_DOWN));
		showHideTableInfoPaneMI.setAccelerator(new KeyCodeCombination(KeyCode.U, keyModifierOS, KeyCodeCombination.SHIFT_DOWN));
		
		// actions and bindings for the menus
		openFileMI.setOnAction(e -> {
			LOG.debug("Selecting files to open");
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Open file(s)...");
			chooser.getExtensionFilters().addAll(
					new ExtensionFilter("All Supported (*.mp3, *.flac, *.wav, *.m4a)","*.mp3", "*.flac", "*.wav", "*.m4a"),
					new ExtensionFilter("mp3 files (*.mp3)", "*.mp3"),
					new ExtensionFilter("flac files (*.flac)","*.flac"),
					new ExtensionFilter("wav files (*.wav)", "*.wav"),
					new ExtensionFilter("m4a files (*.wav)", "*.m4a"));
			List<File> files = chooser.showOpenMultipleDialog(rootStage);
			if(files != null) {
				TaskPoolManager.getInstance().parseFiles(files, true);
				sc.getRootController().setStatusMessage("Opening files");
			}
		});
		importFolderMI.setOnAction(e -> {
			LOG.debug("Choosing folder to being imported");
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Choose folder");
			File folder = chooser.showDialog(rootStage);
			if(folder != null) {
				Thread countFilesThread = new Thread(() -> {
					List<File> files = Utils.getAllFilesInFolder(folder, MainPreferences.getInstance().getExtensionsFileFilter(), 0);
					Platform.runLater(() -> {
						if(files.isEmpty()) {
							Alert alert = createAlert("Import", "No files", "There are no valid files to import on the selected folder."
									+ "Change the folder or the import options in preferences", AlertType.WARNING);
							alert.showAndWait();
						}
						else {
							Alert alert = createAlert("Import", "Import " + files.size() + " files?", "", AlertType.CONFIRMATION);
							Optional<ButtonType> result = alert.showAndWait();
							if(result.isPresent() && result.get().equals(ButtonType.OK)) {
								TaskPoolManager.getInstance().parseFiles(files, false);
								sc.getRootController().setStatusMessage("Importing files");
							}
						}
					});
				});
				countFilesThread.start();
			}
		});
		importItunesMI.setOnAction(e -> {
			LOG.debug("Choosing Itunes xml file");
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Select 'iTunes Music Library.xml' file");
			chooser.getExtensionFilters().add(new ExtensionFilter("xml files (*.xml)", "*.xml"));
			File xmlFile = chooser.showOpenDialog(rootStage);
			if(xmlFile != null) 
				TaskPoolManager.getInstance().parseItunesLibrary(xmlFile.getAbsolutePath());
		});
		preferencesMI.setOnAction(e -> sc.openPreferencesScene());
		editMI.setOnAction(e -> doEdit());
		deleteMI.setOnAction(e -> doDelete());
		Button prevButton = (Button) rootLayout.lookup("#prevButton");
		prevMI.disableProperty().bind(prevButton.disableProperty());
		prevMI.setOnAction(e -> PlayerFacade.getInstance().previous());
		Button nextButton = (Button) rootLayout.lookup("#nextButton");
		nextMI.disableProperty().bind(nextButton.disableProperty());
		nextMI.setOnAction(e -> PlayerFacade.getInstance().next());
		volIncrMI.setOnAction(e -> sc.getRootController().doIncreaseVolume());
		volDecrMI.setOnAction(e -> sc.getRootController().doDecreaseVolume());
		selCurrMI.setOnAction(e -> {
			Track currentTrack = PlayerFacade.getInstance().getCurrentTrack();
			trackTable.getSelectionModel().clearSelection();
			if(currentTrack != null) {
				Map.Entry<Integer, Track> currentEntry = new AbstractMap.SimpleEntry<Integer, Track>(currentTrack.getTrackID(), currentTrack);
				trackTable.getSelectionModel().select(currentEntry);
				trackTable.scrollTo(currentEntry);
			}
			LOG.debug("Current track in the player selected in the table");
		});
		aboutMI.setOnAction(e -> {
			Alert alert = createAlert("About Musicott", "Musicott", "", AlertType.INFORMATION);
			Label text = new Label(" Version 0.8.0\n\n Copyright © 2015 Octavio Calleya.");
			Label text2 = new Label(" Licensed under GNU GPLv3. This product includes\n software developed by other open source projects.");
			Hyperlink githubLink = new Hyperlink("https://github.com/octaviospain/Musicott/");
			githubLink.setOnAction(event -> hostServices.showDocument(githubLink.getText()));
			FlowPane fp = new FlowPane();
			fp.getChildren().addAll(text, githubLink, text2);
			alert.getDialogPane().contentProperty().set(fp);
			ImageView iv = new ImageView();
			iv.setImage(new Image("file:resources/images/musicotticon.png"));
			alert.setGraphic(iv);
			alert.showAndWait();
			LOG.debug("Showing about window");
		});
		newPlaylistMI.setOnAction(e -> doAddNewPlaylist());
		showHideNavigationPaneMI.setOnAction(e -> {
			if(showHideNavigationPaneMI.getText().startsWith("Show")) {
				sc.getRootController().showNavigationPane(true);
				showHideNavigationPaneMI.setText(showHideNavigationPaneMI.getText().replaceFirst("Show", "Hide"));
			} else if(showHideNavigationPaneMI.getText().startsWith("Hide")) {
				sc.getRootController().showNavigationPane(false);
				showHideNavigationPaneMI.setText(showHideNavigationPaneMI.getText().replaceFirst("Hide", "Show"));				
			}
		});
		showHideTableInfoPaneMI.setOnAction(e -> {
			if(showHideTableInfoPaneMI.getText().startsWith("Show")) {
				sc.getRootController().showTableInfoPane(true);
				showHideTableInfoPaneMI.setText(showHideTableInfoPaneMI.getText().replaceFirst("Show", "Hide"));
			} else if(showHideTableInfoPaneMI.getText().startsWith("Hide")) {
				sc.getRootController().showTableInfoPane(false);
				showHideTableInfoPaneMI.setText(showHideTableInfoPaneMI.getText().replaceFirst("Hide", "Show"));				
			}
		});
		
		if (os != null && os.startsWith ("Mac")) {
			MenuToolkit tk = MenuToolkit.toolkit();
			
			Menu appMenu = new Menu("Musicott");
			appMenu.getItems().addAll(preferencesMI, new SeparatorMenuItem(), tk.createQuitMenuItem("Musicott"));				
			Menu windowMenu = new Menu("Window");
			windowMenu.getItems().addAll(tk.createMinimizeMenuItem(), tk.createZoomMenuItem(),
			new SeparatorMenuItem(), tk.createBringAllToFrontItem());
			
			tk.setApplicationMenu(appMenu);
			menuBar.getMenus().addAll(appMenu, fileMN, editMN, controlsMN, viewMN, windowMenu, aboutMN);
			tk.autoAddWindowMenuItems(windowMenu);
			tk.setGlobalMenuBar(menuBar);
			LOG.debug("OS X native menubar created");
		}
		else {	// Default MenuBar
			MenuItem closeMI = new MenuItem("Close");
			closeMI.setAccelerator(new KeyCodeCombination(KeyCode.F4, KeyCodeCombination.ALT_DOWN));
			closeMI.setOnAction(event -> {LOG.info("Exiting Musicott"); System.exit(0);});
			fileMN.getItems().addAll(new SeparatorMenuItem(), preferencesMI, new SeparatorMenuItem(), closeMI);
			
			menuBar.getMenus().addAll(fileMN, editMN, controlsMN, viewMN, aboutMN);
			VBox headerVBox = (VBox) rootLayout.lookup("#headerVBox");
			headerVBox.getChildren().add(0, menuBar);
			LOG.debug("Default menubar created");
		}
	}
	
	/**
	 * Builds the content menu to be shown on the table
	 */
	private void buildContextMenu() {
		MenuItem cmPlay = new MenuItem("Play");
		cmPlay.setOnAction(event -> {
			if(!trackSelection.isEmpty())
				PlayerFacade.getInstance().addTracks(trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()), true);
		});
		MenuItem cmEdit = new MenuItem("Edit");
		cmEdit.setOnAction(event -> doEdit());
		MenuItem cmDelete = new MenuItem("Delete");
		cmDelete.setOnAction(event -> doDelete());
		MenuItem cmAddToQueue = new MenuItem("Add to Play Queue");
		cmAddToQueue.setOnAction(event -> {
			if(!trackSelection.isEmpty())
				PlayerFacade.getInstance().addTracks(trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()), false);
		});
		MenuItem cmDeleteFromPlaylist = new MenuItem("Delete from playlist");
		cmDeleteFromPlaylist.disableProperty().bind(selectedMenuProperty.emptyProperty().not());
		cmDeleteFromPlaylist.setOnAction(event -> {
			if(!trackSelection.isEmpty())
				ml.removeFromPlaylist(playlistTreeView.getSelectedPlaylist(), trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
		});
		Menu cmAddToPlaylist = new Menu("Add to Playlist");
		
		ContextMenu cm = new ContextMenu();
		cm.getItems().addAll(cmPlay, cmEdit, cmDelete, cmAddToQueue, new SeparatorMenuItem(), cmDeleteFromPlaylist, cmAddToPlaylist);
		
		// Right click on row = show ContextMenu
		trackTable.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			if(event.getButton() == MouseButton.SECONDARY) {
				// Updates the playlists everytime
				List<MenuItem> playlistsMI = new ArrayList<>();
				for(Playlist pl: ml.getPlaylists()) {
					String playListName = pl.getName();
					MenuItem playlistItem = new MenuItem(playListName);
					playlistItem.setOnAction(e -> {
						if(!trackSelection.isEmpty())
							ml.addToPlaylist(playListName, trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
					});
					playlistsMI.add(playlistItem);
				}
				cmAddToPlaylist.getItems().clear();
				cmAddToPlaylist.getItems().addAll(playlistsMI);
				cm.show(trackTable,event.getScreenX(),event.getScreenY());
				LOG.debug("Showing context menu");
			}
			else if(event.getButton() == MouseButton.PRIMARY && cm.isShowing())
				cm.hide();
		});
	}	

	/**
	 * Builds the context menu to be shown on the playlist pane
	 * to add or delete playlists
	 */
	private void buildPlaylistContextMenu() {
		ContextMenu cm = new ContextMenu();
		MenuItem addPlaylist = new MenuItem("Add new playlist");
		MenuItem deletePlaylist = new MenuItem("Delete playlist");
		addPlaylist.setOnAction(e -> doAddNewPlaylist());
		deletePlaylist.setOnAction(e -> {
			ml.removePlaylist(playlistTreeView.getSelectedPlaylist());
			Playlist selected = playlistTreeView.getSelectedPlaylist();
			if(selected != null) {
				int playlistToDeleteIndex = playlistTreeView.getSelectionModel().getSelectedIndex();
				if(playlistToDeleteIndex == 0 && playlistTreeView.getRoot().getChildren().size() == 1) {
					SceneManager.getInstance().getRootController().showTableInfoPane(false);
					ml.showMode(ALL_SONGS_MODE);
				}
				else {
					playlistTreeView.getSelectionModel().selectFirst();
				}
				playlistTreeView.getRoot().getChildren().removeIf(treeItem -> treeItem.getValue().equals(selected));
			}
		});
		cm.getItems().addAll(addPlaylist, deletePlaylist);
		playlistTreeView.setContextMenu(cm);
	}
	
	/**
	 * Auxiliary function to create an Alert
	 * 
	 */
	private Alert createAlert(String title, String header, String content, AlertType type) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.initModality(Modality.APPLICATION_MODAL);
		alert.initOwner(rootStage);
		return alert;
	}
	
	/**
	 * Handles the creation of a new playlist
	 */
	private void doAddNewPlaylist() {
		ml.clearShowingTracks();
		Playlist newPlaylist = new Playlist("");
		playlistTitleTextField.setText("");
		setPlaylistTitleEdit(true);
		playlistCover.imageProperty().bind(newPlaylist.playlistCoverProperty());
		playlistTitleTextField.setOnKeyPressed(event -> {
			String newTitle = playlistTitleTextField.getText();
			if(event.getCode() == KeyCode.ENTER && !newTitle.equals("") && !ml.containsPlaylist(newTitle)) {
				newPlaylist.setName(newTitle);
				ml.addPlaylist(newPlaylist);
				TreeItem<Playlist> playListItem = new TreeItem<>(newPlaylist);
				playlistTreeView.addPlaylistItem(playListItem);
				setPlaylistTitleEdit(false);
				event.consume();
			}
		});
	}
	
	/**
	 * Puts a text field to edit the name of the playlist if <tt>editPlaylistTitle</tt> is true,
	 * otherwise shows the label with the title of the selected or entered playlist
	 * 
	 * @param editPlaylistTitle
	 */
	private void setPlaylistTitleEdit(boolean editPlaylistTitle) {
		sc.getRootController().showTableInfoPane(true);
		if(editPlaylistTitle && !playlistInfoVBox.getChildren().contains(playlistTitleTextField)) {
			playlistInfoVBox.getChildren().remove(playlistTitleLabel);
			playlistInfoVBox.getChildren().add(0, playlistTitleTextField);
			playlistTitleTextField.requestFocus();
		} else if(!editPlaylistTitle && !playlistInfoVBox.getChildren().contains(playlistTitleLabel)) {
			playlistInfoVBox.getChildren().remove(playlistTitleTextField);
			playlistInfoVBox.getChildren().add(0, playlistTitleLabel);			
		}
	}
	
	/**
	 * Handles edit action
	 */
	private void doEdit() {
		if(trackSelection != null & !trackSelection.isEmpty()) {
			if(trackSelection.size() > 1) {
				Alert alert = createAlert("", "Are you sure you want to edit multiple files?", "", AlertType.CONFIRMATION);
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK) {
					sc.openEditScene(trackSelection.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
					LOG.debug("Opened edit stage for various tracks");
				}
				else
					alert.close();
			}
			else {
				sc.openEditScene(trackSelection.stream().map(Map.Entry::getValue).collect(Collectors.toList()));
				LOG.debug("Opened edit stage for a single track");
			}
		}
	}
	
	/**
	 * Handles delete action
	 */
	private void doDelete() {
		if(trackSelection != null && !trackSelection.isEmpty()) {
			int numDeletedTracks = trackSelection.size();
			Alert alert = createAlert("", "Delete "+numDeletedTracks+" files from Musicott?", "", AlertType.CONFIRMATION);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				new Thread(() -> {
					ml.removeTracks(trackSelection.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
					Platform.runLater(() -> sc.closeIndeterminatedProgressScene());
				}).start();
				sc.openIndeterminatedProgressScene();
			}
			else
				alert.close();
		}
	}
}