package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.waveform.WaveformPane;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.music.player.AudioItemPlayer.Status;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.services.lastfm.LastFmService;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.table.AudioItemTableViewBase;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
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
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.transgressoft.commons.music.player.AudioItemPlayer.Status.*;
import static org.fxmisc.easybind.EasyBind.combine;
import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * Controller for the player UI pane. Delegates all playback and queue operations
 * to {@link PlayerService}. Handles view bindings and responds to Spring events
 * to keep the UI in sync with playback state.
 */
@FxmlView("/fxml/PlayerController.fxml")
@Controller
public class PlayerController {

    private static final double VOLUME_AMOUNT = 0.05;
    private static final String PLAY_QUEUE_BUTTON_STYLE = "-fx-effect: dropshadow(one-pass-box, rgb(99, 255, 109), 3, 0.2, 0, 0);";

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlayerService playerService;
    private final ObservableAudioLibrary audioLibrary;
    private final Image defaultCoverImage = ApplicationImage.DEFAULT_COVER.get();

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
    @FXML
    private PlayQueueController playQueueLayoutController;

    private LastFmService lastFmService;
    private WaveformPane waveformPane;
    private ReadOnlyBooleanProperty emptyLibraryProperty;
    private boolean scrobbled = false;

    @Autowired
    public PlayerController(AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository,
                            ApplicationEventPublisher applicationEventPublisher,
                            PlayerService playerService,
                            ObservableAudioLibrary audioLibrary) {
        this.waveformRepository = waveformRepository;
        this.applicationEventPublisher = applicationEventPublisher;
        this.playerService = playerService;
        this.audioLibrary = audioLibrary;
    }

