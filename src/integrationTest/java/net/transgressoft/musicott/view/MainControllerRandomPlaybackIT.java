package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableArtistCatalog;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.events.PlayRandomFromContextEvent;
import net.transgressoft.musicott.service.MediaImportService;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import net.transgressoft.musicott.view.itunes.ItunesImportWizard;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.aop.support.AopUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration tests for contextual random playback dispatched from {@link MainController}.
 *
 * <p>Verifies that the {@link PlayRandomFromContextEvent} listener correctly resolves the
 * active navigation context's item pool and delegates to
 * {@link PlayerService#playRandom(Collection)}, and that an empty pool does not result
 * in a call to {@code playRandom} (no silent failure).
 *
 * @author Octavio Calleya
 */
@JavaFxSpringTest(classes = MainControllerRandomPlaybackITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("MainController random playback")
class MainControllerRandomPlaybackIT extends ApplicationTestBase<BorderPane> {

    @Autowired
    FxControllerAndView<MainController, BorderPane> mainControllerAndView;

    @Autowired
    PlayerService playerService;

    @Autowired
    NavigationController navigationController;

    @Autowired
    ArtistViewController artistViewController;

    @Autowired
    ObservablePlaylistHierarchy playlistRepository;

    @Autowired
    ObservableAudioLibrary audioLibrary;

    @Autowired
    ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    @Override
    protected BorderPane javaFxComponent() {
        return mainControllerAndView.getView().get();
    }

    @BeforeEach
    void resetPlayerServiceMock() {
        reset(playerService);
        when(playerService.getPlayQueueList()).thenReturn(FXCollections.observableArrayList());
        when(playerService.getHistoryQueueList()).thenReturn(FXCollections.observableArrayList());
        when(playerService.currentTrack()).thenReturn(Optional.empty());
        when(playerService.getTotalDuration()).thenReturn(java.time.Duration.ZERO);
    }

    @SuppressWarnings("unchecked")
    private void setNavigationMode(NavigationController.NavigationMode mode) {
        // Unwrap CGLIB proxy to reach the actual NavigationController fields
        Object target = AopUtils.isAopProxy(navigationController)
                ? AopTestUtils.getTargetObject(navigationController)
                : navigationController;
        var navProp = (javafx.beans.property.ObjectProperty<NavigationController.NavigationMode>)
                ReflectionTestUtils.getField(target, "navigationModeProperty");
        Platform.runLater(() -> navProp.set(mode));
        waitForFxEvents();
    }

    @SuppressWarnings("unchecked")
    private void verifyPlayRandomCalledWith(Collection<ObservableAudioItem> expected) {
        ArgumentCaptor<Collection<ObservableAudioItem>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(playerService).playRandom(captor.capture());
        assertEquals(Set.copyOf(expected), Set.copyOf(captor.getValue()));
    }

    @Test
    @DisplayName("PlayRandomFromContextEvent with non-empty ALL_AUDIO_ITEMS library delegates to playerService.playRandom with the full library")
    void playRandomFromContextWithNonEmptyLibraryCallsPlayRandom() {
        setNavigationMode(NavigationController.NavigationMode.ALL_AUDIO_ITEMS);

        Platform.runLater(() ->
                mainControllerAndView.getController().onPlayRandomFromContext(
                        new PlayRandomFromContextEvent(this)));
        waitForFxEvents();

        verifyPlayRandomCalledWith(List.copyOf(audioLibrary.getAudioItemsProperty()));
    }

    @Test
    @DisplayName("PlayRandomFromContextEvent with PLAYLIST mode delegates to playerService.playRandom with selected playlist items")
    void playRandomFromContextWithPlaylistModeCallsPlayRandom() {
        // Ensure a non-empty playlist is selected before entering PLAYLIST mode
        var libraryItems = (ObservableList<ObservableAudioItem>) audioLibrary.getAudioItemsProperty();
        var expectedItems = List.copyOf(libraryItems);
        var playlist = playlistRepository.createPlaylist("Test Playlist For Random", expectedItems);
        Platform.runLater(() -> selectedPlaylistProperty.set(Optional.of(playlist)));
        waitForFxEvents();
        setNavigationMode(NavigationController.NavigationMode.PLAYLIST);

        Platform.runLater(() ->
                mainControllerAndView.getController().onPlayRandomFromContext(
                        new PlayRandomFromContextEvent(this)));
        waitForFxEvents();

        verifyPlayRandomCalledWith(expectedItems);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("PlayRandomFromContextEvent in ARTISTS mode delegates to playerService.playRandom with the selected artist's involved tracks")
    void playRandomFromContextWithArtistsModeCallsPlayRandom() {
        // Select the shown artist directly so the ARTISTS context resolves a non-empty pool,
        // independent of list auto-selection timing.
        var artistCatalog = audioLibrary.getArtistCatalogsProperty().iterator().next();
        Object artistViewTarget = AopUtils.isAopProxy(artistViewController)
                ? AopTestUtils.getTargetObject(artistViewController)
                : artistViewController;
        @SuppressWarnings("unchecked")
        var selectedArtistProp = (ObjectProperty<Optional<ObservableArtistCatalog>>)
                ReflectionTestUtils.getField(artistViewTarget, "selectedArtistProperty");
        Platform.runLater(() -> selectedArtistProp.set(Optional.of(artistCatalog)));
        waitForFxEvents();
        setNavigationMode(NavigationController.NavigationMode.ARTISTS);

        var expectedItems = artistViewController.getSelectedArtistAudioItems();
        assertEquals(List.copyOf(audioLibrary.getAudioItemsProperty()), expectedItems,
                "the single-track fixture's only artist should resolve to the full library");

        Platform.runLater(() ->
                mainControllerAndView.getController().onPlayRandomFromContext(
                        new PlayRandomFromContextEvent(this)));
        waitForFxEvents();

        verifyPlayRandomCalledWith(expectedItems);
    }

    @Test
    @DisplayName("MainController keeps the player controller fully on-screen in every navigation mode when the window is short")
    void keepsPlayerVisibleAcrossNavModesWhenWindowIsShort() {
        var view = mainControllerAndView.getView().get();
        // Shrink the window well below the default 800x600 mount so the "short window" the layout
        // must survive is actually exercised; restored afterwards so later tests keep a full stage.
        Platform.runLater(() -> testStage.setHeight(360));
        waitForFxEvents();
        try {
            for (var mode : NavigationController.NavigationMode.values()) {
                setNavigationMode(mode);
                waitForFxEvents();

                Node player = view.lookup("#playerGridPane");
                assertNotNull(player, "player region must be present in mode " + mode);
                double sceneHeight = view.getScene().getHeight();
                assertTrue(sceneHeight <= 400.0,
                        "window must actually be short (<=400px) to exercise this case in mode "
                                + mode + " but scene was " + sceneHeight + "px");
                double playerBottom = player.localToScene(player.getBoundsInLocal()).getMaxY();
                double playerHeight = player.getBoundsInLocal().getHeight();

                assertTrue(playerBottom <= sceneHeight + 0.5,
                        "player bottom (" + playerBottom + ") must stay within the "
                                + sceneHeight + "px scene in mode " + mode);
                assertTrue(playerHeight >= 50.0,
                        "player must keep its full height (>=50) in mode " + mode + " but was " + playerHeight);
            }
        } finally {
            Platform.runLater(() -> testStage.setHeight(600));
            waitForFxEvents();
        }
    }

    @Test
    @DisplayName("PlayRandomFromContextEvent with no playlist selected in PLAYLIST mode does not call playerService.playRandom")
    void playRandomFromContextWithNoPlaylistSelectedDoesNotCallPlayRandom() {
        setNavigationMode(NavigationController.NavigationMode.PLAYLIST);
        // No playlist selected → empty context

        Platform.runLater(() ->
                mainControllerAndView.getController().onPlayRandomFromContext(
                        new PlayRandomFromContextEvent(this)));
        waitForFxEvents();

        verify(playerService, never()).playRandom(any(Collection.class));
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                MainController.class,
                NavigationController.class,
                PlayerController.class,
                PlayQueueController.class,
                ArtistViewController.class,
                AlbumViewController.class,
                GenreViewController.class,
                PlaylistTreeView.class,
                FullAudioItemTableView.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class MainControllerRandomPlaybackITConfiguration {

    final ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty =
            new SimpleObjectProperty<>(this, "selected playlist", Optional.empty());

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservableAudioLibrary audioLibrary(FXMusicLibrary musicLibrary) throws IOException {
        var library = musicLibrary.audioLibrary();
        // Pre-populate with one track so ALL_AUDIO_ITEMS tests have a non-empty pool
        Path tempFile = Files.createTempFile("mc-rand-cfg-", ".mp3");
        tempFile.toFile().deleteOnExit();
        try (InputStream in = Objects.requireNonNull(
                MainControllerRandomPlaybackITConfiguration.class
                        .getResourceAsStream("/testfiles/testeable.mp3"),
                "Missing test resource: /testfiles/testeable.mp3")) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        library.createFromFile(tempFile);
        return library;
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary,
                                                           ObservableAudioLibrary audioLibrary) throws IOException {
        var repo = musicLibrary.playlistHierarchy();
        // Pre-populate a playlist with the library's first item so PLAYLIST tests have a non-empty pool
        var items = List.copyOf(audioLibrary.getAudioItemsProperty());
        if (!items.isEmpty()) {
            var playlist = repo.createPlaylist("Test Playlist", items);
            selectedPlaylistProperty.set(Optional.of(playlist));
        }
        return repo;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return selectedPlaylistProperty;
    }

    @Bean
    public PlayerService playerService() {
        var service = mock(PlayerService.class);
        when(service.getPlayQueueList()).thenReturn(FXCollections.observableArrayList());
        when(service.getHistoryQueueList()).thenReturn(FXCollections.observableArrayList());
        when(service.currentTrack()).thenReturn(Optional.empty());
        when(service.getTotalDuration()).thenReturn(java.time.Duration.ZERO);
        return service;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> audioWaveformRepository() {
        return mock(AudioWaveformRepository.class);
    }

    @Bean
    public AlertFactory alertFactory() {
        return mock(AlertFactory.class);
    }

    @Bean
    public MediaImportService mediaImportService() {
        return mock(MediaImportService.class);
    }

    @Bean
    public ItunesImportWizard itunesImportWizard() {
        return mock(ItunesImportWizard.class);
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public KeyCombination.Modifier operativeSystemKeyModifier() {
        return KeyCombination.CONTROL_DOWN;
    }

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
