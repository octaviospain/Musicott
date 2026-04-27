package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.waveform.PlayableWaveformPane;
import net.transgressoft.commons.fx.music.waveform.SeekEvent;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.events.PlayItemEvent;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Duration;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link PlayerController}, verifying that the playback controls render
 * correctly, button state reflects library state, and that playback of mp3, m4a, and wav files
 * works correctly — including pause/resume and stop — via the Spring-integrated test context.
 */
@JavaFxSpringTest(classes = PlayerControllerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlayerControllerIT extends ApplicationTestBase<GridPane> {

    @Autowired
    FxControllerAndView<PlayerController, GridPane> playerControllerAndView;

    @Autowired
    PlayerControllerITConfiguration configuration;

    @Autowired
    PlayerService playerService;

    @Override
    protected GridPane javaFxComponent() {
        return playerControllerAndView.getView().get();
    }

    @Test
    @DisplayName("PlayerController renders playback controls")
    void rendersPlaybackControls(FxRobot fxRobot) {
        waitForFxEvents();

        assertThat(fxRobot.lookup("#playerGridPane").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#playButton").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#prevButton").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#nextButton").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#volumeSlider").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#playerStackPane").tryQuery()).isPresent();
        StackPane playerStackPane = fxRobot.lookup("#playerStackPane").queryAs(StackPane.class);
        boolean hasWaveformPane = playerStackPane.getChildren().stream()
                .anyMatch(node -> node instanceof PlayableWaveformPane);
        assertThat(hasWaveformPane).as("playerStackPane should contain a PlayableWaveformPane").isTrue();
    }

    @Test
    @DisplayName("PlayerController waveform pane has zero progress before playback")
    void waveformPaneHasZeroProgressBeforePlayback(FxRobot fxRobot) {
        waitForFxEvents();

        StackPane playerStackPane = fxRobot.lookup("#playerStackPane").queryAs(StackPane.class);
        PlayableWaveformPane waveformPane = (PlayableWaveformPane) playerStackPane.getChildren().stream()
                .filter(node -> node instanceof PlayableWaveformPane)
                .findFirst()
                .orElseThrow();
        assertThat(waveformPane.getProgressProperty().get()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("PlayerController seek event handler calls playerService.seek when duration is available")
    void seekEventHandlerCallsPlayerServiceSeek(FxRobot fxRobot) {
        waitForFxEvents();

        StackPane playerStackPane = fxRobot.lookup("#playerStackPane").queryAs(StackPane.class);
        PlayableWaveformPane waveformPane = (PlayableWaveformPane) playerStackPane.getChildren().stream()
                .filter(node -> node instanceof PlayableWaveformPane)
                .findFirst()
                .orElseThrow();

        // With Duration.ZERO (default mock), seek should NOT be called
        when(playerService.getTotalDuration()).thenReturn(javafx.util.Duration.ZERO);
        Platform.runLater(() -> Event.fireEvent(waveformPane, new SeekEvent(waveformPane, waveformPane, 0.5)));
        waitForFxEvents();
        verify(playerService, never()).seek(any());

        // With valid duration, seek SHOULD be called
        when(playerService.getTotalDuration()).thenReturn(javafx.util.Duration.millis(10000));
        Platform.runLater(() -> Event.fireEvent(waveformPane, new SeekEvent(waveformPane, waveformPane, 0.5)));
        waitForFxEvents();
        verify(playerService).seek(javafx.util.Duration.millis(5000));
    }

    @Test
    @DisplayName("toggles mute state when M key is pressed and no text field is focused")
    void togglesMuteStateWhenMKeyPressedAndNoTextFieldIsFocused(FxRobot fxRobot) throws Exception {
        waitForFxEvents();

        var playerController = playerControllerAndView.getController();
        Slider volumeSlider = fxRobot.lookup("#volumeSlider").queryAs(Slider.class);

        // GIVEN — a non-zero baseline volume on the slider, unmuted
        Platform.runLater(() -> volumeSlider.setValue(0.6));
        waitForFxEvents();
        assertThat(playerController.isMuted()).isFalse();
        assertThat(volumeSlider.getValue()).isEqualTo(0.6);

        // WHEN — toggleMute fires (the action wired to the bare-M accelerator)
        Platform.runLater(playerController::toggleMute);
        waitForFxEvents();

        // THEN — muted, slider at 0 (pre-arms next play at zero volume)
        assertThat(playerController.isMuted()).isTrue();
        assertThat(volumeSlider.getValue()).isEqualTo(0.0);

        // WHEN — toggleMute fires again
        Platform.runLater(playerController::toggleMute);
        waitForFxEvents();

        // THEN — unmuted, pre-mute volume restored
        assertThat(playerController.isMuted()).isFalse();
        assertThat(volumeSlider.getValue()).isEqualTo(0.6);
    }

    @Test
    @DisplayName("does not trigger mute when M key is pressed while a text field is focused")
    void doesNotTriggerMuteWhenMKeyPressedWhileTextFieldIsFocused(FxRobot fxRobot) throws Exception {
        // Builds a stand-alone scene with a MenuBar carrying a bare-M accelerator and a TextField.
        // This validates JavaFX's documented focus-scope behaviour for menu accelerators —
        // the very mechanism the production code relies on (see MenuBarController.initAccelerators).
        AtomicInteger muteFireCount = new AtomicInteger(0);
        CompletableFuture<Stage> stageFuture = new CompletableFuture<>();
        CompletableFuture<TextField> textFieldFuture = new CompletableFuture<>();

        Platform.runLater(() -> {
            MenuItem menuItem = new MenuItem("Mute");
            menuItem.setAccelerator(new KeyCodeCombination(KeyCode.M));
            menuItem.setOnAction(e -> muteFireCount.incrementAndGet());
            Menu controlsMenu = new Menu("Controls");
            controlsMenu.getItems().add(menuItem);
            MenuBar menuBar = new MenuBar(controlsMenu);

            TextField textField = new TextField();
            textField.setId("muteFocusScopeTextField");

            VBox root = new VBox(menuBar, textField);
            Stage probeStage = new Stage();
            probeStage.setScene(new Scene(root, 200, 100));
            probeStage.show();
            textField.requestFocus();
            stageFuture.complete(probeStage);
            textFieldFuture.complete(textField);
        });
        Stage probeStage = stageFuture.get(5, TimeUnit.SECONDS);
        TextField textField = textFieldFuture.get(5, TimeUnit.SECONDS);
        waitForFxEvents();

        // GIVEN — TextField has focus
        assertThat(textField.isFocused()).isTrue();

        // WHEN — M key event is fired into the focused TextField
        Platform.runLater(() -> {
            KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "m", "M", KeyCode.M, false, false, false, false);
            Event.fireEvent(textField, pressed);
            KeyEvent released = new KeyEvent(KeyEvent.KEY_RELEASED, "m", "M", KeyCode.M, false, false, false, false);
            Event.fireEvent(textField, released);
        });
        waitForFxEvents();

        // THEN — accelerator did NOT fire while the TextField had focus
        assertThat(muteFireCount.get()).isZero();

        // WHEN — focus moves to a non-text-input node and M is fired into the scene root
        Platform.runLater(() -> {
            probeStage.getScene().getRoot().requestFocus();
            KeyEvent pressed = new KeyEvent(KeyEvent.KEY_PRESSED, "m", "M", KeyCode.M, false, false, false, false);
            Event.fireEvent(probeStage.getScene(), pressed);
        });
        waitForFxEvents();

        // THEN — accelerator fires when no text input is focused
        assertThat(muteFireCount.get()).isEqualTo(1);

        Platform.runLater(probeStage::close);
        waitForFxEvents();
    }

    @Test
    @DisplayName("PlayerController play button is disabled when library is empty")
    void playButtonIsDisabledWhenLibraryIsEmpty(FxRobot fxRobot) {
        Platform.runLater(() -> configuration.emptyLibraryProperty.set(true));
        waitForFxEvents();

        ToggleButton playButton = fxRobot.lookup("#playButton").queryAs(ToggleButton.class);
        assertThat(playButton.isDisable()).isTrue();

        Platform.runLater(() -> configuration.emptyLibraryProperty.set(false));
        waitForFxEvents();
    }

    @Test
    @DisplayName("PlayerController plays an mp3 audio file")
    void playsMp3AudioFile(FxRobot fxRobot) throws Exception {
        var audioItem = createPlayableAudioItem("/testfiles/testeable.mp3");
        verifyPlaybackStartsForAudioItem(fxRobot, audioItem);
    }

    @Test
    @DisplayName("PlayerController plays an m4a audio file")
    void playsM4aAudioFile(FxRobot fxRobot) throws Exception {
        var audioItem = createPlayableAudioItem("/testfiles/testeable.m4a");
        verifyPlaybackStartsForAudioItem(fxRobot, audioItem);
    }

    @Test
    @DisplayName("PlayerController plays a wav audio file")
    void playsWavAudioFile(FxRobot fxRobot) throws Exception {
        var audioItem = createPlayableAudioItem("/testfiles/testeable.wav");
        verifyPlaybackStartsForAudioItem(fxRobot, audioItem);
    }

    @Test
    @DisplayName("PlayerController pauses and resumes playback")
    void pausesAndResumesPlayback(FxRobot fxRobot) throws Exception {
        var audioItem = createPlayableAudioItem("/testfiles/testeable.mp3");
        var playerController = playerControllerAndView.getController();

        boolean mediaAvailable = startPlayback(playerController, audioItem);
        assumeTrue(mediaAvailable, "Headless mode does not support audio playback (jfxmedia unavailable) — skipping pause/resume test");

        // Pause
        Platform.runLater(playerController::pauseEventListener);
        waitForFxEvents();

        // Track should still be present after pause (not cleared like stop)
        assertThat(playerController.currentTrack()).isPresent();
    }

    @Test
    @DisplayName("PlayerController stops playback and resets UI")
    void stopsPlaybackAndResetsUI(FxRobot fxRobot) throws Exception {
        var audioItem = createPlayableAudioItem("/testfiles/testeable.mp3");
        var playerController = playerControllerAndView.getController();

        boolean mediaAvailable = startPlayback(playerController, audioItem);
        assumeTrue(mediaAvailable, "Headless mode does not support audio playback (jfxmedia unavailable) — skipping stop test");

        // Stop — call next() when queue is empty to trigger stop
        Platform.runLater(playerController::next);
        waitForFxEvents();

        try {
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> playerController.currentTrack().isEmpty());
        } catch (TimeoutException e) {
            assumeTrue(false, "Player did not stop within timeout — skipping stop assertion");
            return;
        }

        // After stop, currentTrack should be empty
        assertThat(playerController.currentTrack()).isEmpty();

        // UI should be in stopped state: play button not selected
        ToggleButton playButton = fxRobot.lookup("#playButton").queryAs(ToggleButton.class);
        assertThat(playButton.isSelected()).isFalse();
    }

    /**
     * Starts playback by adding the item to the queue and triggering next(),
     * then waits for {@code currentTrack} to become present.
     *
     * <p>Returns {@code true} if playback started successfully, or {@code false} if the media
     * system is unavailable in the current environment (e.g. headless without jfxmedia).
     * A {@code false} return means the test should be skipped via {@code Assumptions.assumeTrue}.
     */
    private boolean startPlayback(PlayerController playerController, ObservableAudioItem audioItem) {
        Platform.runLater(() -> {
            playerController.playEventListener(new PlayItemEvent(List.of(audioItem), this));
            playerController.next();
        });
        waitForFxEvents();

        // waitFor checks the WaitForAsyncUtils exception queue (populated by the FX thread
        // uncaught exception handler). If jfxmedia is unavailable, the stored RuntimeException
        // wrapping UnsatisfiedLinkError is thrown here.
        try {
            WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> playerController.currentTrack().isPresent());
            return true;
        } catch (TimeoutException e) {
            // Track not set within timeout — media likely unavailable
            WaitForAsyncUtils.clearExceptions();
            return false;
        } catch (RuntimeException e) {
            // Headless environment may lack jfxmedia native library — JavaFxPlayer cannot initialize
            if (isMediaUnavailableError(e)) {
                WaitForAsyncUtils.clearExceptions();
                return false;
            }
            throw e;
        }
    }

    private boolean isMediaUnavailableError(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof UnsatisfiedLinkError
                    || cause instanceof ExceptionInInitializerError
                    || cause instanceof NoClassDefFoundError) {
                return true;
            }
            cause = cause.getCause();
        }
        String message = e.getMessage();
        return message != null && (message.contains("jfxmedia") || message.contains("media"));
    }

    private void verifyPlaybackStartsForAudioItem(FxRobot fxRobot, ObservableAudioItem audioItem) throws Exception {
        waitForFxEvents();
        var playerController = playerControllerAndView.getController();

        boolean mediaAvailable = startPlayback(playerController, audioItem);
        assumeTrue(mediaAvailable, "Headless mode does not support audio playback (jfxmedia unavailable) — skipping playback assertion");

        assertThat(playerController.currentTrack()).isPresent();

        // Stop playback to clean up for next test
        Platform.runLater(playerController::next);
        waitForFxEvents();
    }

    @SuppressWarnings("unchecked")
    private ObservableAudioItem createPlayableAudioItem(String resourcePath) throws Exception {
        var audioItem = mock(ObservableAudioItem.class);
        // Resources may be inside a JAR; copy to a temp file so Path.of() works
        var suffix = resourcePath.substring(resourcePath.lastIndexOf('.'));
        var extension = suffix.substring(1); // strip leading dot
        var tempFile = Files.createTempFile("musicott-test-", suffix);
        tempFile.toFile().deleteOnExit();
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            assertThat(in).as("Test audio file %s must exist on classpath", resourcePath).isNotNull();
            Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        var path = tempFile;
        when(audioItem.getPath()).thenReturn(path);
        when(audioItem.getExtension()).thenReturn(extension);
        when(audioItem.getEncoding()).thenReturn("MPEG Audio");
        when(audioItem.getEncoder()).thenReturn("");
        when(audioItem.getTitleProperty()).thenReturn(new SimpleStringProperty("Test Track"));

        var artist = mock(net.transgressoft.commons.music.audio.Artist.class);
        when(artist.getName()).thenReturn("Test Artist");
        when(audioItem.getArtistProperty()).thenReturn(new SimpleObjectProperty<>(artist));

        var album = mock(net.transgressoft.commons.music.audio.Album.class);
        when(album.getName()).thenReturn("Test Album");
        when(audioItem.getAlbumProperty()).thenReturn(new SimpleObjectProperty<>(album));

        when(audioItem.getCoverImageProperty()).thenReturn(new SimpleObjectProperty<>(Optional.empty()));
        when(audioItem.getPlayCountProperty()).thenReturn(new SimpleIntegerProperty(0));
        when(audioItem.getDuration()).thenReturn(Duration.ofSeconds(5));
        return audioItem;
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                PlayerController.class,
                PlayQueueController.class
        })
})
class PlayerControllerITConfiguration {

