package com.musicott;

import java.io.IOException;

import com.musicott.task.ImportTask;
import com.musicott.view.ImportCollectionController;
import com.musicott.view.ProgressImportController;
import com.musicott.view.RootLayoutController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SceneManager {

	private Stage rootStage;
	private Stage importStage;
	private Stage progressStage;
	private Scene mainScene;

	private BorderPane rootLayout;
	private AnchorPane importLayout;
	private RootLayoutController rootController;
	private ImportCollectionController importController;
	private ProgressImportController progressImportController;
	
	private static SceneManager instance;
	
	private SceneManager() {
	}
	
	public static SceneManager getInstance() {
		if(instance==null)
			instance = new SceneManager();
		return instance;
	}
	
	protected void setPrimaryStage(Stage primaryStage) {
		rootStage = primaryStage;
		initPrimaryStage();
	}

	
	public RootLayoutController getRootController() {
		return rootController;
	}
	
	public ImportCollectionController getImportController() {
		return importController;
	}
	
	public ProgressImportController getProgressImportController() {
		return progressImportController;
	}
	
	public void openImportScene() {
		try {
			importStage = new Stage();
			importStage.setTitle("Import");
			
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/ImportCollectionMainView.fxml"));
			importLayout = (AnchorPane) loader.load();
			importController = loader.getController();
			importController.setStage(importStage);
			
			Scene importMainScene = new Scene(importLayout);
			importStage.initModality(Modality.APPLICATION_MODAL);
			importStage.initOwner(importMainScene.getWindow());
			importStage.setScene(importMainScene);
			importStage.setResizable(false);
			importStage.showAndWait();
		} catch (IOException e) {
			//TODO Show error dialog and closes import scene
			e.printStackTrace();
		}
	}
	
	public void closeImportScene() {
		importStage.close();
		progressStage.close();
	}
	
	public void showImportProgressScene(ImportTask	task) {
		try {
			progressStage = new Stage();
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/ProgressImportView.fxml"));
			AnchorPane progressLayout =  (AnchorPane) loader.load();
			progressImportController = loader.getController();
			progressImportController.setTask(task);
			progressImportController.runTask();
			
			Scene progressScene = new Scene(progressLayout);
			progressStage.initModality(Modality.APPLICATION_MODAL);
			progressStage.initOwner(progressScene.getWindow());
			progressStage.setScene(progressScene);
			progressStage.setResizable(false);
			progressStage.showAndWait();
		}
		catch(IOException e) {
			// TODO Show error dialog and closes import scene
			e.printStackTrace();
		}
	}
	
	private void initPrimaryStage() {
		try {
			FXMLLoader rootLoader = new FXMLLoader();
			rootLoader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
			rootLayout = (BorderPane) rootLoader.load();
			rootController = rootLoader.getController();
			rootController.setStage(rootStage);
			
			mainScene = new Scene(rootLayout);
			rootStage.setMinWidth(1200);
			rootStage.setMinHeight(770);
			rootStage.setScene(mainScene);
			rootStage.show();	
		} catch (IOException e) {
			//TODO Show error dialog and crashes
			e.printStackTrace();
		}
	}
}