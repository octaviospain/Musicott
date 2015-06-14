package com.musicott.view;

import java.io.File;
import java.util.List;

import com.musicott.SceneManager;
import com.musicott.model.Track;
import com.musicott.task.OpenTask;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class RootLayoutController {
	
	@FXML
	private Menu menuFile;
	@FXML
	private MenuItem menuItemImport;
	@FXML
	private MenuItem  menuItemOpen;
	@FXML
	private Menu menuAbout;
	@FXML
	private MenuItem menuItemAbout;
	@FXML
	private ToggleButton playButton;
	@FXML
	private Button prevButton;
	@FXML
	private Button nextButton;
	@FXML
	private TableView<Track> trackTable;
	@FXML
	private TableColumn<Track,String> nameCol;
	@FXML
	private TableColumn<Track,String> artistCol;
	@FXML
	private TableColumn<Track,String> albumCol;
	@FXML
	private TableColumn<Track,String> genreCol;
	@FXML
	private TableColumn<Track,String> commentsCol;
	@FXML
	private TableColumn<Track,String> albumArtistCol;
	@FXML
	private TableColumn<Track,String> labelCol;
	@FXML
	private TableColumn<Track,String> dateModifiedCol;
	@FXML
	private TableColumn<Track,String> dateAddedCol;
	@FXML
	private TableColumn<Track,Number> sizeCol;
	@FXML
	private TableColumn<Track,Number> totalTimeCol;
	@FXML
	private TableColumn<Track,Number> trackNumberCol;
	@FXML
	private TableColumn<Track,Number> yearCol;
	@FXML
	private TableColumn<Track,Number> bitRateCol;
	@FXML
	private TableColumn<Track,Number> playCountCol;
	@FXML
	private TableColumn<Track,Number> discNumberCol;
	@FXML
	private TableColumn<Track,Number> bpmCol;
	@FXML
	private TableColumn<Track,Boolean> coverCol;
	@FXML
	private TableColumn<Track,Boolean> inDiskCol;

	private ObservableList<Track> tracks;
	
	private Stage rootStage;
	
 	public RootLayoutController() {
	}
	
	@FXML
	public void initialize() {
		nameCol.setCellValueFactory(cellData -> cellData.getValue().getName());
		artistCol.setCellValueFactory(cellData -> cellData.getValue().getArtist());
		albumCol.setCellValueFactory(cellData -> cellData.getValue().getAlbum());
		genreCol.setCellValueFactory(cellData -> cellData.getValue().getGenre());
		commentsCol.setCellValueFactory(cellData -> cellData.getValue().getComments());
		albumArtistCol.setCellValueFactory(cellData -> cellData.getValue().getAlbumArtist());
		labelCol.setCellValueFactory(cellData -> cellData.getValue().getLabel());
		dateModifiedCol.setCellValueFactory(cellData -> cellData.getValue().getDateModified().asString());
		dateAddedCol.setCellValueFactory(cellData -> cellData.getValue().getDateAdded().asString());
		sizeCol.setCellValueFactory(cellData -> cellData.getValue().getSize());
		totalTimeCol.setCellValueFactory(cellData -> cellData.getValue().getTotalTime());
		yearCol.setCellValueFactory(cellData -> cellData.getValue().getYear());
		bitRateCol.setCellValueFactory(cellData -> cellData.getValue().getBitRate());
		playCountCol.setCellValueFactory(cellData -> cellData.getValue().getPlayCount());
		discNumberCol.setCellValueFactory(cellData -> cellData.getValue().getDiscNumber());
		bpmCol.setCellValueFactory(cellData -> cellData.getValue().getBPM());
		coverCol.setCellValueFactory(cellData -> cellData.getValue().getHasCover());
		inDiskCol.setCellValueFactory(cellData -> cellData.getValue().getIsInDisk());
		
		tracks = trackTable.getItems();
	}	
	
	public void addTracks(List<Track> tracks) {
		if(tracks != null && tracks.size()!=0) 
			trackTable.getItems().addAll(tracks);
	}
	
	public void setStage(Stage stage) {
		rootStage = stage;
	}
	
	@FXML
	private void doOpen() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open file(s)...");
		chooser.getExtensionFilters().addAll(
				new ExtensionFilter("Audio Files","*.mp3")); //TODO m4a flac & wav when implemented
		List<File> files = chooser.showOpenMultipleDialog(rootStage);
		if(files != null) {
			OpenTask task = new OpenTask(files);
			SceneManager.getInstance().showImportProgressScene(task);
			Thread t = new Thread(task);
			t.setDaemon(true);
			t.start();
		}
	}
	
	@FXML
	private void doImportCollection() {
		SceneManager.getInstance().openImportScene();
	}
	
	@FXML
	private void doAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About Musicott");
		alert.setHeaderText("Musicott");
		alert.setContentText("Musicott is an application for import and organize music files.");
		ImageView iv = new ImageView();
		iv.setImage(new Image("file:resources/images/musicotticon.png"));
		alert.setGraphic(iv);
		alert.showAndWait();
	}
	
	@FXML
	private void handleExit() {
		System.exit(0);
	}
}