    @FXML
    public void initialize() {
        playButton.disableProperty().bind(emptyLibraryProperty);
        playButton.setOnAction(_ -> playPause());
        prevButton.setOnAction(_ -> previous());
        nextButton.setOnAction(_ -> next());
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

        PauseTransition resizeDebounce = new PauseTransition(Duration.millis(150));
        resizeDebounce.setOnFinished(e -> {
            waveformPane.setWidth(waveformAnchorPane.getWidth());
            waveformPane.setHeight(waveformAnchorPane.getHeight());
        });
        waveformAnchorPane.widthProperty().addListener((_, _, _) -> resizeDebounce.playFromStart());
        waveformAnchorPane.heightProperty().addListener((_, _, _) -> resizeDebounce.playFromStart());

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
        if (dragBoard.hasContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT)) {
            var selectedTracksIds = (List<Integer>) dragBoard.getContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT);
            var resolvedItems = selectedTracksIds.stream()
                    .map(id -> audioLibrary.findById(id).orElse(null))
                    .filter(Objects::nonNull)
                    .map(item -> (ObservableAudioItem) item)
                    .toList();
            if (!resolvedItems.isEmpty()) {
                playerService.addToQueue(resolvedItems);
                logger.debug("Added {} tracks to queue via play queue button drop", resolvedItems.size());
            }
            event.setDropCompleted(true);
        }
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
            if (playerService.currentTrack().isPresent()) {
                playerService.resume();
            } else {
                playerService.playRandom();
            }
        } else {
            playerService.pause();
        }
    }

    public void previous() {
        playerService.previous();
    }

    public void next() {
        playerService.next();
    }

    public Optional<ObservableAudioItem> currentTrack() {
        return playerService.currentTrack();
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
        playerService.increaseVolume();
        volumeSlider.setValue(volumeSlider.getValue() + VOLUME_AMOUNT);
    }

    public void decreaseVolume() {
        playerService.decreaseVolume();
        volumeSlider.setValue(volumeSlider.getValue() - VOLUME_AMOUNT);
    }

    /**
     * Updates the components of the player pane such as the song title label, the artist label,
     * the cover image, or the waveform image; with the given current {@link ObservableAudioItem}.
     */
    public void updatePlayerComponents(ObservableAudioItem currentTrack) {
        logger.debug("Setting up player and view for track {}", currentTrack);
        Color backgroundColor = Color.rgb(73, 73, 73);
        Color waveformColor = Color.rgb(34, 34, 34);

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
                playerService.seek(Duration.millis(trackSlider.getValue()));
            }
        });
    }

    public void playFromQueue(TrackQueueRow trackQueueRow) {
        playerService.playFromQueue(trackQueueRow);
    }

    public void playFromHistoryQueue(TrackQueueRow trackQueueRow) {
        playerService.playFromHistoryQueue(trackQueueRow);
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

    @Value("#{audioLibrary.emptyLibraryProperty()}")
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
        playerService.pause();
    }

    @EventListener
    public void playEventListener(PlayItemEvent playItemEvent) {
        List<ObservableAudioItem> audioItems = playItemEvent.audioItems;
        playerService.addToQueue(audioItems);
        playerService.next();
    }

    @EventListener
    public void playPlaylistRandomlyEventListener(PlayPlaylistRandomlyEvent playPlaylistRandomlyEvent) {
        ObservablePlaylist playlist = playPlaylistRandomlyEvent.playlist;
        playerService.addToQueue(playlist.getAudioItemsProperty());
        playerService.next();
    }

    @EventListener
    public void addAudioItemsToPlayQueueEventListener(AddToPlayQueueEvent addToPlayQueueEvent) {
        playerService.addToQueue(addToPlayQueueEvent.audioItems);
    }

    @EventListener
    public void trackChangedEventListener(AudioItemChangedEvent event) {
        scrobbled = false;
        updatePlayerComponents(event.currentTrack);

        // Bind volume slider bidirectionally to new player
        DoubleProperty volumeProp = playerService.getVolumeProperty();
        if (volumeProp != null) {
            volumeProp.bindBidirectional(volumeSlider.valueProperty());
        }

        // Re-subscribe to current time for slider, progress bar, labels, and scrobbling
        var currentTimeProp = playerService.getCurrentTimeProperty();
        if (currentTimeProp != null) {
            DoubleProperty trackSliderMaxProperty = trackSlider.maxProperty();
            playerService.currentTrack().ifPresent(track -> trackSliderMaxProperty.setValue(track.getDuration().toMillis()));

            BooleanProperty trackSliderValueChangingProperty = trackSlider.valueChangingProperty();
            DoubleProperty trackSliderValueProperty = trackSlider.valueProperty();
            subscribe(currentTimeProp, time -> {
                if (!trackSliderValueChangingProperty.get())
                    trackSliderValueProperty.setValue(time.toMillis());
            });

            DoubleProperty trackProgressBarProgressProperty = trackProgressBar.progressProperty();
            subscribe(trackSliderValueProperty, value -> {
                Double endTime = playerService.getTotalDuration().toMillis();
                if (trackSliderValueChangingProperty.get() && (!endTime.equals(Double.POSITIVE_INFINITY) || !endTime.equals(Double.NaN))) {
                    trackProgressBarProgressProperty.set(value.doubleValue() / endTime);
                    playerService.seek(Duration.millis(value.doubleValue()));
                }
            });

            subscribe(currentTimeProp, t -> trackProgressBarProgressProperty.set(t.toMillis() / trackSliderMaxProperty.get()));
            subscribe(currentTimeProp, t -> updateTrackLabels(t, playerService.getTotalDuration()));

            subscribe(currentTimeProp, time -> {
                if (isCurrentTrackValidToScrobble(time)) {
                    scrobbled = true;
                    playerService.currentTrack().ifPresent(track -> {
                        if (lastFmService != null) {
                            lastFmService.updateNowPlaying(track);
                            lastFmService.scrobble(track);
                        }
                    });
                }
            });
        }
    }

    @EventListener
    public void playbackStatusChangedEventListener(PlaybackStatusChangedEvent event) {
        Status status = event.status;
        if (PLAYING == status) {
            setPlaying();
        } else if (PAUSED == status) {
            playButtonSelectedProperty().setValue(false);
        } else if (STOPPED == status) {
            setStopped();
            // Unbind volume slider when player stops
            DoubleProperty volumeProp = playerService.getVolumeProperty();
            if (volumeProp != null) {
                volumeSlider.valueProperty().unbindBidirectional(volumeProp);
            }
        }
    }

    private boolean isCurrentTrackValidToScrobble(Duration newTime) {
        Duration totalDuration = playerService.getTotalDuration();
        boolean isDurationBeyond30Seconds = totalDuration.greaterThanOrEqualTo(Duration.seconds(30));
        boolean isDurationBeyondMidTime = newTime.greaterThanOrEqualTo(totalDuration.divide(2.0));
        boolean isDurationLongerThan4Minutes = newTime.greaterThanOrEqualTo(Duration.minutes(4));

        return !scrobbled && isDurationBeyond30Seconds && (isDurationBeyondMidTime || isDurationLongerThan4Minutes);
    }
}
