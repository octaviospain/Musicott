package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.lirp.event.LirpEventPublisher;
import net.transgressoft.commons.fx.music.audio.ObservableArtistCatalog;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Album;
import net.transgressoft.commons.music.audio.AlbumSet;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.events.SearchTextTypedEvent;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import org.junit.jupiter.api.*;
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

import java.nio.file.Path;

import static net.transgressoft.commons.music.audio.ImmutableArtist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test verifying that the global search field triggers live filtering across all
 * subscriber views — the audio item table ({@link FullAudioItemTableView}) and the artist list
 * ({@link ArtistViewController}) — and that clearing the query restores each view to its
 * full unfiltered state.
 *
 * <p>Events are dispatched through the real Spring {@link ApplicationEventPublisher} so the
 * test exercises the production wire: the multicaster, the {@code @EventListener} method
 * resolution, and the bean scope of each subscriber. Direct method invocation would mask
 * scope bugs (e.g. a prototype-scoped table view never registering with the multicaster).
 */
@JavaFxSpringTest(classes = SearchFlowITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Search flow")
class SearchFlowIT extends ApplicationTestBase<SplitPane> {

    @Autowired
    SetProperty<Artist> artistsProperty;

    @Autowired
    FxControllerAndView<ArtistViewController, SplitPane> artistViewAndController;

    @Autowired
    FullAudioItemTableView audioItemTableView;

    @Autowired
    ObservableAudioLibrary audioRepository;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected SplitPane javaFxComponent() {
        return artistViewAndController.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
    }

    @Test
    @DisplayName("AudioItemTableViewBase filters its predicate when SearchTextTypedEvent fires with a non-empty query")
    void filtersAudioItemTablePredicateOnNonEmptyQuery(FxRobot fxRobot) {
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Apple", "Artist A", "Album A"),
                mockAudioItem("Banana", "Artist B", "Album B"),
                mockAudioItem("Cherry", "Artist C", "Album C")
        );

        Platform.runLater(() -> audioItemTableView.setSourceItems(items));
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(3);

        // Publish through the real Spring multicaster so the test catches scope/registration bugs
        // that would silently drop the event before reaching the table's @EventListener.
        Platform.runLater(() -> applicationEventPublisher.publishEvent(new SearchTextTypedEvent("App", this)));
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(1);
        assertThat(audioItemTableView.getItems().get(0).getTitle()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("ArtistViewController filters its predicate when SearchTextTypedEvent fires with a non-empty query")
    void filtersArtistListPredicateOnNonEmptyQuery(FxRobot fxRobot) {
        Platform.runLater(() -> {
            artistsProperty.clear();
            // Reset predicate to unfiltered state first
            applicationEventPublisher.publishEvent(new SearchTextTypedEvent("", this));
        });
        waitForFxEvents();

        Platform.runLater(() -> {
            artistsProperty.add(of("Aphex Twin"));
            artistsProperty.add(of("Bonobo"));
            artistsProperty.add(of("Caribou"));
        });
        waitForFxEvents();

        ListView<Artist> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(artistsListView.getItems()).hasSize(3);

        Platform.runLater(() -> applicationEventPublisher.publishEvent(new SearchTextTypedEvent("Aph", this)));
        waitForFxEvents();

        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getName()).isEqualTo("Aphex Twin");
    }

    @Test
    @DisplayName("All three views restore to full state when SearchTextTypedEvent fires with empty query")
    void restoresAllViewsToFullStateOnEmptyQuery(FxRobot fxRobot) {
        // --- Seed audio table ---
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Apple", "Artist A", "Album A"),
                mockAudioItem("Banana", "Artist B", "Album B"),
                mockAudioItem("Cherry", "Artist C", "Album C")
        );
        Platform.runLater(() -> {
            audioItemTableView.setSourceItems(items);
            applicationEventPublisher.publishEvent(new SearchTextTypedEvent("App", this));
        });
        waitForFxEvents();
        assertThat(audioItemTableView.getItems()).hasSize(1);

        // --- Seed artist list ---
        Platform.runLater(() -> {
            artistsProperty.clear();
            applicationEventPublisher.publishEvent(new SearchTextTypedEvent("", this));
        });
        waitForFxEvents();

        Platform.runLater(() -> {
            artistsProperty.add(of("Aphex Twin"));
            artistsProperty.add(of("Bonobo"));
            artistsProperty.add(of("Caribou"));
        });
        waitForFxEvents();

        Platform.runLater(() -> applicationEventPublisher.publishEvent(new SearchTextTypedEvent("Aph", this)));
        waitForFxEvents();

        ListView<Artist> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(artistsListView.getItems()).hasSize(1);

        // --- Clear the query — a single SearchTextTypedEvent("") on the bus reaches both subscribers. ---
        Platform.runLater(() -> applicationEventPublisher.publishEvent(new SearchTextTypedEvent("", this)));
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(3);
        assertThat(artistsListView.getItems()).hasSize(3);
    }

    @Test
    @DisplayName("ArtistViewController matches artists by track title, album, and label when the artist catalog is loaded")
    @SuppressWarnings("unchecked")
    void matchesArtistsByTrackTitleAlbumAndLabelWhenArtistCatalogIsLoaded(FxRobot fxRobot) {
        Platform.runLater(() -> {
            artistsProperty.clear();
            applicationEventPublisher.publishEvent(new SearchTextTypedEvent("", this));
        });
        waitForFxEvents();

        var aphexTwin = of("Aphex Twin");
        var bonobo = of("Bonobo");

        // Add artists FIRST without any catalog stubbed so the auto-select path triggers
        // selectedArtistListener with an empty catalog and skips ArtistAlbumListRow construction
        // (which would otherwise fail against the bare AlbumSet/ObservableArtistCatalog mocks below).
        Platform.runLater(() -> {
            artistsProperty.add(aphexTwin);
            artistsProperty.add(bonobo);
        });
        waitForFxEvents();

        ListView<Artist> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(artistsListView.getItems()).hasSize(2);

        // Now wire a populated catalog for aphexTwin so filterArtistsByQuery's catalog-walk branch
        // exercises the loaded path. selectedArtistListener won't refire because the selected
        // artist hasn't changed identity.
        var labelMock = mock(Label.class);
        when(labelMock.getName()).thenReturn("Warp Records");
        var album = mock(Album.class);
        when(album.getName()).thenReturn("Selected Ambient Works 85-92");
        when(album.getAlbumArtist()).thenReturn(aphexTwin);
        when(album.getLabel()).thenReturn(labelMock);
        var item = mock(ObservableAudioItem.class);
        when(item.getTitle()).thenReturn("Xtal");
        when(item.getArtist()).thenReturn(aphexTwin);
        when(item.getAlbum()).thenReturn(album);

        // AlbumSet extends List; stream() default impl returns null on a Mockito mock, so stub it
        // with thenAnswer so the predicate can re-iterate across multiple search calls.
        var albumSet = mock(AlbumSet.class);
        when(albumSet.stream()).thenAnswer(inv -> java.util.stream.Stream.of(item));
        var catalog = mock(ObservableArtistCatalog.class);
        when(catalog.getAlbums()).thenReturn(java.util.Set.of(albumSet));
        // doReturn instead of when().thenReturn() because the production signature is
        // Optional<out ObservableArtistCatalog> (Kotlin covariant) which the typed thenReturn rejects.
        doReturn(java.util.Optional.of(catalog)).when(audioRepository).getArtistCatalog(aphexTwin);
        doReturn(java.util.Optional.empty()).when(audioRepository).getArtistCatalog(bonobo);

        // Title-only query: matches Aphex Twin via track title; Bonobo has no matching catalog item.
        Platform.runLater(() -> applicationEventPublisher.publishEvent(new SearchTextTypedEvent("Xtal", this)));
        waitForFxEvents();
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getName()).isEqualTo("Aphex Twin");

        // Label-only query: confirms the new label-match branch in artistMatchesQuery.
        Platform.runLater(() -> applicationEventPublisher.publishEvent(new SearchTextTypedEvent("Warp", this)));
        waitForFxEvents();
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getName()).isEqualTo("Aphex Twin");

        // Album-only query.
        Platform.runLater(() -> applicationEventPublisher.publishEvent(new SearchTextTypedEvent("Ambient Works", this)));
        waitForFxEvents();
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getName()).isEqualTo("Aphex Twin");

        // Reset stubs back to empty so other tests in the class don't inherit a populated catalog
        // (Spring context is singleton-per-class — mock state leaks otherwise).
        doReturn(java.util.Optional.empty()).when(audioRepository).getArtistCatalog(aphexTwin);
        doReturn(java.util.Optional.empty()).when(audioRepository).getArtistCatalog(bonobo);
        Platform.runLater(() -> {
            applicationEventPublisher.publishEvent(new SearchTextTypedEvent("", this));
            artistsProperty.clear();
        });
        waitForFxEvents();
    }

    /**
     * Creates a mock {@link ObservableAudioItem} suitable for audio table predicate testing.
     * Stubs only the fields examined by {@code AudioItemTableViewBase.audioItemContainsQuery}.
     */
    private ObservableAudioItem mockAudioItem(String title, String artistName, String albumName) {
        var item = mock(ObservableAudioItem.class);
        when(item.getTitle()).thenReturn(title);

        var artist = mock(Artist.class);
        when(artist.getName()).thenReturn(artistName);
        when(item.getArtist()).thenReturn(artist);

        var albumArtist = mock(Artist.class);
        when(albumArtist.getName()).thenReturn(artistName);

        var album = mock(Album.class);
        when(album.getName()).thenReturn(albumName);
        when(album.getAlbumArtist()).thenReturn(albumArtist);
        when(item.getAlbum()).thenReturn(album);

        // Stub path and cover for any defensive null checks in other parts of the table internals
        when(item.getPath()).thenReturn(Path.of("/mock/path/" + title + ".mp3"));

        return item;
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                ArtistViewController.class,
                SimpleAudioItemTableView.class,
                FullAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class SearchFlowITConfiguration {

    @Bean
    public SetProperty<Artist> artistsProperty() {
        return new SimpleSetProperty<>(FXCollections.observableSet());
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ObservableAudioLibrary audioRepository(ReadOnlySetProperty<Artist> artistsProperty) {
        var repository = mock(ObservableAudioLibrary.class);
        when(repository.getArtistsProperty()).thenReturn(artistsProperty);
        when(repository.getArtistCatalogPublisher()).thenReturn(mock(LirpEventPublisher.class));
        return repository;
    }

    // No @Bean for ApplicationEventPublisher — the test relies on the real Spring multicaster so
    // the @EventListener delivery path is fully exercised. ApplicationContext itself implements
    // ApplicationEventPublisher and is autowirable.

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
