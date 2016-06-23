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

import com.musicott.MainApp.*;
import javafx.application.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;

import static com.musicott.MainApp.*;
import static com.musicott.view.MusicottController.*;

/**
 * @author Octavio Calleya
 *
 */
public class MainPreloader extends Preloader {

	private MainPreferences preferences;
	private Stage preloaderStage;
	private Scene preloaderScene;
	private Image musicottIcon;
	private ImageView musicottImageView;
	private Label infoLabel;
	private ProgressBar preloaderProgressBar;
	
	public MainPreloader() {
		preferences = MainPreferences.getInstance();
	}
	
	@Override
    public void init() throws Exception {
        Platform.runLater(() -> {
        	musicottIcon = new Image("file:resources/images/musicotticon.png");
        	musicottImageView = new ImageView(musicottIcon);
        	infoLabel = new Label();
            preloaderProgressBar = new ProgressBar();
            preloaderProgressBar.setPrefSize(300, 20.0);
            preloaderProgressBar.setProgress(0.0);
            preloaderProgressBar.setStyle("-fx-accent: rgb(99,255,109);");
            VBox root = new VBox(musicottImageView, infoLabel, preloaderProgressBar);
            root.setAlignment(Pos.CENTER);
            VBox.setMargin(infoLabel, new Insets(10, 0, 10, 0));
            preloaderScene = new Scene(root, 350, 230);
        });
    }
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		// Set preloader scene and show stage.
        preloaderStage = primaryStage;
        preloaderStage.setOnCloseRequest(event -> event.consume());
        preloaderStage.setTitle("Musicott");
        preloaderStage.getIcons().add(musicottIcon);
        preloaderStage.setScene(preloaderScene);
        preloaderStage.setResizable(false);
        preloaderStage.show();
	}
	
	@Override
	public void handleApplicationNotification(PreloaderNotification info) {
		CustomProgressNotification pn = (CustomProgressNotification) info;
		if(pn.getDetails().equals(FIRST_USE_EVENT))
			openFirstUseDialog();
		else {
			preloaderProgressBar.setProgress(pn.getProgress());
			infoLabel.setText(pn.getDetails());
		}
	}
	
    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
        // Handle state change notifications.
        StateChangeNotification.Type type = info.getType();
        switch (type) {
            case BEFORE_LOAD:
                // Called after MainApp#start is called.
                break;
            case BEFORE_INIT:
                // Called before MainApp#init is called.
                break;
            case BEFORE_START:
                // Called after MainApp#init and before MainApp#start is called.
	            preloaderStage.close();
                break;
        }
    }
    
    private void openFirstUseDialog() {
    	try {
			Stage promptStage = new Stage();
        	FXMLLoader loader = new FXMLLoader ();
        	loader.setLocation(getClass().getResource(PRELOADER_LAYOUT));
			AnchorPane pane = (AnchorPane) loader.load();
			Button openButton = (Button) pane.lookup("#openButton");
			Button okButton = (Button) pane.lookup("#okButton");
			TextField musicottFolderTextField = (TextField) pane.lookup("#musicottFolderTextField");
			
			String defaultMusicottLocation = System.getProperty("user.home")+File.separator+"Music"+File.separator+"Musicott";
			musicottFolderTextField.setText(defaultMusicottLocation);
			okButton.setOnMouseClicked(event -> {
				preferences.setMusicottUserFolder(musicottFolderTextField.getText());
				promptStage.close();
			});
			openButton.setOnMouseClicked(event -> {
				DirectoryChooser chooser = new DirectoryChooser();
				chooser.setInitialDirectory(new File (System.getProperty("user.home")));
				chooser.setTitle("Choose Musicott folder");
				File folder = chooser.showDialog(promptStage);
				if(folder != null)
					musicottFolderTextField.setText(folder.toString());
			});
			
			Scene promptScene = new Scene(pane, 450, 120);
			promptStage.setOnCloseRequest(event -> preferences.setMusicottUserFolder(defaultMusicottLocation));
			promptStage.initModality(Modality.APPLICATION_MODAL);
			promptStage.initOwner(preloaderStage.getOwner());
			promptStage.setResizable(false);
			promptStage.setScene(promptScene);
			promptStage.showAndWait();
		} catch (IOException e) {
			ErrorDemon.getInstance().showErrorDialog("Error opening Musicott's folder selection", null, e);
		}
    }
}
