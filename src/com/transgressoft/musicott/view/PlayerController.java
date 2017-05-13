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
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package com.transgressoft.musicott.view;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.tasks.*;
import com.transgressoft.musicott.util.*;
import com.transgressoft.musicott.util.factories.*;
import com.transgressoft.musicott.view.custom.*;
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
import java.util.*;

import static com.transgressoft.musicott.view.custom.TrackTableRow.*;
import static org.fxmisc.easybind.EasyBind.*;

/**
 * Controller class of the bottom pane that includes the player and the search field.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
@Singleton
public class PlayerController implements MusicottController, ConfigurableController {

    private static final double VOLUME_AMOUNT = 0.05;
    private static final String PLAYQUEUE_BUTTON_STYLE =
            "-fx-effect: dropshadow(one-pass-box, rgb(99, 255, 109), 3, 0.2, 0, 0);";

    private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

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

    @FXML
    private PlayQueueController playQueueLayoutController;
    @FXML
    private AnchorPane playQueueLayout;
    private WaveformPanel mainWaveformPanel;

    private TracksLibrary tracksLibrary;
    private WaveformsLibrary waveformsLibrary;
    private PlayerFacade player;
    private TaskDemon taskDemon;
    private WaveformPaneFactory waveformPaneFactory;
    private ReadOnlyBooleanProperty emptyLibraryProperty;

    @Inject
    public PlayerController(TracksLibrary tracksLibrary, WaveformsLibrary waveformsLibrary, PlayerFacade player,
            TaskDemon taskDemon, WaveformPaneFactory waveformPaneFactory) {
        this.tracksLibrary = tracksLibrary;
        this.waveformsLibrary = waveformsLibrary;
        this.player = player;
        this.taskDemon = taskDemon;
        this.waveformPaneFactory = waveformPaneFactory;
    }

    @FXML
    public void initialize() {
        playButton.disableProperty().bind(emptyLibraryProperty);
        playButton.setOnAction(event -> playPause());
        prevButton.setOnAction(e -> player.previous());
        nextButton.setOnAction(e -> player.next());
        subscribe(volumeSlider.valueChangingProperty(), changing -> {
            if (! changing)
                volumeProgressBar.setProgress(volumeSlider.getValue());});
        subscribe(volumeSlider.valueProperty(), p -> volumeProgressBar.setProgress(p.doubleValue()));

        playerStackPane.getChildren().add(0, waveformSwingNode);

        playQueueButton.setOnAction(event -> {
            if (playQueueLayout.isVisible())
                hidePlayQueue();
            else
                showPlayQueue();
        });

        playerGridPane.setOnMouseClicked(event -> hidePlayQueue());
        playButton.setOnMouseClicked(event -> hidePlayQueue());

        playQueueButton.setOnDragDropped(this::onDragDroppedOnPlayQueueButton);
        playQueueButton.setOnDragOver(this::onDragOverOnPlayQueueButton);
        playQueueButton.setOnDragExited(this::onDragExitedOnPlayQueueButton);

        SwingUtilities.invokeLater(() -> {
            mainWaveformPanel = waveformPaneFactory.create(520, 50);
            waveformSwingNode.setContent(mainWaveformPanel);
        });
    }

    @Override
    public void configure() {
        subscribe(playQueueLayout.visibleProperty(), playQueueButton::setSelected);
        StackPane.setMargin(playQueueLayout, new Insets(0, 0, 480, 0));
        player.setPlayerController(this);
        player.setPlayQueueController(playQueueLayoutController);
        hidePlayQueue();
    }

    private void onDragDroppedOnPlayQueueButton(DragEvent event) {
        Dragboard dragBoard = event.getDragboard();
        List<Integer> selectedTracksIds = (List<Integer>) dragBoard.getContent(TRACK_IDS_MIME_TYPE);
        List<Track> selectedTracks = tracksLibrary.getTracks(selectedTracksIds);
        player.addTracksToPlayQueue(selectedTracks, false);
        event.consume();
    }

    private void onDragOverOnPlayQueueButton(DragEvent event) {
        event.acceptTransferModes(TransferMode.COPY);
        playQueueButton.setStyle(PLAYQUEUE_BUTTON_STYLE);
        playQueueButton.setOpacity(0.9);
        event.consume();
    }

    private void onDragExitedOnPlayQueueButton(DragEvent event) {
        playQueueButton.setStyle("");
        playQueueButton.setOpacity(1.0);
        event.consume();
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

    public void hidePlayQueue() {
        if (playQueueStackPane.getChildren().contains(playQueueLayout)) {
            playQueueStackPane.getChildren().remove(playQueueLayout);
            playQueueLayout.setVisible(false);
        }
    }

    public void showPlayQueue() {
        if (! playQueueStackPane.getChildren().contains(playQueueLayout)) {
            playQueueStackPane.getChildren().add(0, playQueueLayout);
            playQueueLayout.setVisible(true);
        }
    }

    public void focusSearchField() {
        searchTextField.requestFocus();
    }

    @Inject
    public void setEmptyLibraryProperty(ReadOnlyBooleanProperty emptyLibraryProperty) {
        this.emptyLibraryProperty = emptyLibraryProperty;
    }

    public StringProperty searchTextProperty() {
        return searchTextField.textProperty();
    }

    public ReadOnlyBooleanProperty searchFieldFocusedProperty() {
        return searchTextField.focusedProperty();
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
        SwingUtilities.invokeLater(mainWaveformPanel::clear);
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
    }

    public void decreaseVolume() {
        player.decreaseVolume(VOLUME_AMOUNT);
        volumeSlider.setValue(volumeSlider.getValue() - VOLUME_AMOUNT);
    }

    /**
     * Updates the components of the player pane such as the song title label, the artist label,
     * the cover image, or the waveform image; with the given current {@link Track}.
     *
     * @param currentTrack The {@code Track} that is being currently playing.
     */
    public void updatePlayer(Track currentTrack) {
        LOG.debug("Setting up player and view for track {}", currentTrack);
        String fileFormat = currentTrack.getFileFormat();
        if (waveformsLibrary.containsWaveform(currentTrack.getTrackId()))
            setWaveform(currentTrack);
        else if ("wav".equals(fileFormat) || "mp3".equals(fileFormat) || "m4a".equals(fileFormat))
            taskDemon.analyzeTrackWaveform(currentTrack);

        SwingUtilities.invokeLater(() -> mainWaveformPanel.setTrack(currentTrack));
        songTitleLabel.textProperty().bind(currentTrack.nameProperty());
        artistAlbumLabel.textProperty().bind(
                combine(currentTrack.artistProperty(), currentTrack.albumProperty(), (art, alb) -> art + " - " + alb));

        Utils.updateCoverImage(currentTrack, currentCover);
        trackSlider.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Double endTime = trackSlider.getMax();
            if (! endTime.equals(Double.POSITIVE_INFINITY) || ! endTime.equals(Double.NaN)) {
                trackProgressBar.setProgress(trackSlider.getValue() / endTime);
                player.seek(Duration.millis(trackSlider.getValue()));
            }
        });
    }

    public void setWaveform(Track track) {
        mainWaveformPanel.setTrack(track);
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
        int currentSecs = (int) elapsed.subtract(Duration.minutes(currentMins))
                                       .subtract(Duration.hours(currentHours))
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
