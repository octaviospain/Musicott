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

package com.musicott.view.custom;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.player.PlayerFacade;
import com.musicott.util.Utils;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class TrackTableView extends TableView<Map.Entry<Integer, Track>> {
	
	private TableColumn<Map.Entry<Integer, Track>, String> nameCol, artistCol, albumCol, genreCol, commentsCol, albumArtistCol, labelCol;
	private TableColumn<Map.Entry<Integer, Track>, LocalDateTime> dateModifiedCol, dateAddedCol;
	private TableColumn<Map.Entry<Integer, Track>, Number> sizeCol, yearCol, bitRateCol, playCountCol, discNumberCol, bpmCol, trackNumberCol;
	private TableColumn<Map.Entry<Integer, Track>, Duration> totalTimeCol;
	private TableColumn<Map.Entry<Integer, Track>, Boolean> coverCol;

	private ObservableList<Map.Entry<Integer, Track>> tracks;
	private List<Map.Entry<Integer, Track>> selection;
	
	private MusicLibrary ml;
	private PlayerFacade player;
	
	@SuppressWarnings("unchecked")
	public TrackTableView() {
		super();
		ml = MusicLibrary.getInstance();
		player = PlayerFacade.getInstance();
		tracks = ml.getShowingTracks();
		selection = getSelectionModel().getSelectedItems();
		setId("trackTable");
		initColumns();
		getColumns().addAll(artistCol, nameCol, albumCol, genreCol, labelCol, bpmCol, totalTimeCol, yearCol, sizeCol, trackNumberCol, discNumberCol);
		getColumns().addAll(albumArtistCol, commentsCol, bitRateCol, playCountCol, dateModifiedCol, dateAddedCol, coverCol);
		GridPane.setHgrow(this, Priority.ALWAYS);
		GridPane.setVgrow(this, Priority.ALWAYS);
		setPrefWidth(USE_COMPUTED_SIZE);
		setPrefHeight(USE_COMPUTED_SIZE);
		setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);
		getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		getSelectionModel().selectedIndexProperty().addListener(((observable, oldValue, newValue) -> selection = getSelectionModel().getSelectedItems()));
		getSortOrder().add(dateAddedCol);
		// Double click on row = play that track
		setRowFactory(tv -> {
			TableRow<Map.Entry<Integer, Track>> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if(event.getClickCount() == 2 && !row.isEmpty()) {
					List<Integer> singleTrackIDList = new ArrayList<>();
					singleTrackIDList.add(row.getItem().getKey());
					player.addTracks(singleTrackIDList, true);
				}
			});
			return row;
		});
		// Enter key pressed = play; Space key = pause/resume
		addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			if(event.getCode() == KeyCode.ENTER) {
				player.addTracks(selection.stream().map(Map.Entry::getKey).collect(Collectors.toList()), true);
			}
			else if(event.getCode() == KeyCode.SPACE) {
				String playerStatus = player.getTrackPlayer().getStatus();
				if(playerStatus.equals("PLAYING"))
					player.pause();
				else if(playerStatus.equals("PAUSED"))
					player.resume();
				else if(playerStatus.equals("STOPPED"))
					player.play(true);
			}
		});
		setItems(tracks);
	}
	
	private void initColumns() {
		Callback<TableColumn<Map.Entry<Integer, Track>,Number>, TableCell<Map.Entry<Integer, Track>,Number>> numericCellFactory = columns -> new TableCell<Map.Entry<Integer, Track>, Number>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if(empty || item == null)
					setText("");
				else if(((int) item) < 1)
						setText("");
					else
						setText(""+item);
			}
		};
		Callback<TableColumn<Map.Entry<Integer, Track>, LocalDateTime>, TableCell<Map.Entry<Integer, Track>, LocalDateTime>> dateCellFactory = column -> new TableCell<Map.Entry<Integer, Track>, LocalDateTime>() {
			@Override
			protected void updateItem(LocalDateTime item, boolean empty) {
				super.updateItem(item, empty);
				DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
				if(item == null)
					setText("");
				else
					setText(item.format(dateFormatter));
			}
		};
		nameCol = new TableColumn<>("Name");
		nameCol.setPrefWidth(170);
		nameCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getNameProperty());
		artistCol = new TableColumn<>("Artist");
		artistCol.setPrefWidth(170);
		artistCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getArtistProperty());
		albumCol = new TableColumn<>("Album");
		albumCol.setPrefWidth(170);
		albumCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getAlbumProperty());
		genreCol = new TableColumn<>("Genre");
		genreCol.setPrefWidth(120);
		genreCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getGenreProperty());
		commentsCol = new TableColumn<>("Comments");
		commentsCol.setPrefWidth(150);
		commentsCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getCommentsProperty());
		albumArtistCol = new TableColumn<>("Album Artist");
		albumArtistCol.setPrefWidth(100);
		albumArtistCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getAlbumArtistProperty());
		labelCol = new TableColumn<>("Label");
		labelCol.setPrefWidth(120);
		labelCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getLabelProperty());
		dateModifiedCol = new TableColumn<>("Modified");
		dateModifiedCol.setPrefWidth(110);
		dateModifiedCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getDateModifiedProperty());
		dateModifiedCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		dateModifiedCol.setCellFactory(dateCellFactory);
		dateAddedCol = new TableColumn<>("Added");
		dateAddedCol.setPrefWidth(110);
		dateAddedCol.setCellValueFactory(cellData -> new SimpleObjectProperty<LocalDateTime>(cellData.getValue().getValue().getDateAdded()));
		dateAddedCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		dateAddedCol.setSortType(TableColumn.SortType.DESCENDING); 	// Default sort of the table
		dateAddedCol.setCellFactory(dateCellFactory);
		sizeCol = new TableColumn<>("Size");
		sizeCol.setPrefWidth(64);
		sizeCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getValue().getSize()));
		sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		sizeCol.setCellFactory(column -> {return new TableCell<Map.Entry<Integer, Track>,Number>() {
				@Override
				protected void updateItem(Number item, boolean empty) {
					super.updateItem(item, empty);
					if(item == null)
						setText("");	
					else
						setText(Utils.byteSizeString(item.longValue(), 1));
				}
			};});
		totalTimeCol = new TableColumn<>("Duration");
		totalTimeCol.setPrefWidth(60);
		totalTimeCol.setCellValueFactory(cellData -> new SimpleObjectProperty<Duration>(cellData.getValue().getValue().getTotalTime()));
		totalTimeCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		totalTimeCol.setCellFactory(column -> {return new TableCell<Map.Entry<Integer, Track>, Duration>() {
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
			};});
		yearCol = new TableColumn<>("Year");
		yearCol.setPrefWidth(60);
		yearCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getYearProperty());
		yearCol.setCellFactory(numericCellFactory);
		yearCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		playCountCol = new TableColumn<>("Plays");
		playCountCol.setPrefWidth(50);
		playCountCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getPlayCountProperty());
		playCountCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		discNumberCol = new TableColumn<>("Disc Num");
		discNumberCol.setPrefWidth(45);
		discNumberCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getDiscNumberProperty());
		discNumberCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		discNumberCol.setCellFactory(numericCellFactory);
		trackNumberCol = new TableColumn<>("Track Num");
		trackNumberCol.setPrefWidth(45);
		trackNumberCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getTrackNumberProperty());
		trackNumberCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		trackNumberCol.setCellFactory(numericCellFactory);
		bitRateCol = new TableColumn<>("BitRate");
		bitRateCol.setPrefWidth(60);
		bitRateCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getValue().getBitRate()));
		bitRateCol.setCellFactory(columns -> new TableCell<Map.Entry<Integer, Track>, Number>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				if(empty || item == null)
					setText("");
				else if(((int) item) == 0)
						setText("");
					else
						setText(""+item);
			}
		});
		bitRateCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		coverCol = new TableColumn<>("Cover");
		coverCol.setPrefWidth(50);
		coverCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getHasCoverProperty());
		coverCol.setCellFactory(CheckBoxTableCell.forTableColumn(coverCol));
		bpmCol = new TableColumn<>("BPM");
		bpmCol.setPrefWidth(60);
		bpmCol.setCellValueFactory(cellData -> cellData.getValue().getValue().getBpmProperty());
		bpmCol.setStyle("-fx-alignment: CENTER-RIGHT;");
		bpmCol.setCellFactory(numericCellFactory);
	}
}