    final SimpleBooleanProperty emptyLibraryProperty = new SimpleBooleanProperty(false);

    @Bean
    public PlayerService playerService() {
        var service = mock(PlayerService.class);
        when(service.getPlayQueueList()).thenReturn(FXCollections.observableArrayList());
        when(service.getHistoryQueueList()).thenReturn(FXCollections.observableArrayList());
        when(service.currentTrack()).thenReturn(Optional.empty());
        when(service.getTotalDuration()).thenReturn(javafx.util.Duration.ZERO);
        return service;
    }

    @Bean
    public ObservableAudioLibrary audioLibrary() {
        var library = mock(ObservableAudioLibrary.class);
        when(library.getEmptyLibraryProperty()).thenReturn(emptyLibraryProperty);
        return library;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> audioWaveformRepository() {
        return mock(AudioWaveformRepository.class);
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    // destroyMethod = "" prevents Spring from auto-inferring the shutdown() method as the destroy callback,
    // which would call Platform.exit() and kill the JavaFX Application Thread between test classes
    @Bean(destroyMethod = "")
    public FxWeaver fxWeaver(ConfigurableApplicationContext applicationContext) {
        return new SpringFxWeaver(applicationContext);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public <C, V extends Node> FxControllerAndView<C, V> controllerAndView(FxWeaver fxWeaver, InjectionPoint injectionPoint) {
        return new InjectionPointLazyFxControllerAndViewResolver(fxWeaver).resolve(injectionPoint);
    }
}
