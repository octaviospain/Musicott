package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.waveform.PlayableWaveformPane;
import net.transgressoft.commons.fx.music.waveform.SeekEvent;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.music.player.AudioItemPlayer.Status;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.table.AudioItemTableViewBase;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import jakarta.annotation.PreDestroy;
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
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
import org.fxmisc.easybind.Subscription;

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
    private Slider volumeSlider;
    @FXML
    private ProgressBar volumeProgressBar;

    @FXML
    private AnchorPane playQueueLayout;
    @FXML
    private PlayQueueController playQueueLayoutController;

    private PlayableWaveformPane playableWaveformPane;
    private ReadOnlyBooleanProperty emptyLibraryProperty;
    private boolean muted = false;
    private double preMuteVolume = 0.0;
    private Subscription progressSubscription;
    private Subscription labelsSubscription;

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

        playableWaveformPane = new PlayableWaveformPane();
        playableWaveformPane.getBackgroundColorProperty().set(Color.rgb(73, 73, 73));
        playableWaveformPane.getWaveformColorProperty().set(Color.rgb(34, 34, 34));
        playableWaveformPane.getPlayedColorProperty().set(Color.rgb(99, 255, 109));
        // Add waveform behind the labels VBox in the StackPane (z-order: waveform at index 0, VBox on top)
        playerStackPane.getChildren().add(0, playableWaveformPane);

        playableWaveformPane.addEventHandler(SeekEvent.Companion.getSEEK(), event -> {
            Duration total = playerService.getTotalDuration();
            if (total != null && total.toMillis() > 0) {
                playerService.seek(Duration.millis(event.getSeekRatio() * total.toMillis()));
            }
        });

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
        configurePlayQueuePopupSizing();
        hidePlayQueue();
    }

    /**
     * Sizes and positions the play-queue popup so it always fits within the current scene.
     * The popup's height is capped to leave room for the menubar + player area, and the
     * StackPane bottom margin is recomputed on every scene-height change so the popup
     * tracks the available space instead of relying on a fixed offset that overflows the
     * top of the window at small heights.
     */
    private void configurePlayQueuePopupSizing() {
        double designedHeight = playQueueLayout.getPrefHeight();   // 467 in PlayQueueController.fxml
        double topReserved = 30.0;                                 // menubar + small gap
        double bottomReserved = 60.0;                              // player area + small gap

        Runnable apply = () -> {
            var scene = playQueueLayout.getScene();
            if (scene == null) return;
            double sceneH = scene.getHeight();
            if (sceneH <= 0) return;
            double maxAllowed = Math.max(120.0, sceneH - topReserved - bottomReserved);
            double popupH = Math.min(designedHeight, maxAllowed);
            playQueueLayout.setPrefHeight(popupH);
            playQueueLayout.setMaxHeight(popupH);
            // Pin popup just above the player area; if the window is taller than the
            // popup needs, push it up so it sits near the top of the window (preserves
            // the original design intent of having the queue float above the table area).
            double bottomMargin = Math.max(bottomReserved, sceneH - popupH - topReserved);
            StackPane.setMargin(playQueueLayout, new Insets(0, 0, bottomMargin, 0));
        };

        playQueueLayout.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.heightProperty().addListener((o, ov, nv) -> apply.run());
                apply.run();
            }
        });
        apply.run();
    }

    @PreDestroy
    public void dispose() {
        playableWaveformPane.dispose();
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
        playableWaveformPane.getProgressProperty().set(0.0);
        nextButton.setDisable(true);
        prevButton.setDisable(true);
        songTitleLabel.textProperty().unbind();
        songTitleLabel.setText("");
        artistAlbumLabel.textProperty().unbind();
        artistAlbumLabel.setText("");
        currentCover.setVisible(false);
        currentTimeLabel.setText("");
        remainingTimeLabel.setText("");
    }

    public void setPlaying() {
        playButton.setSelected(true);
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
     * Toggles the muted state of the audio output. While muted, the previous volume value is
     * preserved and restored on unmute. When toggled while paused, the muted state is pre-armed
     * for the next play — no audio change occurs until playback resumes, at which point the
     * underlying player picks up the muted volume from the bidirectionally-bound slider.
     */
    public void toggleMute() {
        if (muted) {
            volumeSlider.setValue(preMuteVolume);
            muted = false;
            logger.debug("Player unmuted (volume restored to {})", preMuteVolume);
        } else {
            preMuteVolume = volumeSlider.getValue();
            volumeSlider.setValue(0.0);
            muted = true;
            logger.debug("Player muted (pre-mute volume saved as {})", preMuteVolume);
        }
    }

    public boolean isMuted() {
        return muted;
    }

    /**
     * Updates the components of the player pane such as the song title label, the artist label,
     * the cover image, or the waveform image; with the given current {@link ObservableAudioItem}.
     */
    public void updatePlayerComponents(ObservableAudioItem currentTrack) {
        logger.debug("Setting up player and view for track {}", currentTrack);

        waveformRepository.getOrCreateWaveformAsync(currentTrack,
                        (short) playableWaveformPane.getWidth(),
                        (short) playableWaveformPane.getHeight())
                .thenAccept(waveform -> Platform.runLater(() -> playableWaveformPane.loadWaveform(waveform)));

        songTitleLabel.textProperty().bind(currentTrack.getTitleProperty());
        artistAlbumLabel.textProperty().bind(
                combine(currentTrack.getArtistProperty(), currentTrack.getAlbumProperty(), (art, alb) -> art.getName() + " - " + alb.getName()));

        currentTrack.getCoverImageProperty().get().ifPresentOrElse(currentCover::setImage, () -> currentCover.setImage(defaultCoverImage));
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

    @Value("#{audioLibrary.getEmptyLibraryProperty()}")
    public void setEmptyLibraryProperty(ReadOnlyBooleanProperty emptyLibraryProperty) {
        this.emptyLibraryProperty = emptyLibraryProperty;
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
        updatePlayerComponents(event.currentTrack);

        // Bind volume slider bidirectionally to new player
        DoubleProperty volumeProp = playerService.getVolumeProperty();
        if (volumeProp != null) {
            volumeProp.bindBidirectional(volumeSlider.valueProperty());
        }

        // Unsubscribe previous track subscriptions to prevent listener accumulation
        if (progressSubscription != null) progressSubscription.unsubscribe();
        if (labelsSubscription != null) labelsSubscription.unsubscribe();

        // Re-subscribe to current time for progress binding and labels
        var currentTimeProp = playerService.getCurrentTimeProperty();
        if (currentTimeProp != null) {
            progressSubscription = subscribe(currentTimeProp, time -> {
                Duration total = playerService.getTotalDuration();
                if (total != null && total.toMillis() > 0) {
                    playableWaveformPane.getProgressProperty().set(time.toMillis() / total.toMillis());
                }
            });

            labelsSubscription = subscribe(currentTimeProp, t -> {
                Duration total = playerService.getTotalDuration();
                if (total != null) updateTrackLabels(t, total);
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

}
