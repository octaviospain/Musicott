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

package com.musicott;

import java.util.Optional;

import com.musicott.MainApp.CustomProgressNotification;
import com.musicott.MainApp.EventNotification;

import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class MainPreloader extends Preloader {
	
	private MainPreferences pf;
	private Stage preloaderStage;
	private Scene preloaderScene;
	private Image musicottIcon;
	private ImageView musicottImageView;
	private Label infoLabel;
	private ProgressBar preloaderProgressBar;
	
	public MainPreloader() {
		pf = MainPreferences.getInstance();
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
		if(info instanceof CustomProgressNotification) {
			CustomProgressNotification pn = (CustomProgressNotification) info;
			preloaderProgressBar.setProgress(pn.getProgress());
			infoLabel.setText(pn.getDetails());
		}
		else if(info instanceof EventNotification) {
			openFirstUseDialog();
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
    	String defaultMusicottLocation = System.getProperty("user.home")+"/Music/Musicott";
    	TextInputDialog inputDialog = new TextInputDialog(defaultMusicottLocation);
    	DialogPane pane = inputDialog.getDialogPane();
    	pane.setMinWidth(500);
    	pane.setGraphic(null);
    	pane.getButtonTypes().remove(1);
    	pane.getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
    	inputDialog.setOnCloseRequest(event -> pf.setMusicottUserFolder(defaultMusicottLocation));
    	inputDialog.setResizable(false);
    	inputDialog.setTitle("");
    	inputDialog.setHeaderText("");
    	inputDialog.setContentText("Musicott folder");
    	Optional<String> result = inputDialog.showAndWait();
		if(result.isPresent())
			pf.setMusicottUserFolder(result.get());
    }
}