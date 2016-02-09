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

package com.musicott.view;

import java.io.ByteArrayInputStream;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musicott.model.MusicLibrary;
import com.musicott.model.Track;
import com.musicott.player.FlacPlayer;
import com.musicott.player.NativePlayer;
import com.musicott.player.PlayerFacade;
import com.musicott.player.TrackPlayer;
import com.musicott.services.ServiceManager;
import com.musicott.task.TaskPoolManager;
import com.musicott.view.custom.WaveformPanel;

import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingNode;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

/**
 * @author Octavio Calleya
 *
 */
public class PlayerController {

	private final Logger LOG = LoggerFactory.getLogger(getClass().getName());
	public final Image DEFAULT_COVER_IMAGE = new Image(getClass().getResourceAsStream("/images/default-cover-image.png"));
	private static final double VOLUME_AMOUNT = 0.05;
	
	@FXML
	private GridPane playerGridPane;
	@FXML
	private ToggleButton playButton, playQueueButton;
	@FXML
	private Button prevButton, nextButton;
	@FXML
	private ImageView currentCover;
	@FXML
	private SwingNode waveformSwingNode;
	@FXML
	private StackPane playerStackPane, playQueueStackPane;
	@FXML
	private Label songTitleLabel, artistAlbumLabel, currentTimeLabel, remainingTimeLabel; 
	@FXML
	private Slider trackSlider, volumeSlider;
	@FXML
	private ProgressBar trackProgressBar;
	@FXML
	private TextField searchTextField;
	@FXML
	private ProgressBar volumeProgressBar;
	private AnchorPane playQueuePane;
	private WaveformPanel mainWaveformPane;
	
	private MusicLibrary ml = MusicLibrary.getInstance();
	private ServiceManager services = ServiceManager.getInstance();
	private PlayerFacade player = PlayerFacade.getInstance();
	
	public PlayerController() {}
	
