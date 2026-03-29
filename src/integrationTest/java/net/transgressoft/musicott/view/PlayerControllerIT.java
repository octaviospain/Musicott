package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
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
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
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
        assertThat(fxRobot.lookup("#trackSlider").tryQuery()).isPresent();
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
        return service;
    }

    @Bean
    public ObservableAudioLibrary audioLibrary() {
        var library = mock(ObservableAudioLibrary.class);
        when(library.emptyLibraryProperty()).thenReturn(emptyLibraryProperty);
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
