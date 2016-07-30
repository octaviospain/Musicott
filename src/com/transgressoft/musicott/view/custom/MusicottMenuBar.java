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

package com.transgressoft.musicott.view.custom;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.Utils.*;
import com.transgressoft.musicott.view.*;
import de.codecentric.centerdevice.*;
import javafx.application.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.input.KeyCombination.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.stage.FileChooser.*;
import org.slf4j.*;

import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.MusicottController.*;
import static javafx.scene.input.KeyCodeCombination.ALT_DOWN;

/**
 * Creates a MenuBar or a native OS X menu bar
 *
 * @author Octavio Calleya
 * @version 0.9-b
 */
public class MusicottMenuBar extends MenuBar {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

	private static final String MUSICOTT_GITHUB_LINK = "https://github.com/octaviospain/Musicott/";
	private static final String ABOUT_MUSICOTT_FIRST_LINE = " Version 0.8.0\n\n Copyright © 2015 Octavio Calleya.";
	private static final String ABOUT_MUSICOTT_SECOND_LINE = " Licensed under GNU GPLv3. This product includes\n" + " " +
																"software developed by other open source projects.";

	private StageDemon stageDemon = StageDemon.getInstance();
	private PlayerFacade playerFacade = PlayerFacade.getInstance();
	private RootController rootController = stageDemon.getRootController();
	private NavigationController navigationController = stageDemon.getNavigationController();

	private Stage primaryStage;

	private Menu fileMenu;
	private Menu editMenu;
	private Menu controlsMenu;
	private Menu viewMenu;
	private Menu aboutMenu;
	private MenuItem openFileMenuItem;
	private MenuItem importFolderMenuItem;
	private MenuItem importItunesMenuItem;
	private MenuItem preferencesMenuItem;
	private MenuItem editMenuItem;
	private MenuItem deleteMenuItem;
	private MenuItem previousMenuItem;
	private MenuItem nextMenuItem;
	private MenuItem increaseVolumeMenuItem;
	private MenuItem decreaseVolumeMenuItem;
	private MenuItem selectCurrentTrackMenuItem;
	private MenuItem aboutMenuItem;
	private MenuItem newPlaylistMenuItem;
	private MenuItem showHideNavigationPaneMenuItem;
	private MenuItem showHideTableInfoPaneMenuItem;
	private Image musicottIcon = new Image(getClass().getResourceAsStream(MUSICOTT_ICON));
	private ImageView musicottImageView = new ImageView(musicottIcon);

	public MusicottMenuBar(Stage primaryStage) {
		super();
		this.primaryStage = primaryStage;
		initializeMenus();
		setFileMenuActions();
		setEditMenuActions();
		setControlsMenuActions();
		setViewMenuActions();
		setAboutMenuActions();
		showHideTableInfoDisableBinding();
		showHideNavigationPaneTextBinding();
		showHideTableInfoPaneTextBinding();
	}

	/**
	 * Configures the {@link MenuBar} as a native Mac OS X one
	 *
	 * @see <a href="https://github.com/codecentric/NSMenuFX">NSMenuFX</a>
	 */
	public void macMenuBar() {
		MenuToolkit menuToolkit = MenuToolkit.toolkit();
		Menu appMenu = new Menu("Musicott");
		appMenu.getItems().addAll(preferencesMenuItem, new SeparatorMenuItem());
		appMenu.getItems().add(menuToolkit.createQuitMenuItem("Musicott"));
		Menu windowMenu = new Menu("Window");
		windowMenu.getItems().addAll(menuToolkit.createMinimizeMenuItem(), menuToolkit.createCloseWindowMenuItem());
		windowMenu.getItems().addAll(menuToolkit.createZoomMenuItem(), new SeparatorMenuItem());
		windowMenu.getItems().addAll(menuToolkit.createHideOthersMenuItem(), menuToolkit.createUnhideAllMenuItem());
		windowMenu.getItems().addAll(menuToolkit.createBringAllToFrontItem());

		menuToolkit.setApplicationMenu(appMenu);
		getMenus().addAll(appMenu, fileMenu, editMenu, controlsMenu, viewMenu, windowMenu, aboutMenu);
		menuToolkit.autoAddWindowMenuItems(windowMenu);
		menuToolkit.setGlobalMenuBar(this);

		setAccelerators(KeyCodeCombination.META_DOWN);
		LOG.debug("OS X native menubar created");
	}