	@FXML
	public void initialize() {
		playButton.disableProperty().bind(ml.trackslistProperty().emptyProperty());
		prevButton.setOnAction(e -> player.previous());
		nextButton.setOnAction(e -> player.next());
		volumeSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {if(!isChanging) volumeProgressBar.setProgress(volumeSlider.getValue());});
		volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> volumeProgressBar.setProgress(newValue.doubleValue()));

		SwingUtilities.invokeLater(() -> {
			mainWaveformPane = new WaveformPanel(520, 50);
            waveformSwingNode.setContent(mainWaveformPane);
		});
		playerStackPane.getChildren().add(0, waveformSwingNode);
	}
	
	public void setWaveform(Track track) {
		mainWaveformPane.setTrack(track);
	}
	
	public void setPlayQueuePane(AnchorPane pane) {
		playQueuePane = pane;
		StackPane.setMargin(playQueuePane, new Insets(0, 0, 480, 0));
		EventHandler<Event> hidePlayQueueAction = e -> showPlayQueue(false);
		playerGridPane.setOnMouseClicked(hidePlayQueueAction);
		playButton.setOnMouseClicked(hidePlayQueueAction);
		playQueuePane.setVisible(false);
		
		playQueuePane.visibleProperty().addListener((obs, oldVal, newVal) -> {
			if(newVal.booleanValue())
				playQueueButton.setSelected(true);
			else
				playQueueButton.setSelected(false);
		});
	}
	
	public void showPlayQueue(boolean show) {
		if(show && !playQueueStackPane.getChildren().contains(playQueuePane)) {
			playQueueStackPane.getChildren().add(0, playQueuePane);
			playQueuePane.setVisible(true);
		} else if(!show && playQueueStackPane.getChildren().contains(playQueuePane)) {
			playQueueStackPane.getChildren().remove(playQueuePane);
			playQueuePane.setVisible(false);
		}
	}
	
	@FXML
	private void doShowHidePlayQueue() {
		if(playQueuePane.isVisible())
			showPlayQueue(false);
		else
			showPlayQueue(true);
	}
	
	@FXML
	private void doPlayPause() {
		LOG.trace("Play/pause button clicked");
		if(playButton.isSelected()) {	// play
			if(player.getCurrentTrack() != null)
				player.resume();
			else
				player.play(true);
		}
		else							// pause
			player.pause();
	}
	
	private void setPlaying() {
		playButton.setSelected(true);
		trackSlider.setDisable(false);
		nextButton.setDisable(false);
		prevButton.setDisable(false);
		currentCover.setVisible(true);
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
		SwingUtilities.invokeLater(() -> mainWaveformPane.clear());
		if(player.getTrackPlayer() instanceof NativePlayer)
			volumeSlider.valueProperty().unbindBidirectional(((NativePlayer)player.getTrackPlayer()).getMediaPlayer().volumeProperty());
		else if (player.getTrackPlayer() instanceof FlacPlayer) {
			
		}
	}
	
	public void doIncreaseVolume() {
		if(player != null)
			player.increaseVolume(VOLUME_AMOUNT);
		volumeSlider.setValue(volumeSlider.getValue()+VOLUME_AMOUNT);
		LOG.trace("Volume increased "+volumeSlider.getValue());
	}
	
	public void doDecreaseVolume() {
		if(player != null)
			player.decreaseVolume(VOLUME_AMOUNT);
		volumeSlider.setValue(volumeSlider.getValue()-VOLUME_AMOUNT);
		LOG.trace("Volume decreased "+volumeSlider.getValue());
	}
	
	public void updatePlayerInfo(TrackPlayer currentPlayer, Track currentTrack) {
		// Set up the player and the view related to it
		LOG.debug("Setting up player and view for track {}", currentTrack);
		if(ml.containsWaveform(currentTrack.getTrackID()))
			setWaveform(currentTrack);
		else if (currentTrack.getFileFormat().equals("wav") || currentTrack.getFileFormat().equals("mp3") || currentTrack.getFileFormat().equals("m4a")) 
			TaskPoolManager.getInstance().addTrackToProcess(currentTrack);
		if(currentPlayer instanceof NativePlayer)
			setUpPlayer(((NativePlayer) currentPlayer).getMediaPlayer());
		else if(currentPlayer instanceof FlacPlayer)
			setUpPlayer((FlacPlayer) currentPlayer);
		
		SwingUtilities.invokeLater(() -> mainWaveformPane.setTrack(currentTrack));
		songTitleLabel.textProperty().bind(currentTrack.nameProperty());;
		artistAlbumLabel.textProperty().bind(Bindings.createStringBinding(
				() -> currentTrack.artistProperty().get()+" - "+currentTrack.albumProperty().get(), currentTrack.artistProperty(), currentTrack.albumProperty())
		);
		if(currentTrack.hasCover())
			currentCover.setImage(new Image(new ByteArrayInputStream(currentTrack.getCoverBytes())));
		else
			currentCover.setImage(DEFAULT_COVER_IMAGE);
	//	currentTrack.getHasCoverProperty().addListener(observable -> currentCover.setImage(new Image(new ByteArrayInputStream(currentTrack.getCoverBytes()))));
	}
	
	private void setUpPlayer(MediaPlayer mediaPlayer) {
		trackSlider.valueProperty().addListener((observable) -> {
			double endTime = mediaPlayer.getStopTime().toMillis();
			if(trackSlider.isValueChanging() && (!(endTime == Double.POSITIVE_INFINITY) || !(endTime == Double.NaN))) {
				trackProgressBar.setProgress(trackSlider.getValue() / endTime);
				mediaPlayer.seek(Duration.millis(trackSlider.getValue()));
			}
		});
		trackSlider.addEventHandler(MouseEvent.MOUSE_CLICKED, (event) -> {
			double endTime = mediaPlayer.getStopTime().toMillis();
			if(!(endTime == Double.POSITIVE_INFINITY) || !(endTime == Double.NaN)) {
			trackProgressBar.setProgress(trackSlider.getValue() / endTime);
			mediaPlayer.seek(Duration.millis(trackSlider.getValue()));
			}
		});
		mediaPlayer.totalDurationProperty().addListener((observable, oldDuration, newDuration) -> trackSlider.setMax(newDuration.toMillis()));
		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> {if (!trackSlider.isValueChanging()) trackSlider.setValue(newTime.toMillis());});
		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> trackProgressBar.setProgress(newTime.toMillis() / mediaPlayer.getStopTime().toMillis()));
		mediaPlayer.volumeProperty().bindBidirectional(volumeSlider.valueProperty());
		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> formatTime(newTime,mediaPlayer.getMedia().getDuration()));	
		mediaPlayer.statusProperty().addListener((observable, oldStatus, newStatus) -> {
			if(newStatus == Status.PLAYING) {
				setPlaying();
			}
			else if (newStatus == Status.PAUSED) {
				playButton.setSelected(false);
			}
			else if (newStatus == Status.STOPPED) {
				setStopped();
			}
		});
		mediaPlayer.currentTimeProperty().addListener((observable, oldTime, newTime) -> {
			if(newTime.greaterThanOrEqualTo(mediaPlayer.getStopTime().divide(2.0)))
				player.incrementCurentTrackPlayCount();
			if(!player.isCurrentTrackScrobbled() && mediaPlayer.getTotalDuration().greaterThanOrEqualTo(Duration.seconds(30)) &&
			  (newTime.greaterThanOrEqualTo(mediaPlayer.getStopTime().divide(2.0)) || newTime.greaterThanOrEqualTo(Duration.minutes(4)))) {
				
				player.setCurrentTrackScrobbled(true);
				services.udpateAndScrobbleLastFM(player.getCurrentTrack());
			}
		});
	}
	
	private void setUpPlayer(FlacPlayer mediaPlayer) {
		
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
}