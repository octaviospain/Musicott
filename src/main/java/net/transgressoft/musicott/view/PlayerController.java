package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.WaveformPane;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.player.JavaFxPlayer;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.music.player.AudioItemPlayer.Status;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.services.lastfm.LastFmService;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.table.AudioItemTableViewBase;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static net.transgressoft.commons.music.player.AudioItemPlayer.Status.*;
import static org.fxmisc.easybind.EasyBind.combine;
import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * @author Octavio Calleya
 */
@FxmlView("/fxml/PlayerController.fxml")
@Controller
public class PlayerController {

    private static final double VOLUME_AMOUNT = 0.05;
    private static final String PLAY_QUEUE_BUTTON_STYLE = "-fx-effect: dropshadow(one-pass-box, rgb(99, 255, 109), 3, 0.2, 0, 0);";

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository;
    private final PlayerCore playerCore;
    private final Image defaultCoverImage = ApplicationImage.DEFAULT_COVER.get();

    private final ObservableList<TrackQueueRow> playQueueList;
    private final ObservableList<TrackQueueRow> historyQueueList;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

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
    private AnchorPane waveformAnchorPane;
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
    private ProgressBar volumeProgressBar;

    @FXML
    private AnchorPane playQueueLayout;
    @Autowired
    private FxControllerAndView<PlayQueueController, AnchorPane> playQueueControllerFxControllerAndView;

    private LastFmService lastFmService;
    private WaveformPane waveformPane;
    private ReadOnlyBooleanProperty emptyLibraryProperty;

    @Autowired
    public PlayerController(AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository) {
        this.waveformRepository = waveformRepository;
        this.playQueueList = FXCollections.observableArrayList();
        this.historyQueueList = FXCollections.observableArrayList();
        this.playerCore = new PlayerCore(playQueueList, historyQueueList);
    }

