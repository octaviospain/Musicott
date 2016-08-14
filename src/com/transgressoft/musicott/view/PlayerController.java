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

package com.transgressoft.musicott.view;

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.view.custom.*;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.embed.swing.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.util.*;
import org.slf4j.*;

import javax.swing.*;
import java.io.*;
import java.util.*;

import static com.transgressoft.musicott.view.custom.TrackTableRow.*;

/**
 * Controller class of the bottom pane that includes the player and the search field.
 *
 * @author Octavio Calleya
 * @version 0.9.1-b
 */
public class PlayerController implements MusicottController {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	private static final double VOLUME_AMOUNT = 0.05;
	private final Image COVER_IMAGE = new Image(getClass().getResourceAsStream(DEFAULT_COVER_IMAGE));

	@FXML
	private GridPane playerGridPane;
	@FXML
	private ToggleButton playButton;
	@FXML
	private ToggleButton playQueueButton;
	@FXML
	private Button prevButton;
	@FXML
	private Button nextButton;
	@FXML
	private ImageView currentCover;
	@FXML
	private SwingNode waveformSwingNode;
	@FXML
	private StackPane playerStackPane;
	@FXML
	private StackPane playQueueStackPane;
	@FXML
	private Label songTitleLabel;
	@FXML
	private Label artistAlbumLabel;
	@FXML
	private Label currentTimeLabel;
	@FXML
	private Label remainingTimeLabel;
	@FXML
	private Slider trackSlider;
	@FXML
	private Slider volumeSlider;
	@FXML
	private ProgressBar trackProgressBar;
	@FXML
	private TextField searchTextField;
	@FXML
	private ProgressBar volumeProgressBar;
	private AnchorPane playQueuePane;
	private WaveformPanel mainWaveformPanel;