	/**
	 * Configures the {@link MenuBar} bar with default accelerators and menus.
	 */
	public void defaultMenuBar() {
		MenuItem closeMI = new MenuItem("Close");
		closeMI.setAccelerator(new KeyCodeCombination(KeyCode.F4, ALT_DOWN));
		closeMI.setOnAction(event -> {
			LOG.info("Exiting Musicott");
			System.exit(0);
		});
		fileMenu.getItems().addAll(new SeparatorMenuItem(), preferencesMenuItem, new SeparatorMenuItem(), closeMI);
		getMenus().addAll(fileMenu, editMenu, controlsMenu, viewMenu, aboutMenu);
		setAccelerators(KeyCodeCombination.CONTROL_DOWN);
		LOG.debug("Default menu bar created");
	}

	private void initializeMenus() {
		fileMenu = new Menu("File");
		editMenu = new Menu("Menu");
		controlsMenu = new Menu("Controls");
		viewMenu = new Menu("View");
		aboutMenu = new Menu("About");
		openFileMenuItem = new MenuItem("Open File(s)...");
		importFolderMenuItem = new MenuItem("Import Folder...");
		importItunesMenuItem = new MenuItem("Import from iTunes Library...");
		preferencesMenuItem = new MenuItem("Preferences");
		editMenuItem = new MenuItem("Edit");
		deleteMenuItem = new MenuItem("Delete");
		previousMenuItem = new MenuItem("Previous");
		nextMenuItem = new MenuItem("Next");
		increaseVolumeMenuItem = new MenuItem("Increase volume");
		decreaseVolumeMenuItem = new MenuItem("Decrease volume");
		selectCurrentTrackMenuItem = new MenuItem("Select Current Track");
		aboutMenuItem = new MenuItem("About");
		newPlaylistMenuItem = new MenuItem("Add new playlist");
		showHideNavigationPaneMenuItem = new MenuItem("Hide navigation pane");
		showHideTableInfoPaneMenuItem = new MenuItem("Hide table info pane");

		fileMenu.getItems().addAll(openFileMenuItem, importFolderMenuItem, importItunesMenuItem);
		editMenu.getItems().addAll(editMenuItem, deleteMenuItem, new SeparatorMenuItem(), newPlaylistMenuItem);
		controlsMenu.getItems().addAll(previousMenuItem, nextMenuItem, new SeparatorMenuItem());
		controlsMenu.getItems().addAll(increaseVolumeMenuItem, decreaseVolumeMenuItem, new SeparatorMenuItem());
		controlsMenu.getItems().add(selectCurrentTrackMenuItem);
		viewMenu.getItems().addAll(showHideNavigationPaneMenuItem, showHideTableInfoPaneMenuItem);
		aboutMenu.getItems().add(aboutMenuItem);
	}

