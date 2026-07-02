package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.FxAudioItemTestFactory;
import net.transgressoft.commons.fx.music.audio.ObservableArtistCatalog;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitFor;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration tests verifying that the "Add to play queue" context action in ARTISTS navigation
 * mode correctly routes through {@link ArtistViewController#getSelectedTracks()} rather than
 * the empty main-table selection.
 */
@JavaFxSpringTest(classes = ArtistViewAddTracksITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Artist view add-tracks routing")
class ArtistViewAddTracksIT extends ApplicationTestBase<BorderPane> {

    @Autowired
    FxControllerAndView<MainController, BorderPane> mainControllerAndView;

    @Autowired
    NavigationController navigationController;

    @Autowired
    ArtistViewController artistViewController;

    @Autowired
    ObservableAudioLibrary audioLibrary;

    @Override
    protected BorderPane javaFxComponent() {
        return mainControllerAndView.getView().get();
    }

    @BeforeEach
    @Override
    protected void beforeEach() {
        super.beforeEach();
    }

    @Test
    @DisplayName("add-to-play-queue context action in ARTISTS mode uses the artist-view selection, not the empty main-table selection")
    void addToPlayQueueInArtistsModeUsesArtistViewSelection() throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = FxAudioItemTestFactory.createFxAudioItem(attributes -> {
            attributes.setTitle("Kiara");
            attributes.setArtist(bonobo);
            attributes.setAlbum(AudioItemTestFactory.createAlbum("Black Sands", bonobo));
            attributes.setTrackNumber((short) 1);
            attributes.setDiscNumber((short) 1);
        }, Set.of(bonobo));

        @SuppressWarnings("unchecked")
        ObservableList<ObservableAudioItem> libraryItems =
                (ObservableList<ObservableAudioItem>) audioLibrary.getAudioItemsProperty();
        Platform.runLater(() -> libraryItems.add(track));
        waitForFxEvents();

        // Switch to ARTISTS mode
        setNavigationMode(NavigationController.NavigationMode.ARTISTS);

        // The artistsListView now holds ObservableArtistCatalog items. Since the track was added
        // directly to the audio items list (bypassing the library's catalog machinery), the catalog
        // property is empty. Drive artist selection and album-row construction via reflection so the
        // album rows are built from the audioItemsProperty path (which contains the track).
        ObservableArtistCatalog bonoboMockCatalog = mock(ObservableArtistCatalog.class);
        when(bonoboMockCatalog.getArtistName()).thenReturn("Bonobo");
        when(bonoboMockCatalog.getArtist()).thenReturn(bonobo);

        Object artistViewTarget = AopUtils.isAopProxy(artistViewController)
                ? AopTestUtils.getTargetObject(artistViewController)
                : artistViewController;
        @SuppressWarnings("unchecked")
        var selectedArtistProp = (ObjectProperty<Optional<ObservableArtistCatalog>>)
                ReflectionTestUtils.getField(artistViewTarget, "selectedArtistProperty");
        Platform.runLater(() -> {
            selectedArtistProp.set(Optional.of(bonoboMockCatalog));
            ReflectionTestUtils.invokeMethod(artistViewTarget, "refreshAlbumRowsForArtist", bonobo);
        });

        @SuppressWarnings("unchecked")
        ListView<ArtistAlbumListRow> albumsListView = lookup("#albumsListView").queryAs(ListView.class);
        waitFor(5, TimeUnit.SECONDS, () -> !albumsListView.getItems().isEmpty());
        waitForFxEvents();

        // Select all tracks in the first album row's embedded table
        ArtistAlbumListRow albumRow = albumsListView.getItems().get(0);
        Platform.runLater(albumRow::selectAllAudioItems);
        waitForFxEvents();

        ObservableList<ObservableAudioItem> artistSelection = artistViewController.getSelectedTracks();
        assertThat(artistSelection).isNotEmpty();

        // Retrieve the private audioItemContextMenu from MainController via reflection
        MainController controller = mainControllerAndView.getController();
        Object audioItemContextMenu = ReflectionTestUtils.getField(controller, "audioItemContextMenu");
        assertThat(audioItemContextMenu).isNotNull();

        // Invoke show() with an empty main-table selection. In ARTISTS mode the context menu must
        // substitute the artist-view selection internally before any menu item action fires.
        ObservableList<ObservableAudioItem> emptyMainTableSelection = FXCollections.emptyObservableList();
        Platform.runLater(() -> ReflectionTestUtils.invokeMethod(
                audioItemContextMenu, "show",
                emptyMainTableSelection, javaFxComponent(), 0.0, 0.0));
        waitForFxEvents();

        // Read the resolved 'selection' field — it must equal the artist-view selection, not the
        // empty main-table selection that was passed to show().
        @SuppressWarnings("unchecked")
        ObservableList<ObservableAudioItem> resolvedSelection =
                (ObservableList<ObservableAudioItem>) ReflectionTestUtils.getField(audioItemContextMenu, "selection");

        assertThat(resolvedSelection)
                .as("context menu selection in ARTISTS mode must equal the artist-view selection")
                .containsExactlyInAnyOrderElementsOf(artistSelection);
    }

    @SuppressWarnings("unchecked")
    void setNavigationMode(NavigationController.NavigationMode mode) {
        Object target = AopUtils.isAopProxy(navigationController)
                ? AopTestUtils.getTargetObject(navigationController)
                : navigationController;
        var navProp = (javafx.beans.property.ObjectProperty<NavigationController.NavigationMode>)
                ReflectionTestUtils.getField(target, "navigationModeProperty");
        Platform.runLater(() -> navProp.set(mode));
        waitForFxEvents();
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
                PlaylistTreeView.class,
                FullAudioItemTableView.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class ArtistViewAddTracksITConfiguration {

    final ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty =
            new SimpleObjectProperty<>(this, "selected playlist", Optional.empty());

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservableAudioLibrary audioLibrary(FXMusicLibrary musicLibrary) throws IOException {
        return musicLibrary.audioLibrary();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary,
                                                          ObservableAudioLibrary audioLibrary) throws IOException {
        return musicLibrary.playlistHierarchy();
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
    public javafx.application.Application.Parameters applicationParameters() {
        return mock(javafx.application.Application.Parameters.class);
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