    @FXML
    public void initialize() {
        PlayQueueController controller = playQueueControllerFxControllerAndView.getController();
        controller.setHistoryQueueList(historyQueueList);
        controller.setPlayQueueList(playQueueList);

        playButton.disableProperty().bind(emptyLibraryProperty);
        playButton.setOnAction(event -> playPause());
        prevButton.setOnAction(e -> previous());
        nextButton.setOnAction(e -> next());
        subscribe(volumeSlider.valueChangingProperty(), changing -> {
            if (!changing)
                volumeProgressBar.setProgress(volumeSlider.getValue());
        });
        subscribe(volumeSlider.valueProperty(), p -> volumeProgressBar.setProgress(p.doubleValue()));

        playerStackPane.getChildren().add(0, waveformAnchorPane);
        waveformPane = new WaveformPane();
        waveformAnchorPane.getChildren().add(waveformPane);
        AnchorPane.setBottomAnchor(waveformPane, 0.0);
        AnchorPane.setTopAnchor(waveformPane, 0.0);
        AnchorPane.setLeftAnchor(waveformPane, 0.0);
        AnchorPane.setRightAnchor(waveformPane, 0.0);
        waveformPane.widthProperty().bind(waveformAnchorPane.widthProperty());
        waveformPane.heightProperty().bind(waveformAnchorPane.heightProperty());

        playQueueButton.setOnAction(event -> {
            if (playQueueLayout.isVisible())
                hidePlayQueue();
            else
                showPlayQueue();
        });
        playQueueLayout.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue)
                hidePlayQueue();
        });
        playQueueLayout.setOnMouseExited(event -> hidePlayQueue());

        playerGridPane.setOnMouseClicked(event -> hidePlayQueue());
        playButton.setOnMouseClicked(event -> hidePlayQueue());

        playQueueButton.setOnDragDropped(this::onDragDroppedOnPlayQueueButton);
        playQueueButton.setOnDragOver(this::onDragOverOnPlayQueueButton);
        playQueueButton.setOnDragExited(this::onDragExitedOnPlayQueueButton);

        subscribe(playQueueLayout.visibleProperty(), playQueueButton::setSelected);
        StackPane.setMargin(playQueueLayout, new Insets(0, 0, 480, 0));
        hidePlayQueue();
    }

    @SuppressWarnings("unchecked")
    private void onDragDroppedOnPlayQueueButton(DragEvent event) {
        var dragBoard = event.getDragboard();
        var selectedTracksIds = (List<Integer>) dragBoard.getContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT);
        //        List<Track> selectedTracks = tracksLibrary.getTracks(selectedTracksIds);  //TODO Fix
        //        player.addTracksToPlayQueue(selectedTracks, false);
        event.consume();
    }

    private void onDragOverOnPlayQueueButton(DragEvent event) {
        event.acceptTransferModes(TransferMode.COPY);
        playQueueButton.setStyle(PLAY_QUEUE_BUTTON_STYLE);
        playQueueButton.setOpacity(0.10);
        event.consume();
    }

    private void onDragExitedOnPlayQueueButton(DragEvent event) {
        playQueueButton.setStyle("");
        playQueueButton.setOpacity(1.0);
        event.consume();
    }

    public void playPause() {
        logger.trace("Play/pause button clicked");
        if (playButton.isSelected()) {
            if (playerCore.currentTrack().isPresent()) {
                playerCore.resume();
            } else {
                playerCore.playRandom();
            }
        } else
            playerCore.pause();
    }

    public void previous() {
        playerCore.previous();
    }

    public void next() {
        playerCore.next();
    }

    public Optional<ObservableAudioItem> currentTrack() {
        return playerCore.currentTrack();
    }

    public void hidePlayQueue() {
        if (playQueueStackPane.getChildren().contains(playQueueLayout)) {
            playQueueStackPane.getChildren().remove(playQueueLayout);
            playQueueLayout.setVisible(false);
        }
    }

    private void showPlayQueue() {
        if (!playQueueStackPane.getChildren().contains(playQueueLayout)) {
            playQueueStackPane.getChildren().add(0, playQueueLayout);
            playQueueLayout.setVisible(true);
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
        waveformPane.getGraphicsContext2D().clearRect(0, 0, waveformPane.getWidth(), waveformPane.getHeight());
    }

    public void setPlaying() {
        playButton.setSelected(true);
        trackSlider.setDisable(false);
        nextButton.setDisable(false);
        prevButton.setDisable(false);
        currentCover.setVisible(true);
    }

    public void increaseVolume() {
        playerCore.increaseVolume();
        volumeSlider.setValue(volumeSlider.getValue() + VOLUME_AMOUNT);
    }

    public void decreaseVolume() {
        playerCore.decreaseVolume();
        volumeSlider.setValue(volumeSlider.getValue() - VOLUME_AMOUNT);
    }

    /**
     * Updates the components of the player pane such as the song title label, the artist label,
     * the cover image, or the waveform image; with the given current {@link ObservableAudioItem}.
     */
    public void updatePlayerComponents(ObservableAudioItem currentTrack) {
        logger.debug("Setting up player and view for track {}", currentTrack);
        Color backgroundColor = Color.color(73, 73, 73);
        Color waveformColor = Color.color(34, 34, 34);

        // Show default waveform animation while computing the waveform
        // and then
        waveformRepository.getOrCreateWaveformAsync(currentTrack, (short) waveformAnchorPane.getWidth(), (short) waveformAnchorPane.getHeight())
                .thenAccept(waveform -> Platform.runLater(() -> waveformPane.drawWaveformAsync(waveform, waveformColor, backgroundColor)));

        songTitleLabel.textProperty().bind(currentTrack.getTitleProperty());
        artistAlbumLabel.textProperty().bind(
                combine(currentTrack.getArtistProperty(), currentTrack.getAlbumProperty(), (art, alb) -> art.getName() + " - " + alb.getName()));

        currentTrack.getCoverImageProperty().get().ifPresentOrElse(currentCover::setImage, () -> currentCover.setImage(defaultCoverImage));
        trackSlider.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            Double endTime = trackSlider.getMax();
            if (!endTime.equals(Double.POSITIVE_INFINITY) || !endTime.equals(Double.NaN)) {
                trackProgressBar.setProgress(trackSlider.getValue() / endTime);
                playerCore.seek(Duration.millis(trackSlider.getValue()));
            }
        });
    }

    public void playFromQueue(TrackQueueRow trackQueueRow) {
        playerCore.playFromQueue(trackQueueRow);
    }

    public void playFromHistoryQueue(TrackQueueRow trackQueueRow) {
        playerCore.playFromHistoryQueue(trackQueueRow);
    }

    /**
     * Updates the current time label and the remaining time label of an AudioItem that is currently being played.
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
                                                          (int) total.toHours()
        );
        remainingTimeLabel.setText(remainingTimeText);
    }

    private String getFormattedTimeString(int currentHours, int currentMins, int currentSecs, int totalHours) {
        var formattedTime = "";
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

    @Value("#{audioRepository.emptyLibraryProperty()}")
    public void setEmptyLibraryProperty(ReadOnlyBooleanProperty emptyLibraryProperty) {
        this.emptyLibraryProperty = emptyLibraryProperty;
    }

    @Autowired(required = false)
    public void setLastFmService(LastFmService lastFmService) {
        this.lastFmService = lastFmService;
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

    @EventListener(classes = PauseEvent.class)
    public void pauseEventListener() {
        playerCore.pause();
    }

    @EventListener
    public void playEventListener(PlayItemEvent playItemEvent) {
        List<ObservableAudioItem> audioItems = playItemEvent.audioItems;
        playerCore.addToQueue(audioItems);
    }

    @EventListener
    public void playPlaylistRandomlyEventListener(PlayPlaylistRandomlyEvent playPlaylistRandomlyEvent) {
        ObservablePlaylist playlist = playPlaylistRandomlyEvent.playlist;
        playerCore.addToQueue(playlist.getAudioItemsProperty());
    }

    @EventListener
    public void addAudioItemsToPlayQueueEventListener(AddToPlayQueueEvent addToPlayQueueEvent) {
        playerCore.addToQueue(addToPlayQueueEvent.audioItems);
    }

    // Move object
    private class PlayerCore {

        private final Logger LOG = LoggerFactory.getLogger(getClass().getName());

        private final ObservableList<TrackQueueRow> playQueueList;
        private final ObservableList<TrackQueueRow> historyQueueList;

        private Optional<ObservableAudioItem> currentTrack = Optional.empty();
        private JavaFxPlayer trackPlayer;
        private boolean playingRandom = false;
        private boolean played = false;
        private boolean scrobbled = false;

        public PlayerCore(ObservableList<TrackQueueRow> playQueueList, ObservableList<TrackQueueRow> historyQueueList) {
            this.playQueueList = playQueueList;
            this.historyQueueList = historyQueueList;
        }

        public void play(ObservableAudioItem audioItem) {
            if (!audioItem.getPath().toFile().exists()) {
                applicationEventPublisher.publishEvent(new ErrorEvent("File not found", audioItem.getPath().toString(), this));
                // and remove from queue
            } else {
                if (trackPlayer.status().equals(STOPPED) ||
                        trackPlayer.status().equals(PAUSED) ||
                        trackPlayer.status().equals(UNKNOWN)) {
                    // if nothing is currently being played
                    setPlayer(audioItem);
                } else if (trackPlayer.status().equals(PLAYING)) {
                    // if something is being played
                    // TODO ask user if stop current and play or add to top of the queue

                    stop();
                    setPlayer(audioItem);
                }
            }
        }

        private void setPlayer(ObservableAudioItem audioItem) {
            scrobbled = false;
            played = false;
            trackPlayer = new JavaFxPlayer();
            trackPlayer.play(audioItem);
            currentTrack = Optional.of(audioItem);
            updatePlayerComponents(audioItem);
            bindMediaPlayer();
            LOG.debug("Created new player");
        }

        private void bindMediaPlayer() {
            trackPlayer.getVolumeProperty().bindBidirectional(volumeSlider.valueProperty());

            bindPlayerConfiguration(trackPlayer);

            subscribe(trackPlayer.getStatusProperty(), status -> {
                if (PLAYING == status)
                    setPlaying();
                else if (PAUSED == status)
                    playButtonSelectedProperty().setValue(false);
                else if (STOPPED == status) {
                    setStopped();
                    volumeSlider.valueProperty().unbindBidirectional(trackPlayer.getVolumeProperty());
                }
            });
        }

        private void bindPlayerConfiguration(JavaFxPlayer trackPlayer) {
            subscribe(trackPlayer.getCurrentTimeProperty(), time -> {
                Duration halfTime = trackPlayer.getTotalDuration().divide(2.0);
                if (time.greaterThanOrEqualTo(halfTime))
                    currentTrack.get().getPlayCountProperty().add(1);
                if (isCurrentTrackValidToScrobble(trackPlayer, time)) {
                    scrobbled = true;
                    if (lastFmService != null) {
                        lastFmService.updateNowPlaying(currentTrack.get());
                        lastFmService.scrobble(currentTrack.get());
                    }
                }
            });

            DoubleProperty trackSliderMaxProperty = trackSlider.maxProperty();
            currentTrack.ifPresent(track -> trackSliderMaxProperty.setValue(track.getDuration().toMillis()));

            BooleanProperty trackSliderValueChangingProperty = trackSlider.valueChangingProperty();
            DoubleProperty trackSliderValueProperty = trackSlider.valueProperty();
            subscribe(trackPlayer.getCurrentTimeProperty(), time -> {
                if (!trackSliderValueChangingProperty.get())
                    trackSliderValueProperty.setValue(time.toMillis());
            });

            DoubleProperty trackProgressBarProgressProperty = trackProgressBar.progressProperty();
            subscribe(trackSliderValueProperty, value -> {
                Double endTime = trackPlayer.getTotalDuration().toMillis();
                if (trackSliderValueChangingProperty.get() && (!endTime.equals(Double.POSITIVE_INFINITY) || !endTime.equals(Double.NaN))) {
                    trackProgressBarProgressProperty.set(value.doubleValue() / endTime);
                    trackPlayer.seek(value.doubleValue());
                }
            });

            subscribe(trackPlayer.getCurrentTimeProperty(),
                      t -> trackProgressBarProgressProperty.set(t.toMillis() / trackSliderMaxProperty.get())
            );
            subscribe(trackPlayer.getCurrentTimeProperty(),
                      t -> updateTrackLabels(t, trackPlayer.getTotalDuration()) // or trackPlayer.getDuration() ?
            );
        }

        private boolean isCurrentTrackValidToScrobble(JavaFxPlayer trackPlayer, Duration newTime) {
            boolean isDurationBeyond30Seconds = trackPlayer.getTotalDuration().greaterThanOrEqualTo(Duration.seconds(30));
            boolean isDurationBeyondMidTime = newTime.greaterThanOrEqualTo(trackPlayer.getTotalDuration().divide(2.0));
            boolean isDurationLongerThan4Minutes = newTime.greaterThanOrEqualTo(Duration.minutes(4));

            return !scrobbled && isDurationBeyond30Seconds && (isDurationBeyondMidTime || isDurationLongerThan4Minutes);
        }

        public void playRandom() {
            // TODO
        }

        public void playFromQueue(TrackQueueRow trackQueueRow) {
            ObservableAudioItem track = trackQueueRow.getTrack();
            setPlayer(track);
            historyQueueList.add(0, trackQueueRow);
            playQueueList.remove(trackQueueRow);
            LOG.debug("Play from queue selected. Queue size {}, history queue size {}", playQueueList.size(), historyQueueList.size());
        }

        public void playFromHistoryQueue(TrackQueueRow trackQueueRow) {
            ObservableAudioItem track = trackQueueRow.getTrack();
            setPlayer(track);
            historyQueueList.remove(trackQueueRow);
            LOG.debug("Play from history selected. History queue size {}", historyQueueList.size());
        }

        public void pause() {
            switch (playerStatus()) {
                case PLAYING:
                    trackPlayer.pause();
                    LOG.info("Player paused");
                    break;
                case PAUSED:
                    resume();
                    break;
                case STOPPED:
                    playRandom();
                    break;
                default:
            }
        }

        public void resume() {
            trackPlayer.resume();
            LOG.info("Player resumed");
        }

        private void stop() {
            trackPlayer.stop();
            currentTrack = Optional.empty();
            LOG.info("Player topped");
        }

        public void previous() {
            if (!historyQueueList.isEmpty()) {
                setPlayer(historyQueueList.get(0).getTrack());
                historyQueueList.remove(0);
            } else
                stop();
        }

        public void next() {
            if (playQueueList.isEmpty())
                stop();
            else {
                ObservableAudioItem nextTrack = playQueueList.get(0).getTrack();
                play(nextTrack);
            }
        }

        public void addToQueue(Collection<ObservableAudioItem> audioItems) {
            if (playingRandom) {
                playQueueList.clear();
                playingRandom = false;
            }
            audioItems.stream().filter(JavaFxPlayer.Companion::isPlayable).map(TrackQueueRow::new).forEach(playQueueList::add);
        }

        public void increaseVolume() {
            if (trackPlayer != null) {
                double currentVolume = trackPlayer.getVolumeProperty().get();
                trackPlayer.setVolume(currentVolume + VOLUME_AMOUNT);
            }
        }

        public void decreaseVolume() {
            if (trackPlayer != null) {
                double currentVolume = trackPlayer.getVolumeProperty().get();
                trackPlayer.setVolume(currentVolume - VOLUME_AMOUNT);
            }
        }

        public void seek(Duration seekTime) {
            trackPlayer.seek(seekTime.toMillis());
            LOG.debug("Player seeked value {}", seekTime.toSeconds());
        }

        private Optional<ObservableAudioItem> currentTrack() {
            return currentTrack;
        }

        private Status playerStatus() {
            return trackPlayer == null ? UNKNOWN : trackPlayer.status();
        }
    }
}
