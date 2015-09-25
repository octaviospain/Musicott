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

package com.musicott.view;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.SwingUtilities;

import com.musicott.SceneManager;
import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.player.FlacPlayer;
import com.musicott.player.PlayerFacade;
import com.musicott.player.NativePlayer;
import com.musicott.player.TrackPlayer;
import com.musicott.task.OpenTask;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class RootLayoutController {
	
	private static final double AMOUNT_VOLUME = 0.05;
	
	@FXML
	private Menu menuFile;
	@FXML
	private MenuItem menuItemImport;
	@FXML
	private MenuItem  menuItemOpen;
	@FXML
	private Menu menuEdit;
	@FXML
	private MenuItem menuItemDelete;
	@FXML
	private MenuItem menuItemEdit; 
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
	private ToggleButton playQueueButton;
	@FXML
	private ImageView currentCover;
	@FXML
	private StackPane playerStackPane;
	@FXML
	private Label titleLabel;
	@FXML
	private Label artistAlbumLabel;
	@FXML
	private Label currentTimeLabel;
	@FXML
	private Label remainingTimeLabel;
	@FXML
	private Slider trackSlider;
	@FXML
	private ProgressBar trackProgressBar;
	@FXML
	private Slider volumeSlider;
	@FXML
	private TextField searchTextField;
	@FXML
	private ProgressBar volumeProgressBar;	
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
	private TableColumn<Track,LocalDateTime> dateModifiedCol;
	@FXML
	private TableColumn<Track,LocalDateTime> dateAddedCol;
	@FXML
	private TableColumn<Track,Number> sizeCol;
	@FXML
	private TableColumn<Track,Duration> totalTimeCol;
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
	private WaveformPanel waveformPanel;
	
	private ObservableList<Track> tracks;
	private ObservableList<Track> selection;
	private FilteredList<Track> filteredTracks;
	
	private Stage rootStage;
	private SceneManager sc;
	
	private MusicLibrary ml;
	private PlayerFacade player;
	
	private boolean emptyTable = true;
	
 	public RootLayoutController() {
	}
	
	@FXML
	public void initialize() {
		nameCol.setCellValueFactory(cellData -> cellData.getValue().getNameProperty());
		artistCol.setCellValueFactory(cellData -> cellData.getValue().getArtistProperty());
		albumCol.setCellValueFactory(cellData -> cellData.getValue().getAlbumProperty());
		genreCol.setCellValueFactory(cellData -> cellData.getValue().getGenreProperty());
		commentsCol.setCellValueFactory(cellData -> cellData.getValue().getCommentsProperty());
		albumArtistCol.setCellValueFactory(cellData -> cellData.getValue().getAlbumArtistProperty());
		labelCol.setCellValueFactory(cellData -> cellData.getValue().getLabelProperty());
		dateModifiedCol.setCellValueFactory(cellData -> cellData.getValue().getDateModifiedProperty());
		dateAddedCol.setCellValueFactory(cellData -> new SimpleObjectProperty<LocalDateTime>(cellData.getValue().getDateAdded()));
		sizeCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSize()));
		totalTimeCol.setCellValueFactory(cellData -> new SimpleObjectProperty<Duration>(cellData.getValue().getTotalTime()));
		yearCol.setCellValueFactory(cellData -> cellData.getValue().getYearProperty());
		bitRateCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getBitRate()));
		playCountCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getPlayCount()));
		discNumberCol.setCellValueFactory(cellData -> cellData.getValue().getDiscNumberProperty());
		trackNumberCol.setCellValueFactory(cellData -> cellData.getValue().getTrackNumberProperty());
		bpmCol.setCellValueFactory(cellData -> cellData.getValue().getBpmProperty());
		coverCol.setCellValueFactory(cellData -> cellData.getValue().getHasCoverProperty());
		coverCol.setCellFactory(CheckBoxTableCell.forTableColumn(coverCol));
		bpmCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		bpmCol.setCellFactory(columns -> {
			return new TableCell<Track, Number>() {
				@Override
				protected void updateItem(Number item, boolean empty) {
					super.updateItem(item, empty);
					if(empty || item == null)
						setText("");
					else
						if(((int) item) == 0 || ((int) item) == -1)
							setText("");
						else
							setText(""+item);
				}
			};
		});
		discNumberCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		discNumberCol.setCellFactory(columns -> {
			return new TableCell<Track, Number>() {
				@Override
				protected void updateItem(Number item, boolean empty) {
					super.updateItem(item, empty);
					if(empty || item == null)
						setText("");
					else
						if(((int) item) == 0)
							setText("");
						else
							setText(""+item);
				}
			};
		});
		trackNumberCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		trackNumberCol.setCellFactory(columns -> {
			return new TableCell<Track, Number>() {
				@Override
				protected void updateItem(Number item, boolean empty) {
					super.updateItem(item, empty);
					if(empty || item == null)
						setText("");
					else
						if(((int) item) == 0)
							setText("");
						else
							setText(""+item);
				}
			};
		});
		yearCol.setCellFactory(columns -> {
			return new TableCell<Track, Number>() {
				@Override
				protected void updateItem(Number item, boolean empty) {
					super.updateItem(item, empty);
					if(empty || item == null)
						setText("");
					else
						if(((int) item) == 0)
							setText("");
						else
							setText(""+item);
				}
			};
		});
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
		dateModifiedCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		dateModifiedCol.setCellFactory(column -> {
			return new TableCell<Track,LocalDateTime>() {
				@Override
				protected void updateItem(LocalDateTime item, boolean empty) {
					super.updateItem(item, empty);
					if(item == null)
						setText("");
					else
						setText(item.format(dateFormatter));
				}
			}; 
		});
		dateAddedCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		dateAddedCol.setSortType(TableColumn.SortType.DESCENDING);
		dateAddedCol.setCellFactory(column -> {
			return new TableCell<Track,LocalDateTime>() {
				@Override
				protected void updateItem(LocalDateTime item, boolean empty) {
					super.updateItem(item, empty);
					if(item == null)
						setText("");
					else
						setText(item.format(dateFormatter));
				}
			}; 
		});
		sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		sizeCol.setCellFactory(column -> {
			return new TableCell<Track,Number>() {
				@Override
				protected void updateItem(Number item, boolean empty) {
					super.updateItem(item, empty);
					if(item == null)
						setText("");	
					else {
						int kiloBytes = ((int) item)/1024;
						if(kiloBytes < 1024)
							setText(kiloBytes+" KB");
						else {
							int megaBytes = kiloBytes/1024;
							String strKiloBytes = ""+kiloBytes%1024;
							setText(megaBytes+","+(strKiloBytes.length()>1 ? strKiloBytes.substring(0, 1) : strKiloBytes)+" MB");
						}
					}
				}
			};
		});
		totalTimeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		totalTimeCol.setCellFactory(column -> {
			return new TableCell<Track, Duration>() {
				@Override
				protected void updateItem(Duration item, boolean empty) {
					super.updateItem(item, empty);
					if(item == null)
						setText("");
					else {
						int hours = (int)item.toHours();
						int mins = (int)item.subtract(Duration.hours(hours)).toMinutes();
						int secs = (int)item.subtract(Duration.minutes(mins)).subtract(Duration.hours(hours)).toSeconds();
						setText((hours>0 ? hours+":" : "")+(mins<10 ? "0"+mins : mins)+":"+(secs<10 ? "0"+secs : secs));
					}
				}
			};
		});
		playCountCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		bitRateCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		selection = trackTable.getSelectionModel().getSelectedItems();
		trackTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		trackTable.getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> selection = trackTable.getSelectionModel().getSelectedItems()));
		trackTable.getSortOrder().add(dateAddedCol);
		sc = SceneManager.getInstance();
		ml = MusicLibrary.getInstance();
		prevButton.setDisable(true);
		nextButton.setDisable(true);
		playButton.setDisable(true);
		trackSlider.setDisable(true);
		trackSlider.setValue(0.0);
		volumeSlider.setMin(0.0);
		volumeSlider.setMax(1.0);
		volumeSlider.setValue(1.0);
		volumeProgressBar.setProgress(1.0);
		volumeSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {if(!isChanging) volumeProgressBar.setProgress(volumeSlider.getValue());});
		volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> volumeProgressBar.setProgress(newValue.doubleValue()));
		SwingNode waveformSwingNode = new SwingNode();
		SwingUtilities.invokeLater(() -> {
            waveformPanel = new WaveformPanel(515, 50);
            waveformSwingNode.setContent(waveformPanel);
		});
		playerStackPane.getChildren().add(0, waveformSwingNode);
		
		tracks = trackTable.getItems();
		filteredTracks = new FilteredList<Track>(tracks, predicate -> true);
		searchTextField.textProperty().addListener((observable, oldText, newText) -> {
			filteredTracks.setPredicate(track -> {
				boolean result = true;
				if(newText != null && !newText.isEmpty()) {
					if(track.getName().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getArtist().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getLabel().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getGenre().toLowerCase().contains(newText.toLowerCase()) ||
					   track.getAlbum().toLowerCase().contains(newText.toLowerCase()))
						result = true;
					else
						result = false;
				}
				return result;
			});
		});
		SortedList<Track> sortedTracks = new SortedList<Track>(filteredTracks);
		sortedTracks.comparatorProperty().bind(trackTable.comparatorProperty());
		trackTable.setItems(sortedTracks);
		ml.setTracks(tracks);
		playButton.setDisable(true);
		trackSlider.setDisable(true);
		emptyTable = true;

		prevButton.setDisable(true);
		nextButton.setDisable(true);
		
		player = new PlayerFacade(tracks);	
		tracks.addListener(((ListChangeListener.Change<? extends Track> c) -> {
			while(c.next()) {
				if(c.wasRemoved()) {
					player.removeTracks(c.getRemoved());
					sc.saveLibrary();
				}
				else
					sc.saveLibrary();
			}
		}));
		
		// Set up the ContextMenu
		MenuItem cmPlay = new MenuItem("Play");
		cmPlay.setOnAction(event -> {if(!selection.isEmpty()) {player.play(selection); setPlaying();}});
		MenuItem cmEdit = new MenuItem("Edit");
		cmEdit.setOnAction(event -> doEdit());
		MenuItem cmDelete = new MenuItem("Delete");
		cmDelete.setOnAction(event -> doDelete());
		MenuItem cmAddToQueue = new MenuItem("Add to Play Queue");
		cmAddToQueue.setOnAction(event -> {
			if(!selection.isEmpty())
				player.addTracks(selection);
		});
		ContextMenu cm = new ContextMenu();
		cm.getItems().add(cmPlay);
		cm.getItems().add(cmAddToQueue);
		cm.getItems().add(cmEdit);
		cm.getItems().add(cmDelete);
		// Double click on row = play that track
		trackTable.setRowFactory(tv -> {
			TableRow<Track> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if(event.getClickCount() == 2 && !row.isEmpty() && !row.getItem().getFileFormat().equals("flac")) //TODO
					player.play(row.getItem());
			});
			return row;
		});
		trackTable.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			if(event.getButton() == MouseButton.SECONDARY)
				cm.show(trackTable,event.getScreenX(),event.getScreenY());
			else if(event.getButton() == MouseButton.PRIMARY && cm.isShowing())
				cm.hide();
		});
		trackTable.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if(event.getCode() == KeyCode.ENTER) {
				for(Track t: selection)
					if(t.getFileName().substring(t.getFileName().length()-4).equals("flac"))
						selection.remove(t);
				if(!selection.isEmpty()) {
					player.play(selection);
					setPlaying();
				}
			}
			else if(event.getCode() == KeyCode.SPACE) {
				if(player.getTrackPlayer().getStatus().equals("PLAYING")) {
					playButton.setSelected(false);
					player.pause();
				}
				else if(player.getTrackPlayer().getStatus().equals("PAUSED")) {
					playButton.setSelected(true);
					player.play();
				}
			}
		});
	}	
	
	public void addTracks(List<Track> listTracks) {
		if(listTracks != null && !listTracks.isEmpty()) {
			tracks.addAll(listTracks);	
			ml.setTracks(tracks);
			sc.getPlayQueueController().setPlayer(player);
			if(emptyTable) {
				playButton.setDisable(false);
				emptyTable = false;
			}
		}
	}
	
	public void setStage(Stage stage) {
		rootStage = stage;
	}
	
	public void setStopped() {
		playButton.setSelected(false);
		trackSlider.setDisable(true);
		nextButton.setDisable(true);
		prevButton.setDisable(true);
		titleLabel.setText("");
		artistAlbumLabel.setVisible(false);
		currentTimeLabel.setVisible(false);
		remainingTimeLabel.setVisible(false);
		currentCover.setImage(null);
		SwingUtilities.invokeLater(() -> waveformPanel.clear());
		if(player.getTrackPlayer() instanceof NativePlayer)
			volumeSlider.valueProperty().unbindBidirectional(((NativePlayer)player.getTrackPlayer()).getMediaPlayer().volumeProperty());
		else if (player.getTrackPlayer() instanceof FlacPlayer) {
			//TODO
		}
	}
	
	public void setPlaying() {
		playButton.setSelected(true);
		trackSlider.setDisable(false);
		nextButton.setDisable(false);
		prevButton.setDisable(false);
		artistAlbumLabel.setVisible(true);
		currentTimeLabel.setVisible(true);
		remainingTimeLabel.setVisible(true);
	}
	
	public void preparePlayerInfo(TrackPlayer currentPlayer, Track currentTrack) {
		// Set up the player and the view related to it
		if(currentPlayer instanceof NativePlayer)
			setUpPlayer(((NativePlayer) currentPlayer).getMediaPlayer());
		else if(currentPlayer instanceof FlacPlayer)
			setUpPlayer((FlacPlayer) currentPlayer);
		
		SwingUtilities.invokeLater(() -> waveformPanel.setTrack(currentTrack));
		titleLabel.setText(currentTrack.getName());
		artistAlbumLabel.setText(currentTrack.getArtist()+" - "+currentTrack.getAlbum());
		if(currentTrack.getHasCover())
			currentCover.setImage(new Image(new ByteArrayInputStream(currentTrack.getCoverFile())));
		else
			currentCover.setImage(new Image(RootLayoutController.class.getResourceAsStream("/images/default-cover-icon.png")));
	}
	
	private void setUpPlayer(MediaPlayer mediaPlayer) {
		trackSlider.valueProperty().addListener((observable) -> {
			if(trackSlider.isValueChanging()) {
				trackProgressBar.setProgress(trackSlider.getValue() / mediaPlayer.getStopTime().toSeconds());
				mediaPlayer.seek(Duration.seconds(trackSlider.getValue()));
			}
		});
		trackSlider.addEventHandler(MouseEvent.MOUSE_CLICKED, (event) -> {
			trackProgressBar.setProgress(trackSlider.getValue() / mediaPlayer.getStopTime().toSeconds());
			mediaPlayer.seek(Duration.seconds(trackSlider.getValue()));
		});
		mediaPlayer.totalDurationProperty().addListener((observable, oldDuration, newDuration) -> trackSlider.setMax(newDuration.toSeconds()));
		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> {if (!trackSlider.isValueChanging()) trackSlider.setValue(newTime.toSeconds());});
		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> trackProgressBar.setProgress(newTime.toSeconds() / mediaPlayer.getStopTime().toSeconds()));
		mediaPlayer.volumeProperty().bindBidirectional(volumeSlider.valueProperty());
		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> formatTime(newTime,mediaPlayer.getMedia().getDuration()));	
	}
	
	private void setUpPlayer(FlacPlayer flacPlayer) {
		//TODO
	}
	
	private void formatTime(Duration elapsed, Duration total) {
		int currentHours = (int)elapsed.toHours();
		int currentMins = (int)elapsed.subtract(Duration.hours(currentHours)).toMinutes();
		int currentSecs = (int)elapsed.subtract(Duration.minutes(currentMins)).subtract(Duration.hours(currentHours)).toSeconds();
		currentTimeLabel.setText(((int)total.toHours()>0 ? currentHours+":" : "")+(currentMins<10 ? "0"+currentMins : currentMins)+":"+(currentSecs<10 ? "0"+currentSecs : currentSecs));
		
		Duration remaining = total.subtract(elapsed);
		int remainingHours = (int)remaining.toHours();
		int remainingMins = (int)remaining.subtract(Duration.hours(remainingHours)).toMinutes();
		int remainingSecs = (int)remaining.subtract(Duration.minutes(remainingMins)).subtract(Duration.hours(remainingHours)).toSeconds();
		remainingTimeLabel.setText("-"+((int)total.toHours()>0 ? remainingHours+":" : "")+(remainingMins<10 ? "0"+remainingMins : remainingMins)+":"+(remainingSecs<10 ? "0"+remainingSecs : remainingSecs));
	}
	
	@FXML
	private void doShowHidePlayQueue() {
		sc.showHidePlayQueue();
	}
	
	@FXML
	private void doPlayPause() {
		if(playButton.isSelected()) {
			if(trackSlider.isDisabled())
				trackSlider.setDisable(false);
			if(prevButton.isDisabled())
				prevButton.setDisable(false);
			if(nextButton.isDisabled())
				nextButton.setDisable(false);
			player.play();
		}
		else
			player.pause();
	}
	
	@FXML
	private void doNext() {
		if(!nextButton.isDisabled())
			player.next();
	}
	
	@FXML
	private void doPrevious() {	
		if(!prevButton.isDisabled())
			player.previous();
	}
	
	@FXML
	private void doIncreaseVolume() {
		if(player != null)
			player.increaseVolume(AMOUNT_VOLUME);
		volumeSlider.setValue(volumeSlider.getValue()+AMOUNT_VOLUME);
	}
	
	@FXML
	private void doDecreaseVolume() {
		if(player != null)
			player.decreaseVolume(AMOUNT_VOLUME);
		volumeSlider.setValue(volumeSlider.getValue()-AMOUNT_VOLUME);
	}
	
	@FXML
	private void doSelectCurrentTrack() {
		trackTable.getSelectionModel().clearSelection();
		trackTable.getSelectionModel().select(player.getCurrentTrack());
	}
	
	@FXML
	private void doDelete() {
		if(selection != null && !selection.isEmpty()) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("");
			alert.setHeaderText("");
			alert.setContentText("Delete this files from Musicott?");
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				tracks.removeAll(selection);
				Map<Track,float[]> waveformsMap = ml.getWaveforms();
				for(Track t : selection) {
					if(waveformsMap.containsKey(t))
						waveformsMap.remove(t);
				}
				sc.saveLibrary(); 
				setStopped();
				if(tracks.size() == 0) {
					playButton.setDisable(true);
					emptyTable = true;					
				}
			}
			else
				alert.close();
		}
	}
	
	@FXML
	private void doEdit() {
		if(selection != null & !selection.isEmpty()) {
			if(selection.size() > 1) {
				Alert alert = new Alert(AlertType.CONFIRMATION);
				alert.setTitle("");
				alert.setHeaderText("");
				alert.setContentText("Are you sure you want to edit multiple files?");
				Optional<ButtonType> result = alert.showAndWait();
				if (result.get() == ButtonType.OK)
					sc.openEditScene(selection);
				else
					alert.close();
			}
			else
				sc.openEditScene(selection);
		}
	}
	
	@FXML
	private void doOpen() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Open file(s)...");
		chooser.getExtensionFilters().addAll(
				new ExtensionFilter("All Supported (*.mp3, *.flac, *.wav, *.m4a)","*.mp3", "*.flac", "*.wav", "*.m4a"),
				new ExtensionFilter("Mp3 Files", "*.mp3"),
				new ExtensionFilter("Flac Files","*.flac"),
				new ExtensionFilter("Wav Files", "*.wav"),
				new ExtensionFilter("M4a Files", "*.m4a"));
		List<File> files = chooser.showOpenMultipleDialog(rootStage);
		if(files != null) {
			OpenTask task = new OpenTask(files);
			sc.showImportProgressScene(task,false);
			Thread t = new Thread(task, "Open Thread");
			t.setDaemon(true);
			t.start();
		}
	}
	
	@FXML
	private void doImportCollection() {
		sc.openImportScene();
	}
	
	@FXML
	private void doAbout() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About Musicott");
		alert.setHeaderText("Musicott");
		Label text = new Label(" Version 0.6.0\n\n Copyright © 2015 Octavio Calleya.");
		Label text2 = new Label(" Licensed under GNU GPLv3. This product includes\n software developed by other open source projects.");
		Hyperlink githubLink = new Hyperlink("https://github.com/octaviospain/Musicott/");
		githubLink.setOnAction(event -> {
			sc.getApplication().getHostServices().showDocument(githubLink.getText());
		});
		FlowPane fp = new FlowPane();
		fp.getChildren().addAll(text, githubLink, text2);
		alert.getDialogPane().contentProperty().set(fp);
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