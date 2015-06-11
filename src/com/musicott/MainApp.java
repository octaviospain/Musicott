package com.musicott;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {
	
	public static void main(String[] args) {
		launch();
	}
	
	@Override
	public void start(Stage primaryStage) {
		Stage mainStage = primaryStage;
		mainStage.setTitle("Musicott");
		mainStage.getIcons().add(new Image("file:resources/images/musicotticon.png"));		
		SceneManager sc = SceneManager.getInstance();
		sc.setPrimaryStage(mainStage);
	}
}