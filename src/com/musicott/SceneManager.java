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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.musicott;

import java.io.IOException;

import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.task.LoadLibraryTask;
import com.musicott.task.SaveLibraryTask;
import com.musicott.view.EditController;
import com.musicott.view.ImportCollectionController;
import com.musicott.view.PlayQueueController;
import com.musicott.view.ProgressImportController;
import com.musicott.view.RootLayoutController;
import com.musicott.model.Track;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class SceneManager {
	
	private Stage importStage;
	private Stage progressStage;
	private Stage editStage;
	private MainApp application;
	
	private BorderPane rootLayout;
	private AnchorPane playQueueLayout;

	private EditController editController;
	private RootLayoutController rootController;
	private ImportCollectionController importController;
	private ProgressImportController progressImportController;
	private PlayQueueController playQueueController;
	
	private static SceneManager instance;
	private ErrorHandler errorHandler;
	
	private SceneManager() {
	}
	
	public static SceneManager getInstance() {
		if(instance==null)
			instance = new SceneManager();
		return instance;
	}
	
	protected void setPrimaryStage(MainApp application) {
		this.application = application;
		errorHandler = ErrorHandler.getInstance();
		initPrimaryStage();
	}

	public void saveLibrary() {
		SaveLibraryTask task = new SaveLibraryTask();
		Thread t = new Thread(task, "SaveLibrary Thread");
		t.setDaemon(true);
		t.run();
	}
	
	public MainApp getApplication() {
		return application;
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
	
	public PlayQueueController getPlayQueueController() {
		return playQueueController;
	}
	
	public void showHidePlayQueue() {
		if(playQueueLayout.isVisible())
			playQueueLayout.setVisible(false);
		else
			playQueueLayout.setVisible(true);
	}
	
	public void openEditScene(ObservableList<Track> selection) {
		try {
			editStage = new Stage();
			editStage.setTitle("Edit");
			
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(SceneManager.class.getResource("view/EditInfoView.fxml"));
			AnchorPane editLayout = (AnchorPane) loader.load();
			editController = loader.getController();
			editController.setStage(editStage);
			editController.setSelection(selection);
			
			Scene editScene = new Scene(editLayout);
			editStage.initModality(Modality.APPLICATION_MODAL);
			editStage.initOwner(editScene.getWindow());
			editStage.setScene(editScene);
			editStage.setResizable(false);
			editStage.showAndWait();
		} catch (IOException e) {
			errorHandler.addError(e, ErrorType.COMMON);
			errorHandler.showErrorDialog(ErrorType.COMMON);
		}
	}
	
	public void openImportScene() {
		try {
			importStage = new Stage();
			importStage.setTitle("Import");
			
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/ImportCollectionView.fxml"));

			AnchorPane importLayout = (AnchorPane) loader.load();
			importController = loader.getController();
			importController.setStage(importStage);
			
			Scene importMainScene = new Scene(importLayout);
			importStage.initModality(Modality.APPLICATION_MODAL);
			importStage.initOwner(importMainScene.getWindow());
			importStage.setScene(importMainScene);
			importStage.setResizable(false);
			importStage.showAndWait();
		} catch (IOException e) {
			errorHandler.addError(e, ErrorType.COMMON);
			errorHandler.showErrorDialog(ErrorType.COMMON);
		}
	}
	
	public void closeImportScene() {
		if(errorHandler.hasErrors(ErrorType.PARSE)) 
			errorHandler.showErrorDialog(progressStage.getScene(), ErrorType.PARSE);
		progressStage.close();
		if(importStage != null && importStage.isShowing())
			importStage.close();
	}
	
	public void showImportProgressScene(Task<?> task, boolean hideCancelButton) {
		try {
			progressStage = new Stage();
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/ProgressImportView.fxml"));
			AnchorPane progressLayout =  (AnchorPane) loader.load();
			progressImportController = loader.getController();
			if(hideCancelButton)
				progressImportController.hideCancelButton();
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
			errorHandler.addError(e, ErrorType.COMMON);
			errorHandler.showErrorDialog(ErrorType.COMMON);
		}
	}
	
	private void initPrimaryStage() {
		try {
			Stage rootStage = application.getStage();
			FXMLLoader rootLoader = new FXMLLoader();
			rootLoader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
			rootLayout = (BorderPane) rootLoader.load();
			rootController = rootLoader.getController();
			rootController.setStage(rootStage);

			LoadLibraryTask task = new LoadLibraryTask();
			Thread t = new Thread(task,"LoadLibrary Thread");
			t.setDaemon(true);
			t.start();
			
			FXMLLoader pqLoader = new FXMLLoader();
			pqLoader.setLocation(MainApp.class.getResource("view/PlayQueueView.fxml"));
			playQueueLayout = (AnchorPane) pqLoader.load();
			playQueueController = pqLoader.getController();
			
			Scene mainScene = new Scene(rootLayout,1200,775);
			rootStage.setMinWidth(1200);
			rootStage.setMinHeight(775);
			rootStage.setScene(mainScene);
			rootStage.show();

			// Set the dropdown play queue pane
			ToggleButton playQueueButton = (ToggleButton)rootLayout.lookup("#playQueueButton");
			playQueueLayout.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
			playQueueLayout.setLayoutY(playQueueButton.getLayoutY()+50);
			playQueueLayout.setVisible(false);
			rootLayout.getChildren().add(playQueueLayout);
			
			// The play queue pane moves if the window is resized
			rootStage.widthProperty().addListener((observable, oldValue, newValue) -> {
				playQueueLayout.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
				playQueueLayout.setLayoutY(playQueueButton.getLayoutY()+50);
			});
			rootStage.heightProperty().addListener((observable, oldValue, newValue) -> {
				playQueueLayout.setLayoutX(playQueueButton.getLayoutX()-150+(playQueueButton.getWidth()/2));
				playQueueLayout.setLayoutY(playQueueButton.getLayoutY()+50);
			});
			
			// Closes the play queue pane when click on the view
			rootLayout.setOnMouseClicked(event -> {if(playQueueLayout.isVisible()) playQueueLayout.setVisible(false);});
			TableView<Track> trackTable = (TableView<Track>) rootLayout.lookup("#trackTable");
			trackTable.setOnMouseClicked(event -> {if(playQueueLayout.isVisible()) playQueueLayout.setVisible(false);});
			
			// Set the thumb image of the track slider
			StackPane thumb = (StackPane)rootLayout.lookup("#trackSlider").lookup(".thumb");
			thumb.getChildren().clear();
			thumb.getChildren().add(new ImageView(new Image(SceneManager.class.getResourceAsStream("/icons/sliderthumb-icon.png"))));
			thumb.setPrefSize(5,5);
		} catch (IOException e) {
			errorHandler.addError(e, ErrorType.COMMON);
			errorHandler.showErrorDialog(ErrorType.COMMON);
		}
	}
}