package com.musicott.view;

import java.util.List;

import com.musicott.SceneManager;
import com.musicott.model.Track;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class RootLayoutController {
	
	@FXML
	private Menu menuFile;
	@FXML
	private MenuItem menuOpenImport;
	@FXML
	private Button playButton;
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
	private TableColumn<Track,Boolean> m4aCol;
	@FXML
	private TableColumn<Track,Boolean> flacCol;
	@FXML
	private TableColumn<Track,Boolean> wavCol;
	@FXML
	private TableColumn<Track,Boolean> coverCol;
	@FXML
	private TableColumn<Track,Boolean> inDiskCol;

	private ObservableList<Track> tracks;
	
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
		m4aCol.setCellValueFactory(cellData -> cellData.getValue().getHasFlacVersion());
		flacCol.setCellValueFactory(cellData -> cellData.getValue().getHasFlacVersion());
		wavCol.setCellValueFactory(cellData -> cellData.getValue().getHasWavVersion());
		coverCol.setCellValueFactory(cellData -> cellData.getValue().getHasCover());
		inDiskCol.setCellValueFactory(cellData -> cellData.getValue().getIsInDisk());
		
		tracks = trackTable.getItems();
	}	
	
	public void addTracks(List<Track> tracks) {
		if(tracks != null && tracks.size()!=0) 
			trackTable.getItems().addAll(tracks);
	}
	
	@FXML
	private void doImportCollection() {
		SceneManager.getInstance().openImportScene();
	}
	
	@FXML
	private void handleExit() {
		System.exit(0);
	}
}