	@FXML
	public void initialize() {
		playButton.disableProperty().bind(musicLibrary.emptyLibraryProperty());
		playButton.setOnAction(event -> playPause());
		prevButton.setOnAction(e -> player.previous());
		nextButton.setOnAction(e -> player.next());
		volumeSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
			if (! isChanging)
				volumeProgressBar.setProgress(volumeSlider.getValue());
		});
		volumeSlider.valueProperty().addListener(
				(observable, oldValue, newValue) -> volumeProgressBar.setProgress(newValue.doubleValue()));

		SwingUtilities.invokeLater(() -> {
			mainWaveformPanel = new WaveformPanel(520, 50);
			waveformSwingNode.setContent(mainWaveformPanel);
		});
		playerStackPane.getChildren().add(0, waveformSwingNode);

		playQueueButton.setOnAction(event -> {
			if (playQueuePane.isVisible())
				hidePlayQueue();
			else
				showPlayQueue();
		});

		playerGridPane.setOnMouseClicked(event -> hidePlayQueue());
		playButton.setOnMouseClicked(event -> hidePlayQueue());

		playQueueStackPane.setOnDragOver(event -> {
			event.acceptTransferModes(TransferMode.ANY);
			event.consume();
		});
		playQueueStackPane.setOnDragDropped(event -> {
			Dragboard dragBoard = event.getDragboard();
			List<Integer> selectedTracks = (List<Integer>) dragBoard.getContent(TRACK_ID_MIME_TYPE);
			player.addTracksToPlayQueue(selectedTracks, false);
		});
	}

	private void playPause() {
		LOG.trace("Play/pause button clicked");
		if (playButton.isSelected()) {
			if (player.getCurrentTrack().isPresent())
				player.resume();
			else
				player.play(true);
		}
		else
			player.pause();
	}

	public StringProperty searchTextProperty() {
		return searchTextField.textProperty();
	}

	public ReadOnlyBooleanProperty previousButtonDisabledProperty() {
		return prevButton.disabledProperty();
	}

	public ReadOnlyBooleanProperty nextButtonDisabledProperty() {
		return nextButton.disabledProperty();
	}

	public BooleanProperty playButtonSelectedProperty() {
		return playButton.selectedProperty();
	}

	public DoubleProperty trackSliderMaxProperty() {
		return trackSlider.maxProperty();
	}

	public BooleanProperty trackSliderValueChangingProperty() {
		return trackSlider.valueChangingProperty();
	}

	public DoubleProperty trackSliderValueProperty() {
		return trackSlider.valueProperty();
	}

	public DoubleProperty trackProgressBarProgressProperty() {
		return trackProgressBar.progressProperty();
	}

	public DoubleProperty volumeSliderValueProperty() {
		return volumeSlider.valueProperty();
	}

	public void setWaveform(Track track) {
		mainWaveformPanel.setTrack(track);
	}

	/**
	 * Sets the play queue pane node that contains the play queue and history queue lists.
	 *
	 * @param pane
	 */
	public void setPlayQueuePane(AnchorPane pane) {
		playQueuePane = pane;
		playQueuePane.setVisible(false);
		playQueuePane.visibleProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue)
				playQueueButton.setSelected(true);
			else
				playQueueButton.setSelected(false);
		});
		StackPane.setMargin(playQueuePane, new Insets(0, 0, 480, 0));
	}

	public void hidePlayQueue() {
		if (playQueueStackPane.getChildren().contains(playQueuePane)) {
			playQueueStackPane.getChildren().remove(playQueuePane);
			playQueuePane.setVisible(false);
		}
	}

	public void showPlayQueue() {
		if (! playQueueStackPane.getChildren().contains(playQueuePane)) {
			playQueueStackPane.getChildren().add(0, playQueuePane);
			playQueuePane.setVisible(true);
		}
	}

	public void setStopped() {
		playButton.setSelected(false);
		trackSlider.setDisable(true);
		nextButton.setDisable(true);
		prevButton.setDisable(true);
		songTitleLabel.textProperty().unbind();
		songTitleLabel.setText("");
		artistAlbumLabel.textProperty().unbind();
		artistAlbumLabel.setText("");
		currentCover.setVisible(false);
		currentTimeLabel.setText("");
		remainingTimeLabel.setText("");
		SwingUtilities.invokeLater(() -> mainWaveformPanel.clear());
	}

	public void setPlaying() {
		playButton.setSelected(true);
		trackSlider.setDisable(false);
		nextButton.setDisable(false);
		prevButton.setDisable(false);
		currentCover.setVisible(true);
	}

	public void increaseVolume() {
		player.increaseVolume(VOLUME_AMOUNT);
		volumeSlider.setValue(volumeSlider.getValue() + VOLUME_AMOUNT);
		LOG.trace("Volume increased " + volumeSlider.getValue());
	}

	public void decreaseVolume() {
		player.decreaseVolume(VOLUME_AMOUNT);
		volumeSlider.setValue(volumeSlider.getValue() - VOLUME_AMOUNT);
		LOG.trace("Volume decreased " + volumeSlider.getValue());
	}

	/**
	 * Updates the components of the player pane such as the song title label, the artist label,
	 * the cover image, or the waveform image; with the given current {@link Track}.
	 *
	 * @param currentTrack The <tt>Track</tt> that is being currently playing.
	 */
	public void updatePlayer(Track currentTrack) {
		LOG.debug("Setting up player and view for track {}", currentTrack);
		String fileFormat = currentTrack.getFileFormat();
		if (musicLibrary.containsWaveform(currentTrack.getTrackId()))
			setWaveform(currentTrack);
		else if ("wav".equals(fileFormat) || "mp3".equals(fileFormat) || "m4a".equals(fileFormat))
			TaskDemon.getInstance().analyzeTrackWaveform(currentTrack);

		SwingUtilities.invokeLater(() -> mainWaveformPanel.setTrack(currentTrack));
		songTitleLabel.textProperty().bind(currentTrack.nameProperty());
		artistAlbumLabel.textProperty().bind(Bindings.createStringBinding(
				() -> currentTrack.artistProperty().get() + " - " + currentTrack.albumProperty().get(),
				currentTrack.artistProperty(), currentTrack.albumProperty()));
		if (currentTrack.getCoverImage().isPresent()) {
			byte[] coverBytes = currentTrack.getCoverImage().get();
			Image image = new Image(new ByteArrayInputStream(coverBytes));
			currentCover.setImage(image);
		}
		else
			currentCover.setImage(COVER_IMAGE);

		trackSlider.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			Double endTime = trackSlider.getMax();
			if (! endTime.equals(Double.POSITIVE_INFINITY) || ! endTime.equals(Double.NaN)) {
				trackProgressBar.setProgress(trackSlider.getValue() / endTime);
				player.seek(trackSlider.getValue());
			}
		});
	}

	/**
	 * Updates the current time label and the remaining time label of the
	 * {@link Track} that is currently being played.
	 *
	 * @param elapsed The elapsed time of the track
	 * @param total   The total time of the track
	 */
	public void updateTrackLabels(Duration elapsed, Duration total) {
		int currentHours = (int) elapsed.toHours();
		int currentMins = (int) elapsed.subtract(Duration.hours(currentHours)).toMinutes();
		int currentSecs = (int) elapsed.subtract(Duration.minutes(currentMins)).subtract(Duration.hours(currentHours))
									   .toSeconds();

		String currentTimeText = getFormattedTimeString(currentHours, currentMins, currentSecs, (int) total.toHours());
		currentTimeLabel.setText(currentTimeText);

		Duration remaining = total.subtract(elapsed);
		int remainingHours = (int) remaining.toHours();
		int remainingMins = (int) remaining.subtract(Duration.hours(remainingHours)).toMinutes();
		int remainingSecs = (int) remaining.subtract(Duration.minutes(remainingMins))
										   .subtract(Duration.hours(remainingHours)).toSeconds();

		String remainingTimeText = getFormattedTimeString(remainingHours, remainingMins, remainingSecs,
														  (int) total.toHours());
		remainingTimeLabel.setText(remainingTimeText);
	}

	private String getFormattedTimeString(int currentHours, int currentMins, int currentSecs, int totalHours) {
		String formattedTime = "";
		if (totalHours > 0)
			formattedTime += Integer.toString(currentHours) + ":";

		if (currentMins < 10)
			formattedTime += "0" + currentMins;
		else
			formattedTime += Integer.toString(currentMins);
		formattedTime += ":";
		if (currentSecs < 10)
			formattedTime += "0" + Integer.toString(currentSecs);
		else
			formattedTime += Integer.toString(currentSecs);
		return formattedTime;
	}
}
