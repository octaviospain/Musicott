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
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
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
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import org.controlsfx.control.PopOver;
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

    private static final double PLAY_QUEUE_POPOVER_OFFSET = -10.0;
    private static final double PLAY_QUEUE_DESIGNED_HEIGHT = 467.0;
    private static final double PLAY_QUEUE_RESERVED_HEIGHT = 90.0;
    private static final double VOLUME_AMOUNT = 0.05;
    private static final String PLAY_QUEUE_BUTTON_STYLE = "-fx-effect: dropshadow(one-pass-box, rgb(99, 255, 109), 3, 0.2, 0, 0);";

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository;
    private final PlayerService playerService;
    private final ObservableAudioLibrary audioLibrary;
    private final ApplicationEventPublisher applicationEventPublisher;
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
    private boolean closingPlayQueuePopOver = false;
    private boolean reanchoringPlayQueuePopOver = false;
    private PopOver playQueuePopOver;
    private Window playQueueOwnerWindow;
    private Subscription progressSubscription;
    private final PauseTransition playQueueFocusLossDelay = new PauseTransition(Duration.millis(150));
    private final ChangeListener<Boolean> playQueueOwnerFocusListener = (observable, oldValue, focused) -> {
        if (Boolean.TRUE.equals(focused)) {
            playQueueFocusLossDelay.stop();
        } else if (playQueueOwnerWindow != null) {
            playQueueFocusLossDelay.playFromStart();
        }
    };
    private final ChangeListener<Boolean> playQueueOwnerShowingListener = (observable, oldValue, showing) -> {
        if (Boolean.FALSE.equals(showing))
            hidePlayQueue();
    };
    private final ChangeListener<Boolean> playQueueOwnerIconifiedListener = (observable, oldValue, iconified) -> {
        if (Boolean.TRUE.equals(iconified))
            hidePlayQueue();
    };
    private final ChangeListener<Window> playQueueSceneWindowListener = (observable, oldWindow, newWindow) -> {
        unbindPlayQueueOwnerWindow(oldWindow);
        bindPlayQueueOwnerWindow(newWindow);
    };
    private final ChangeListener<Number> playQueuePopupResizeListener = (observable, oldValue, newValue) -> applyPlayQueuePopupSizing();
    private final ChangeListener<Scene> playQueueSizingSceneListener = (observable, oldScene, newScene) ->
            configurePlayQueuePopupSizingForScene(oldScene, newScene);
    private final ChangeListener<Scene> playQueueOwnerLifecycleSceneListener = (observable, oldScene, newScene) ->
            configurePlayQueueOwnerLifecycleForScene(oldScene, newScene);
    private final javafx.event.EventHandler<MouseEvent> playQueuePopOverMousePressedHandler = event -> {
        if (playQueuePopOver != null && playQueuePopOver.isShowing()
                && isWithinPlayQueueButton(event.getScreenX(), event.getScreenY())) {
            hidePlayQueue();
            event.consume();
        }
    };

    @Autowired
    public PlayerController(AudioWaveformRepository<AudioWaveform, ObservableAudioItem> waveformRepository,
                            PlayerService playerService,
                            ObservableAudioLibrary audioLibrary,
                            ApplicationEventPublisher applicationEventPublisher) {
        this.waveformRepository = waveformRepository;
        this.playerService = playerService;
        this.audioLibrary = audioLibrary;
        this.applicationEventPublisher = applicationEventPublisher;
        playQueueFocusLossDelay.setOnFinished(_ -> {
            if (playQueueOwnerWindow == null || playQueueOwnerWindow.isFocused())
                return;
            if (playQueueOwnerWindow instanceof Stage stage && stage.isIconified())
                return;
            hidePlayQueue();
        });
    }

    @FXML
    public void initialize() {
        playButton.disableProperty().bind(emptyLibraryProperty);
        playButton.setOnAction(_ -> playPause());
        prevButton.setOnAction(_ -> previous());
        nextButton.setOnAction(_ -> next());
        subscribe(volumeSlider.valueChangingProperty(), changing -> {
            if (Boolean.FALSE.equals(changing))
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
            java.time.Duration total = playerService.getTotalDuration();
            if (!total.isZero()) {
                long seekMillis = Math.round(event.getSeekRatio() * total.toMillis());
                playerService.seek(java.time.Duration.ofMillis(seekMillis));
            }
        });

        playQueueLayout.setVisible(true);
        playQueueStackPane.getChildren().remove(playQueueLayout);
        playQueuePopOver = new PopOver(playQueueLayout);
        playQueuePopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        playQueuePopOver.setDetachable(false);
        playQueuePopOver.setAnimated(false);
        playQueuePopOver.setHeaderAlwaysVisible(false);
        playQueuePopOver.setAutoHide(true);
        playQueuePopOver.setAutoFix(true);
        playQueuePopOver.setConsumeAutoHidingEvents(false);
        playQueuePopOver.getStyleClass().add("play-queue-popover");
        playQueuePopOver.getRoot().getStylesheets().add(Objects.requireNonNull(getClass()
                .getResource("/css/playqueuepane.css")).toExternalForm());
        playQueuePopOver.setOnAutoHide(_ -> playQueueButton.setSelected(false));
        playQueuePopOver.setOnShown(_ -> {
            if (playQueuePopOver.getScene() != null) {
                playQueuePopOver.getScene().removeEventFilter(MouseEvent.MOUSE_PRESSED, playQueuePopOverMousePressedHandler);
                playQueuePopOver.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, playQueuePopOverMousePressedHandler);
            }
        });
        playQueuePopOver.setOnHidden(_ -> {
            if (playQueuePopOver.getScene() != null)
                playQueuePopOver.getScene().removeEventFilter(MouseEvent.MOUSE_PRESSED, playQueuePopOverMousePressedHandler);
            if (!reanchoringPlayQueuePopOver)
                restoreOrDeselectPlayQueueButton();
        });
        configurePlayQueuePopupSizing();
        configurePlayQueuePopupOwnerLifecycle();

        playQueueButton.setOnAction(event -> {
            if (playQueuePopOver.isShowing())
                hidePlayQueue();
            else
                showPlayQueue();
        });

        playQueueButton.setOnDragDropped(this::onDragDroppedOnPlayQueueButton);
        playQueueButton.setOnDragOver(this::onDragOverOnPlayQueueButton);
        playQueueButton.setOnDragExited(this::onDragExitedOnPlayQueueButton);

        hidePlayQueue();
    }

    /**
     * Caps the queue content height so the popover can stay within smaller windows
     * without relying on the old in-scene StackPane margin positioning.
     */
    private void configurePlayQueuePopupSizing() {
        playerGridPane.sceneProperty().addListener(playQueueSizingSceneListener);
        configurePlayQueuePopupSizingForScene(null, playerGridPane.getScene());
    }

    private void configurePlayQueuePopupSizingForScene(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.heightProperty().removeListener(playQueuePopupResizeListener);
            oldScene.widthProperty().removeListener(playQueuePopupResizeListener);
        }
        if (newScene != null) {
            newScene.heightProperty().addListener(playQueuePopupResizeListener);
            newScene.widthProperty().addListener(playQueuePopupResizeListener);
            applyPlayQueuePopupSizing();
        }
    }

    private void applyPlayQueuePopupSizing() {
        var scene = playerGridPane.getScene();
        if (scene == null) return;
        double sceneH = scene.getHeight();
        if (sceneH <= 0) return;
        double maxAllowed = Math.max(120.0, sceneH - PLAY_QUEUE_RESERVED_HEIGHT);
        double popupH = Math.min(PLAY_QUEUE_DESIGNED_HEIGHT, maxAllowed);
        playQueueLayout.setPrefHeight(popupH);
        playQueueLayout.setMaxHeight(popupH);
        boolean reopenAfterResize = playQueuePopOver != null
                && (playQueuePopOver.isShowing() || playQueueButton.isSelected());
        if (reopenAfterResize) {
            Platform.runLater(() -> {
                if (playQueuePopOver != null) {
                    reanchoringPlayQueuePopOver = true;
                    try {
                        if (playQueuePopOver.isShowing())
                            playQueuePopOver.hide();
                        showPlayQueuePopOver();
                        playQueueButton.setSelected(true);
                    } finally {
                        reanchoringPlayQueuePopOver = false;
                    }
                }
            });
        }
    }

    /**
     * Keeps the queue popover tied to the primary stage lifecycle so it never stays
     * visible after the application window loses desktop ownership.
     */
    private void configurePlayQueuePopupOwnerLifecycle() {
        playerGridPane.sceneProperty().addListener(playQueueOwnerLifecycleSceneListener);
        configurePlayQueueOwnerLifecycleForScene(null, playerGridPane.getScene());
    }

    private void configurePlayQueueOwnerLifecycleForScene(Scene oldScene, Scene newScene) {
        if (oldScene != null)
            oldScene.windowProperty().removeListener(playQueueSceneWindowListener);
        unbindPlayQueueOwnerWindow(playQueueOwnerWindow);
        if (newScene != null) {
            newScene.windowProperty().addListener(playQueueSceneWindowListener);
            bindPlayQueueOwnerWindow(newScene.getWindow());
        }
    }

    private void bindPlayQueueOwnerWindow(Window window) {
        if (window == null || window == playQueueOwnerWindow)
            return;
        playQueueOwnerWindow = window;
        playQueueOwnerWindow.focusedProperty().addListener(playQueueOwnerFocusListener);
        playQueueOwnerWindow.showingProperty().addListener(playQueueOwnerShowingListener);
        if (playQueueOwnerWindow instanceof Stage stage)
            stage.iconifiedProperty().addListener(playQueueOwnerIconifiedListener);
    }

    private void unbindPlayQueueOwnerWindow(Window window) {
        if (window == null)
            return;
        window.focusedProperty().removeListener(playQueueOwnerFocusListener);
        window.showingProperty().removeListener(playQueueOwnerShowingListener);
        if (window instanceof Stage stage)
            stage.iconifiedProperty().removeListener(playQueueOwnerIconifiedListener);
        if (window == playQueueOwnerWindow)
            playQueueOwnerWindow = null;
    }

    /**
     * Restores the popover after owner-window geometry changes, but lets explicit
     * dismissals clear the toggle state and keep the queue closed.
     */
    private void restoreOrDeselectPlayQueueButton() {
        if (closingPlayQueuePopOver || !playQueueButton.isSelected()) {
            playQueueButton.setSelected(false);
            return;
        }
        if (playQueueOwnerWindow == null || !playQueueOwnerWindow.isShowing() || !playQueueOwnerWindow.isFocused()) {
            playQueueButton.setSelected(false);
            return;
        }
        Platform.runLater(() -> {
            if (playQueuePopOver != null && !playQueuePopOver.isShowing() && playQueueButton.isSelected())
                showPlayQueuePopOver();
        });
    }

    private boolean isWithinPlayQueueButton(double screenX, double screenY) {
        Bounds bounds = playQueueButton.localToScreen(playQueueButton.getBoundsInLocal());
        return bounds != null && bounds.contains(screenX, screenY);
    }

    @PreDestroy
    public void dispose() {
        playerGridPane.sceneProperty().removeListener(playQueueSizingSceneListener);
        playerGridPane.sceneProperty().removeListener(playQueueOwnerLifecycleSceneListener);
        if (playerGridPane.getScene() != null) {
            playerGridPane.getScene().heightProperty().removeListener(playQueuePopupResizeListener);
            playerGridPane.getScene().widthProperty().removeListener(playQueuePopupResizeListener);
            playerGridPane.getScene().windowProperty().removeListener(playQueueSceneWindowListener);
        }
        playQueueFocusLossDelay.stop();
        unbindPlayQueueOwnerWindow(playQueueOwnerWindow);
        hidePlayQueue();
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
                    .map(ObservableAudioItem.class::cast)
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
                applicationEventPublisher.publishEvent(new PlayRandomFromContextEvent(this));
                // If the context had no playable tracks, no playback started — keep the toggle in
                // sync with the still-stopped player instead of leaving it showing the pause icon.
                if (playerService.currentTrack().isEmpty()) {
                    playButton.setSelected(false);
                }
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
        if (playQueuePopOver != null && playQueuePopOver.isShowing()) {
            closingPlayQueuePopOver = true;
            try {
                playQueuePopOver.hide();
            } finally {
                closingPlayQueuePopOver = false;
            }
        }
        playQueueButton.setSelected(false);
    }

    private void showPlayQueue() {
        if (playQueuePopOver != null && !playQueuePopOver.isShowing()) {
            showPlayQueuePopOver();
        }
        playQueueButton.setSelected(true);
    }

    private void showPlayQueuePopOver() {
        playQueuePopOver.show(playQueueButton, PLAY_QUEUE_POPOVER_OFFSET);
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
        logger.trace("Setting up player and view for track {}", currentTrack);

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
        // Delegate to the unified random path so shuffle, queue-replacement, empty-pool feedback,
        // and the playingRandom lifecycle are handled in one place.
        playerService.playRandom(playlist.getAudioItemsProperty());
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

        // Unsubscribe previous tick subscription before re-binding to the new track's currentTime
        if (progressSubscription != null) progressSubscription.unsubscribe();

        // Single subscription updates both waveform progress and time labels per tick,
        // calling getTotalDuration() once to avoid redundant player calls.
        var currentTimeProp = playerService.getCurrentTimeProperty();
        if (currentTimeProp != null) {
            progressSubscription = subscribe(currentTimeProp, time -> {
                java.time.Duration total = playerService.getTotalDuration();
                if (!total.isZero()) {
                    playableWaveformPane.getProgressProperty().set(time.toMillis() / total.toMillis());
                }
                updateTrackLabels(time, Duration.millis(total.toMillis()));
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
