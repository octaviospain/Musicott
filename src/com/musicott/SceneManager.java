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

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.error.ErrorHandler;
import com.musicott.error.ErrorType;
import com.musicott.view.EditInfoController;
import com.musicott.view.ImportController;
import com.musicott.view.ItunesImportController;
import com.musicott.view.PlayQueueController;
import com.musicott.view.RootController;
import com.musicott.model.Track;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

/**
 * @author Octavio Calleya
 *
 */
public class SceneManager {
	
	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	
	private Stage mainStage;
	private Stage importStage;
	private Stage itunesImportStage;
	private Stage editStage;
	private Stage progressStage;
	
	private EditInfoController editController;
	private RootController rootController;
	private ImportController importController;
	private ItunesImportController itunesImportController;
	private PlayQueueController playQueueController;
	
	private static volatile SceneManager instance;
	private static ErrorHandler errorHandler;
	
	private SceneManager() {
	}
	
	public static SceneManager getInstance() {
		if(instance == null) {
			instance = new SceneManager();
			errorHandler = ErrorHandler.getInstance();
		}
		return instance;
	}
	
	protected void setMainStage(Stage mainStage) {
		this.mainStage = mainStage;
	}
	
	public Stage getMainStage() {
		return mainStage;
	}
	
	protected void setRootController(RootController rootController) {
		this.rootController = rootController;
	}
	
	public RootController getRootController() {
		return rootController;
	}
	
	protected void setPlayQueueController(PlayQueueController playQueueController) {
		this.playQueueController = playQueueController;
	}
	
	public PlayQueueController getPlayQueueController() {
		return this.playQueueController;
	}
	
	public void openEditScene(List<Track> selection) {
		if(editStage == null) {
			try {
				editStage = new Stage();
				editStage.setTitle("Edit");
				
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(getClass().getResource("/view/EditInfoLayout.fxml"));
				AnchorPane editLayout = (AnchorPane) loader.load();
				editController = loader.getController();
				editController.setStage(editStage);
				editController.setSelection(selection);
				
				Scene editScene = new Scene(editLayout);
				editStage.initModality(Modality.APPLICATION_MODAL);
				editStage.initOwner(editScene.getWindow());
				editStage.setScene(editScene);
				editStage.setResizable(false);
			} catch (IOException e) {
				LOG.error("Error", e);
				errorHandler.addError(e, ErrorType.COMMON);
				errorHandler.showErrorDialog(ErrorType.COMMON);
			}
		}
		editController.setSelection(selection);
		editStage.showAndWait();
	}
	
	public void openImportScene() {
		if(importStage == null) {
			try {
				importStage = new Stage();
				importStage.setTitle("Import");
				
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(getClass().getResource("/view/ImportCollectionLayout.fxml"));
	
				AnchorPane importLayout = (AnchorPane) loader.load();
				importController = loader.getController();
				importController.setStage(importStage);
				
				Scene importMainScene = new Scene(importLayout);
				importStage.initModality(Modality.APPLICATION_MODAL);
				importStage.initOwner(importMainScene.getWindow());
				importStage.setScene(importMainScene);
				importStage.setResizable(false);
			} catch (IOException e) {
				LOG.error("Error", e);
				errorHandler.addError(e, ErrorType.COMMON);
				errorHandler.showErrorDialog(ErrorType.COMMON);
			}
		}
		importStage.showAndWait();
	}

	public void openItunesImportScene() {
		if(itunesImportStage == null) {
			try {
				itunesImportStage = new Stage();
				itunesImportStage.setTitle("Itunes Import");
				
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(getClass().getResource("/view/ItunesImportLayout.fxml"));
				
				AnchorPane itunesImportLayout = (AnchorPane) loader.load();
				itunesImportController = loader.getController();
				itunesImportController.setStage(itunesImportStage);
				
				Scene itunesImportScene = new Scene(itunesImportLayout);
				itunesImportStage.initModality(Modality.APPLICATION_MODAL);
				itunesImportStage.initOwner(itunesImportScene.getWindow());
				itunesImportStage.setScene(itunesImportScene);
				itunesImportStage.setResizable(false);
			} catch (IOException e) {
				LOG.error("Error", e);
				errorHandler.addError(e, ErrorType.COMMON);
				errorHandler.showErrorDialog(ErrorType.COMMON);
			}
		}
		itunesImportStage.showAndWait();
	}
	
	public void openIndeterminatedProgressScene() {
		if(progressStage == null) {
			try {
				progressStage = new Stage();
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(getClass().getResource("/view/ProgressLayout.fxml"));
				AnchorPane progressLayout =  (AnchorPane) loader.load();
				((ProgressBar) progressLayout.lookup(".progress-bar")).setStyle("-fx-accent: rgb(99,255,109);");
				
				Scene progressScene = new Scene(progressLayout);
				progressStage.setOnCloseRequest(event -> event.consume());
				progressStage.initModality(Modality.APPLICATION_MODAL);
				progressStage.initOwner(progressScene.getWindow());
				progressStage.setScene(progressScene);
				progressStage.setResizable(false);
			} catch(IOException e) {
				LOG.error("Error", e);
				errorHandler.addError(e, ErrorType.COMMON);
				errorHandler.showErrorDialog(ErrorType.COMMON);
			}
		}
		progressStage.showAndWait();
	}
	
	public void closeIndeterminatedProgressScene() {
		progressStage.close();
	}
	
	public void openLastFMLogin() {
		Dialog<Optional<Pair<String, String>>> dialog = new Dialog<>();
		dialog.setWidth(200);
		dialog.setHeight(150);
		dialog.initModality(Modality.NONE);
		dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/dialog.css").toExternalForm());
		dialog.setTitle("LastFM Login");
		dialog.setHeaderText("Log in to your LastFM account");
		dialog.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/images/lastfm-logo.png"))));

		ButtonType loginButtonType = new ButtonType("Login", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		TextField usernameField = new TextField();
		usernameField.setPromptText("Username/email");
		PasswordField passwordField = new PasswordField();
		passwordField.setPromptText("Password");

		grid.add(new Label("Username:"), 0, 0);
		grid.add(usernameField, 1, 0);
		grid.add(new Label("Password:"), 0, 1);
		grid.add(passwordField, 1, 1);
		
		Label contentText = new Label("Log in to your LastFM account to scrobble your listened tracks on your profile");
		BorderPane pane = new BorderPane();
		pane.setCenter(grid);
		pane.setTop(contentText);

		dialog.getDialogPane().setContent(pane);
		dialog.setResultConverter(dialogButton -> {
		    if (dialogButton == loginButtonType) {
		        return Optional.of(new Pair<>(usernameField.getText(), passwordField.getText()));
		    }
		    return null;
		});
		dialog.show();
		dialog.resultProperty().addListener(listener -> {
			dialog.getResult().ifPresent(usernamePassword -> {
				MainPreferences.getInstance().setLasFMUsername(usernamePassword.getKey());
				MainPreferences.getInstance().setLasFMPassword(usernamePassword.getValue());
			});
		});
	}
}