	private void setAccelerators(Modifier operativeSystemModifier) {
		Modifier shiftDown = KeyCodeCombination.SHIFT_DOWN;
		openFileMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, operativeSystemModifier));
		importFolderMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.O, operativeSystemModifier, shiftDown));
		importItunesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, operativeSystemModifier, shiftDown));
		preferencesMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, operativeSystemModifier));
		editMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.I, operativeSystemModifier));
		deleteMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.BACK_SPACE, operativeSystemModifier));
		previousMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.RIGHT, operativeSystemModifier));
		nextMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.LEFT, operativeSystemModifier));
		increaseVolumeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.UP, operativeSystemModifier));
		decreaseVolumeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.DOWN, operativeSystemModifier));
		selectCurrentTrackMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.L, operativeSystemModifier));
		newPlaylistMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.N, operativeSystemModifier));
		showHideNavigationPaneMenuItem
				.setAccelerator(new KeyCodeCombination(KeyCode.R, operativeSystemModifier, shiftDown));
		showHideTableInfoPaneMenuItem
				.setAccelerator(new KeyCodeCombination(KeyCode.U, operativeSystemModifier, shiftDown));
	}

	private void setFileMenuActions() {
		openFileMenuItem.setOnAction(e -> {
			LOG.debug("Selecting files to open");
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Open file(s)...");
			chooser.getExtensionFilters()
				   .addAll(new ExtensionFilter("All Supported (*.mp3, *.flac, *.wav, *.m4a)", "*.mp3", "*.flac",
											   "*.wav", "*.m4a"), new ExtensionFilter("mp3 files (*.mp3)", "*.mp3"),
						   new ExtensionFilter("flac files (*.flac)", "*.flac"),
						   new ExtensionFilter("wav files (*.wav)", "*.wav"),
						   new ExtensionFilter("m4a files (*.wav)", "*.m4a"));
			List<File> files = chooser.showOpenMultipleDialog(primaryStage);
			if (files != null) {
				TaskDemon.getInstance().importFiles(files, true);
				navigationController.setStatusMessage("Opening files");
			}
		});
		importFolderMenuItem.setOnAction(e -> {
			LOG.debug("Choosing folder to being imported");
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Choose folder");
			File folder = chooser.showDialog(primaryStage);
			if (folder != null)
				countFilesToImport(folder);
		});
		importItunesMenuItem.setOnAction(e -> {
			LOG.debug("Choosing Itunes xml file");
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Select 'iTunes Music Library.xml' file");
			chooser.getExtensionFilters().add(new ExtensionFilter("xml files (*.xml)", "*.xml"));
			File xmlFile = chooser.showOpenDialog(primaryStage);
			if (xmlFile != null)
				TaskDemon.getInstance().importFromItunesLibrary(xmlFile.getAbsolutePath());
		});
		preferencesMenuItem.setOnAction(e -> stageDemon.showPreferences());
	}

	private void countFilesToImport(File folder) {
		Platform.runLater(() -> {
			navigationController.setStatusMessage("Scanning folders...");
			navigationController.setStatusProgress(- 1);
		});

		Thread countFilesThread = new Thread(() -> countFilesToImportTask(folder));
		countFilesThread.start();
	}

	private void countFilesToImportTask(File folder) {
		Set<String> extensions = MainPreferences.getInstance().getImportFilterExtensions();
		ExtensionFileFilter filter = new ExtensionFileFilter();
		extensions.stream().forEach(filter::addExtension);
		Platform.runLater(() -> {
			navigationController.setStatusMessage("");
			navigationController.setStatusProgress(0);
		});
		List<File> files = Utils.getAllFilesInFolder(folder, filter, 0);
		if (files.isEmpty())
			showNoFilesToImportAlert();
		else
			showImportConfirmationAlert(files);
	}

	private void showNoFilesToImportAlert() {
		String alertContentText = "There are no valid files to import in the selected folder. " + "Change the folder " +
				"or the import options in preferences";
		Platform.runLater(() -> {
			Alert alert = stageDemon.createAlert("Import", "No files", alertContentText, AlertType.WARNING);
			alert.showAndWait();
		});
	}

	private void showImportConfirmationAlert(List<File> filesToImport) {
		String alertContentText = "Import " + filesToImport.size() + " files?";
		Platform.runLater(() -> {
			Alert alert = stageDemon.createAlert("Import", alertContentText, "", AlertType.CONFIRMATION);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.isPresent() && result.get().equals(ButtonType.OK)) {
				TaskDemon.getInstance().importFiles(filesToImport, false);
				stageDemon.getNavigationController().setStatusMessage("Importing files");
			}
		});
	}

	private void setEditMenuActions() {
		editMenuItem.setOnAction(e -> stageDemon.editTracks());
		deleteMenuItem.setOnAction(e -> stageDemon.deleteTracks());
		newPlaylistMenuItem.setOnAction(e -> stageDemon.getRootController().enterNewPlaylistName(false));
	}

	private void setControlsMenuActions() {
		previousMenuItem.disableProperty().bind(stageDemon.getPlayerController().previousButtonDisabledProperty());
		previousMenuItem.setOnAction(e -> playerFacade.previous());

		nextMenuItem.disableProperty().bind(stageDemon.getPlayerController().nextButtonDisabledProperty());
		nextMenuItem.setOnAction(e -> playerFacade.next());

		increaseVolumeMenuItem.setOnAction(e -> stageDemon.getPlayerController().increaseVolume());
		decreaseVolumeMenuItem.setOnAction(e -> stageDemon.getPlayerController().decreaseVolume());

		selectCurrentTrackMenuItem.setOnAction(e -> {
			Optional<Track> currentTrack = playerFacade.getCurrentTrack();
			TrackTableView trackTable = (TrackTableView) primaryStage.getScene().lookup("#trackTable");
			trackTable.getSelectionModel().clearSelection();
			currentTrack.ifPresent(track -> {
				int currentTrackId = track.getTrackId();
				Map.Entry<Integer, Track> currentEntry = new AbstractMap.SimpleEntry<>(currentTrackId, track);
				trackTable.getSelectionModel().select(currentEntry);
				trackTable.scrollTo(currentEntry);
			});
			LOG.debug("Current track in the player selected in the table");
		});
	}

	private void setViewMenuActions() {
		showHideNavigationPaneMenuItem.setOnAction(e -> {
			if (showHideNavigationPaneMenuItem.getText().startsWith("Show"))
				rootController.showNavigationPane();
			else if (showHideNavigationPaneMenuItem.getText().startsWith("Hide"))
				rootController.hideNavigationPane();
		});
		showHideTableInfoPaneMenuItem.setOnAction(e -> {
			if (showHideTableInfoPaneMenuItem.getText().startsWith("Show"))
				rootController.showTableInfoPane();
			else if (showHideTableInfoPaneMenuItem.getText().startsWith("Hide"))
				rootController.hideTableInfoPane();
		});
	}

	private void setAboutMenuActions() {
		aboutMenuItem.setOnAction(e -> {
			Alert alert = stageDemon.createAlert("About Musicott", "Musicott", "", AlertType.INFORMATION);
			Label aboutLabel1 = new Label(ABOUT_MUSICOTT_FIRST_LINE);
			Label aboutLabel2 = new Label(ABOUT_MUSICOTT_SECOND_LINE);
			Hyperlink githubLink = new Hyperlink(MUSICOTT_GITHUB_LINK);
			githubLink.setOnAction(event -> stageDemon.getApplicationHostServices().showDocument(githubLink.getText()));
			FlowPane flowPane = new FlowPane();
			flowPane.getChildren().addAll(aboutLabel1, githubLink, aboutLabel2);
			alert.getDialogPane().contentProperty().set(flowPane);
			alert.setGraphic(musicottImageView);
			alert.showAndWait();
			LOG.debug("Showing about window");
		});
	}

	/**
	 * Binds the show/hide table info pane menu item
	 * to be disabled if a playlist is shown
	 */
	private void showHideTableInfoDisableBinding() {
		ReadOnlyObjectProperty<NavigationMode> selectedMenu = navigationController.navigationModeProperty();
		showHideTableInfoPaneMenuItem.disableProperty().bind(Bindings.createBooleanBinding(
				() -> selectedMenu.getValue().equals(NavigationMode.PLAYLIST), selectedMenu).not());
	}

	/**
	 * Binds the show/hide navigation pane menu item to change
	 * his text if the pane is showing or not
	 */
	private void showHideNavigationPaneTextBinding() {
		ReadOnlyBooleanProperty showingNavigationPaneProperty = rootController.showNavigationPaneProperty();
		showHideNavigationPaneMenuItem.textProperty().bind(Bindings.createStringBinding(() -> {
			String menuText = "";
			if (showingNavigationPaneProperty.get())
				menuText = "Hide navigation pane";
			else
				menuText = "Show navigation pane";
			return menuText;
		}, showingNavigationPaneProperty));
	}

	/**
	 * Binds the show/hide table info pane menu item to change
	 * his text if the pane is showing or not
	 */
	private void showHideTableInfoPaneTextBinding() {
		ReadOnlyBooleanProperty showingTableInfoPaneProperty = rootController.showTableInfoPaneProperty();
		showHideTableInfoPaneMenuItem.textProperty().bind(Bindings.createStringBinding(() -> {
			String menuText = "";
			if (showingTableInfoPaneProperty.get())
				menuText = "Hide table information pane";
			else
				menuText = "Show table information pane";
			return menuText;
		}, showingTableInfoPaneProperty));
	}
}
