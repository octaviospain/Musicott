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

import com.transgressoft.musicott.util.*;
import javafx.application.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;

import static com.transgressoft.musicott.MusicottApplication.*;
import static com.transgressoft.musicott.view.MusicottController.*;

/**
 * Preloader of the application. Shows the progress of the tasks of loading the tracks, the playlists, and the
 * waveforms. <p> If it is the first use of the application a prompt dialog asks the user to enter the location of the
 * application folder. </p>
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class MainPreloader extends Preloader {

	private static final int SCENE_WIDTH = 450;
	private static final int SCENE_HEIGHT = 120;

	private MainPreferences preferences = MainPreferences.getInstance();
	private Stage preloaderStage;
	private Label infoLabel;
	private ProgressBar preloaderProgressBar;

	@Override
	public void start(Stage primaryStage) throws Exception {
		AnchorPane rootAnchorPane = FXMLLoader.load(getClass().getResource(PRELOADER_INIT_LAYOUT));
		infoLabel = (Label) rootAnchorPane.lookup("#infoLabel");
		preloaderProgressBar = (ProgressBar) rootAnchorPane.lookup("#preloaderProgressBar");
		preloaderStage = primaryStage;
		preloaderStage.setOnCloseRequest(Event::consume);
		preloaderStage.setTitle("Musicott");
		preloaderStage.getIcons().add(new Image(getClass().getResourceAsStream(MUSICOTT_APP_ICON)));
		preloaderStage.setScene(new Scene(rootAnchorPane));
		preloaderStage.setResizable(false);
		preloaderStage.initStyle(StageStyle.UNDECORATED);
		preloaderStage.show();
	}

	@Override
	public void handleStateChangeNotification(StateChangeNotification info) {
		// Handle state change notifications.
		StateChangeNotification.Type type = info.getType();
		switch (type) {
			case BEFORE_START:
				// Called after MusicottApplication#init and before MusicottApplication#start is called.
				preloaderStage.close();
				break;
			default:
				break;
		}
	}

	@Override
	public void handleApplicationNotification(PreloaderNotification info) {
		CustomProgressNotification progressNotification = (CustomProgressNotification) info;
		if (progressNotification.getDetails().equals(FIRST_USE_EVENT))
			openFirstUseDialog();
		else {
			preloaderProgressBar.setProgress(progressNotification.getProgress());
			infoLabel.setText(progressNotification.getDetails());
		}
	}

	/**
	 * Shows a window asking the user to enter the location of the application folder. The default location is
	 * <tt>~/Music/Musicott</tt>
	 */
	private void openFirstUseDialog() {
		try {
			Stage promptStage = new Stage();
			AnchorPane preloaderPane = FXMLLoader.load(getClass().getResource(PRELOADER_FIRST_USE_PROMPT));
			Button openButton = (Button) preloaderPane.lookup("#openButton");
			Button okButton = (Button) preloaderPane.lookup("#okButton");
			TextField musicottFolderTextField = (TextField) preloaderPane.lookup("#musicottFolderTextField");

			String sep = File.separator;
			String userHome = System.getProperty("user.home");
			String defaultMusicottLocation = userHome + sep + "Music" + sep + "Musicott";
			musicottFolderTextField.setText(defaultMusicottLocation);

			okButton.setOnMouseClicked(event -> {
				preferences.setMusicottUserFolder(musicottFolderTextField.getText());
				promptStage.close();
			});
			openButton.setOnMouseClicked(event -> {
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setInitialDirectory(new File(userHome));
				chooser.setTitle("Choose Musicott folder");
				File folder = chooser.showDialog(promptStage);
				if (folder != null) {
					musicottFolderTextField.setText(folder.toString());
				}
			});

			Scene promptScene = new Scene(preloaderPane, SCENE_WIDTH, SCENE_HEIGHT);
			promptStage.setOnCloseRequest(event -> preferences.setMusicottUserFolder(defaultMusicottLocation));
			promptStage.initModality(Modality.APPLICATION_MODAL);
			promptStage.initOwner(preloaderStage.getOwner());
			promptStage.setResizable(false);
			promptStage.setScene(promptScene);
			promptStage.initStyle(StageStyle.UNDECORATED);
			promptStage.showAndWait();
		}
		catch (IOException e) {
			ErrorDemon.getInstance().showErrorDialog("Error opening Musicott's folder selection", "", e);
		}
	}
}
