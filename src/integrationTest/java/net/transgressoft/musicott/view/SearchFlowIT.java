package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.property.ObjectProperty;
import javafx.collections.*;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.lirp.event.LirpEventPublisher;
import net.transgressoft.commons.fx.music.audio.ObservableAlbum;
import net.transgressoft.commons.fx.music.audio.ObservableArtistCatalog;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.AlbumDetails;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.search.SearchCoordinator;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.NavigationController;
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
import java.util.concurrent.TimeUnit;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;
import static org.testfx.util.WaitForAsyncUtils.waitFor;

/**
 * Integration test verifying that the global search field triggers live filtering across all
 * subscriber views — the audio item table ({@link FullAudioItemTableView}) and the artist list
 * ({@link ArtistViewController}) — through the {@link SearchCoordinator} async pipeline, and
 * that clearing the query restores each view to its full unfiltered state.
 *
 * <p>The real {@link SearchCoordinator} is wired so the test exercises the full production path:
 * nav-mode gating, off-thread ID computation, and FX-thread predicate application.
 * The coordinator is configured with zero debounce delay (see {@link SearchFlowITCoordinatorSupplier})
 * to eliminate timing races, while still exercising the full Dispatchers.Default → Dispatchers.JavaFx
 * coroutine thread-hop.
 *
 * <p>Each assertion follows the pattern:
 * <ol>
 *   <li>{@code Platform.runLater(() -> searchCoordinator.onQuery(...))} — submit the query on the FX thread</li>
 *   <li>{@code waitForFxEvents()} — drain the FX queue so the query runs and the new job is assigned</li>
 *   <li>{@code waitFor(5s, coordinator::isIdle)} — await job completion; the volatile {@code currentJob}
 *       field provides a happens-before guarantee so any subsequent read on the test thread sees the
 *       predicate that the FX thread wrote</li>
 *   <li>Directly assert collection sizes — safe because of the happens-before from step 3</li>
 * </ol>
 */
@JavaFxSpringTest(classes = {SearchFlowITConfiguration.class, SearchFlowITCoordinatorSupplier.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Search flow")
class SearchFlowIT extends ApplicationTestBase<SplitPane> {

    @Autowired
    SetProperty<ObservableArtistCatalog> artistCatalogsProperty;

    @Autowired
    FxControllerAndView<ArtistViewController, SplitPane> artistViewAndController;

    @Autowired
    FullAudioItemTableView audioItemTableView;

    @Autowired
    ObservableAudioLibrary audioRepository;

    @Autowired
    SearchCoordinator searchCoordinator;

    @Autowired
    ObjectProperty<NavigationController.NavigationMode> navigationModeProperty;

    @Override
    protected SplitPane javaFxComponent() {
        return artistViewAndController.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        // Reset filters, source items, and navigation mode between tests.
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
            audioItemTableView.setSourceItems(javafx.collections.FXCollections.observableArrayList());
            artistCatalogsProperty.clear();
            // onQuery("") cancels any in-flight job and enqueues a blank-reset coroutine on Default.
            searchCoordinator.onQuery("");
        });
        // Drain: ensures onQuery("") has run on the FX thread, assigning the new job to currentJob.
        waitForFxEvents();
        // Wait for the reset coroutine to complete its FX-thread applyMatchIds calls.
        try {
            waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("SearchCoordinator did not become idle within 5s in beforeEach", e);
        }
        // Extra drain rounds to flush cascading FX tasks (e.g. SetChangeListener → Platform.runLater
        // from ArtistViewController's reactive backing list).
        waitForFxEvents();
        waitForFxEvents();
        waitForFxEvents();
    }

    @AfterEach
    void afterEach() {
        // Cancel any in-flight search coroutine so no job leaks into subsequent tests or classes.
        Platform.runLater(() -> {
            artistCatalogsProperty.clear();
            searchCoordinator.onQuery("");
        });
        waitForFxEvents();
        try {
            waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("SearchCoordinator did not become idle within 5s in afterEach", e);
        }
        waitForFxEvents();
        waitForFxEvents();
        waitForFxEvents();
    }

    @Test
    @DisplayName("AudioItemTableViewBase filters its predicate when a qualifying query is submitted")
    void filtersAudioItemTablePredicateOnNonEmptyQuery() throws Exception {
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Apple", "Artist A", "Album A"),
                mockAudioItem("Banana", "Artist B", "Album B"),
                mockAudioItem("Cherry", "Artist C", "Album C")
        );

        Platform.runLater(() -> audioItemTableView.setSourceItems(items));
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(3);

        // Submit query and await quiescence; the volatile currentJob provides happens-before so
        // the assertion below reads the predicate written by the FX thread.
        Platform.runLater(() -> searchCoordinator.onQuery("App"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());

        assertThat(audioItemTableView.getItems()).hasSize(1);
        assertThat(audioItemTableView.getItems().get(0).getTitle()).isEqualTo("Apple");
    }

    @Test
    @DisplayName("ArtistViewController filters its predicate when a qualifying query is submitted")
    void filtersArtistListPredicateOnNonEmptyQuery(FxRobot fxRobot) throws Exception {
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ARTISTS);
            artistCatalogsProperty.clear();
            artistCatalogsProperty.add(mockCatalog(of("Aphex Twin")));
            artistCatalogsProperty.add(mockCatalog(of("Bonobo")));
            artistCatalogsProperty.add(mockCatalog(of("Caribou")));
        });
        waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<ObservableArtistCatalog> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(artistsListView.getItems()).hasSize(3);

        Platform.runLater(() -> searchCoordinator.onQuery("Aph"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());

        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getArtistName()).isEqualTo("Aphex Twin");
    }

    @Test
    @DisplayName("A single query filters every registered view at once so switching modes shows an already-filtered view")
    void singleQueryFiltersAllViewsSimultaneously(FxRobot fxRobot) throws Exception {
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Aphelion", "Artist A", "Album A"),
                mockAudioItem("Banana", "Artist B", "Album B")
        );
        Platform.runLater(() -> {
            audioItemTableView.setSourceItems(items);
            artistCatalogsProperty.clear();
            artistCatalogsProperty.add(mockCatalog(of("Aphex Twin")));
            artistCatalogsProperty.add(mockCatalog(of("Bonobo")));
        });
        waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<ObservableArtistCatalog> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(audioItemTableView.getItems()).hasSize(2);
        assertThat(artistsListView.getItems()).hasSize(2);

        // One query submitted without switching modes must filter BOTH views, so navigating to
        // either view afterward shows it already filtered.
        Platform.runLater(() -> searchCoordinator.onQuery("Aph"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());

        assertThat(audioItemTableView.getItems()).hasSize(1);
        assertThat(audioItemTableView.getItems().get(0).getTitle()).isEqualTo("Aphelion");
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getArtistName()).isEqualTo("Aphex Twin");
    }

    @Test
    @DisplayName("All views restore to full state when the query is cleared")
    void restoresAllViewsToFullStateOnClearedQuery(FxRobot fxRobot) throws Exception {
        // Seed audio table
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Apple", "Artist A", "Album A"),
                mockAudioItem("Banana", "Artist B", "Album B"),
                mockAudioItem("Cherry", "Artist C", "Album C")
        );
        Platform.runLater(() -> audioItemTableView.setSourceItems(items));
        waitForFxEvents();

        // Seed artist list
        Platform.runLater(() -> {
            artistCatalogsProperty.clear();
            artistCatalogsProperty.add(mockCatalog(of("Aphex Twin")));
            artistCatalogsProperty.add(mockCatalog(of("Bonobo")));
            artistCatalogsProperty.add(mockCatalog(of("Caribou")));
        });
        waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<ObservableArtistCatalog> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);

        // Filter the artist list (ARTISTS mode)
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ARTISTS);
            searchCoordinator.onQuery("Aph");
        });
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        assertThat(artistsListView.getItems()).hasSize(1);

        // Filter the audio table (ALL_AUDIO_ITEMS mode)
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
            searchCoordinator.onQuery("App");
        });
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        assertThat(audioItemTableView.getItems()).hasSize(1);

        // Clearing the query triggers immediate reset across ALL registered views
        Platform.runLater(() -> searchCoordinator.onQuery(""));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        waitForFxEvents();
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(3);
        assertThat(artistsListView.getItems()).hasSize(3);
    }

    @Test
    @DisplayName("AudioItemTableViewBase shows zero items when query matches nothing")
    void audioTableShowsZeroItemsWhenQueryMatchesNothing() throws Exception {
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Apple", "Artist A", "Album A"),
                mockAudioItem("Banana", "Artist B", "Album B")
        );
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
            audioItemTableView.setSourceItems(items);
        });
        waitForFxEvents();

        // A query that matches nothing should produce an empty table, not show all items
        Platform.runLater(() -> searchCoordinator.onQuery("zzqqxx"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());

        assertThat(audioItemTableView.getItems()).isEmpty();

        // Clearing the query must restore all items
        Platform.runLater(() -> searchCoordinator.onQuery(""));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("AudioItemTableViewBase shows only the matching track and hides non-matching tracks in ALL_AUDIO_ITEMS mode")
    void audioTableFiltersToExactlyOneMatchingTrack() throws Exception {
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Unique Track Title", "Artist A", "Album A"),
                mockAudioItem("Banana", "Artist B", "Album B"),
                mockAudioItem("Cherry", "Artist C", "Album C")
        );
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
            audioItemTableView.setSourceItems(items);
        });
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(3);

        Platform.runLater(() -> searchCoordinator.onQuery("Unique"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());

        assertThat(audioItemTableView.getItems()).hasSize(1);
        assertThat(audioItemTableView.getItems().get(0).getTitle()).isEqualTo("Unique Track Title");

        // Non-matching items must be absent
        boolean nonMatchingVisible = audioItemTableView.getItems().stream()
                .anyMatch(item -> !"Unique Track Title".equals(item.getTitle()));
        assertThat(nonMatchingVisible).isFalse();
    }

    @Test
    @DisplayName("ArtistViewController shows zero artists when query matches nothing")
    void artistViewShowsZeroArtistsWhenQueryMatchesNothing(FxRobot fxRobot) throws Exception {
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ARTISTS);
            artistCatalogsProperty.clear();
            artistCatalogsProperty.add(mockCatalog(of("Aphex Twin")));
            artistCatalogsProperty.add(mockCatalog(of("Bonobo")));
        });
        waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<ObservableArtistCatalog> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(artistsListView.getItems()).hasSize(2);

        // A query matching no artist should show zero results
        Platform.runLater(() -> searchCoordinator.onQuery("zzqqxx"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());

        assertThat(artistsListView.getItems()).isEmpty();

        // Clearing the query restores all artists
        Platform.runLater(() -> searchCoordinator.onQuery(""));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        waitForFxEvents();

        assertThat(artistsListView.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("AudioItemTableViewBase triggers search immediately with a single-character query")
    void singleCharQueryTriggersImmediateSearch() throws Exception {
        // Use artist names with clearly distinct first letters so a 1-char prefix unambiguously
        // selects exactly one item.
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(
                mockAudioItem("Track X", "Zeppelin", "Album Z"),
                mockAudioItem("Track Y", "Beethoven", "Album B")
        );
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
            audioItemTableView.setSourceItems(items);
        });
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(2);

        // "Z" (single char) matches only "Zeppelin" — single-char search must fire, not be treated as a reset.
        // waitForFxEvents() ensures onQuery("Z") ran on FX so the new job is assigned before isIdle() is polled.
        Platform.runLater(() -> searchCoordinator.onQuery("Z"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());

        assertThat(audioItemTableView.getItems()).hasSize(1);
        assertThat(audioItemTableView.getItems().get(0).getTitle()).isEqualTo("Track X");

        // Beethoven's track must be absent
        boolean beethovenVisible = audioItemTableView.getItems().stream()
                .anyMatch(item -> "Track Y".equals(item.getTitle()));
        assertThat(beethovenVisible).isFalse();

        // Clearing the query resets to all items
        Platform.runLater(() -> searchCoordinator.onQuery(""));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        waitForFxEvents();

        assertThat(audioItemTableView.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("ArtistViewController matches artists by track title, album, and label when the artist catalog is loaded")
    @SuppressWarnings("unchecked")
    void matchesArtistsByTrackTitleAlbumAndLabelWhenArtistCatalogIsLoaded(FxRobot fxRobot) throws Exception {
        Platform.runLater(() -> artistCatalogsProperty.clear());
        waitForFxEvents();

        var aphexTwin = of("Aphex Twin");
        var bonobo = of("Bonobo");

        // Add artists FIRST without any catalog stubbed so the auto-select path triggers
        // selectedArtistListener with an empty catalog and skips ArtistAlbumListRow construction
        // (which would otherwise fail against the bare AlbumSet/ObservableArtistCatalog mocks below).
        Platform.runLater(() -> {
            navigationModeProperty.set(NavigationController.NavigationMode.ARTISTS);
            artistCatalogsProperty.add(mockCatalog(aphexTwin));
            artistCatalogsProperty.add(mockCatalog(bonobo));
        });
        waitForFxEvents();

        @SuppressWarnings("unchecked")
        ListView<ObservableArtistCatalog> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(artistsListView.getItems()).hasSize(2);

        // Wire a populated catalog for aphexTwin so filterArtistsByQuery's catalog-walk branch
        // exercises the loaded path. selectedArtistListener won't refire because the selected
        // artist hasn't changed identity.
        var album = AudioItemTestFactory.createAlbum("Selected Ambient Works 85-92", aphexTwin, false, null,
                Label.of("Warp Records"));
        var item = mock(ObservableAudioItem.class);
        when(item.getTitle()).thenReturn("Xtal");
        when(item.getArtist()).thenReturn(aphexTwin);
        when(item.getAlbum()).thenReturn(album);

        var albumsSetProperty = new javafx.beans.property.SimpleSetProperty<>(
                javafx.collections.FXCollections.observableSet(album));
        var audioItemsListProperty = new javafx.beans.property.SimpleListProperty<>(
                javafx.collections.FXCollections.observableArrayList(item));
        var catalog = mock(ObservableArtistCatalog.class);
        when(catalog.getAlbumsProperty()).thenReturn(albumsSetProperty);
        when(catalog.albumAudioItemsProperty(album.getName())).thenReturn(audioItemsListProperty);
        // doReturn instead of when().thenReturn() because the production signature is
        // Optional<out ObservableArtistCatalog> (Kotlin covariant) which the typed thenReturn rejects.
        doReturn(java.util.Optional.of(catalog)).when(audioRepository).getArtistCatalog(aphexTwin);
        doReturn(java.util.Optional.empty()).when(audioRepository).getArtistCatalog(bonobo);

        // Title-only query: matches Aphex Twin via track title; Bonobo has no matching catalog item.
        Platform.runLater(() -> searchCoordinator.onQuery("Xtal"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getArtistName()).isEqualTo("Aphex Twin");

        // Label-only query: confirms the label-match branch in artistMatchesQuery.
        Platform.runLater(() -> searchCoordinator.onQuery("Warp"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getArtistName()).isEqualTo("Aphex Twin");

        // Album-only query.
        Platform.runLater(() -> searchCoordinator.onQuery("Ambient Works"));
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getArtistName()).isEqualTo("Aphex Twin");

        // Reset stubs back to empty so other tests in the class don't inherit a populated catalog
        // (Spring context is singleton-per-class — mock state leaks otherwise).
        doReturn(java.util.Optional.empty()).when(audioRepository).getArtistCatalog(aphexTwin);
        doReturn(java.util.Optional.empty()).when(audioRepository).getArtistCatalog(bonobo);
        Platform.runLater(() -> {
            artistCatalogsProperty.clear();
            searchCoordinator.onQuery("");
        });
        waitForFxEvents();
        waitFor(5, TimeUnit.SECONDS, () -> searchCoordinator.isIdle());
        waitForFxEvents();
    }

    private static ObservableArtistCatalog mockCatalog(Artist artist) {
        var catalog = mock(ObservableArtistCatalog.class);
        when(catalog.getArtistName()).thenReturn(artist.getName());
        when(catalog.getArtist()).thenReturn(artist);
        return catalog;
    }

    private static int nextId = 1;

    /**
     * Creates a mock {@link ObservableAudioItem} suitable for audio table predicate testing.
     * Stubs only the fields examined by {@code AudioItemTableViewBase.audioItemContainsQuery},
     * and assigns a unique ID so that ID-based predicate filtering is deterministic.
     *
     * <p>All album metadata is fully controlled via mocks to prevent random values generated by
     * {@code AudioItemTestFactory} from contaminating search-match assertions (e.g. a randomly
     * generated label whose name happens to contain the single-char search query).
     */
    private ObservableAudioItem mockAudioItem(String title, String artistName, String albumName) {
        var item = mock(ObservableAudioItem.class);
        when(item.getId()).thenReturn(nextId++);
        when(item.getTitle()).thenReturn(title);

        var artist = mock(Artist.class);
        when(artist.getName()).thenReturn(artistName);
        when(item.getArtist()).thenReturn(artist);

        // Mock AlbumDetails directly so label/albumArtist/year are deterministic and do not
        // contain random strings that could accidentally match the search query under test.
        var album = mock(AlbumDetails.class);
        when(album.getName()).thenReturn(albumName);
        var albumArtistObj = mock(Artist.class);
        when(albumArtistObj.getName()).thenReturn(artistName);
        when(album.getAlbumArtist()).thenReturn(albumArtistObj);
        when(album.getLabel()).thenReturn(null);  // no label → label branch in audioItemContainsQuery returns false
        when(item.getAlbum()).thenReturn(album);

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
    public SetProperty<net.transgressoft.commons.fx.music.audio.ObservableArtistCatalog> artistCatalogsProperty() {
        return new SimpleSetProperty<>(FXCollections.observableSet());
    }

    @Bean
    public ListProperty<ObservableAlbum> albumsProperty() {
        return new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ObservableAudioLibrary audioRepository(
            ReadOnlySetProperty<net.transgressoft.commons.fx.music.audio.ObservableArtistCatalog> artistCatalogsProperty,
            ReadOnlyListProperty<ObservableAlbum> albumsProperty) {
        var repository = mock(ObservableAudioLibrary.class);
        when(repository.getArtistCatalogsProperty()).thenReturn(artistCatalogsProperty);
        when(repository.getAlbumsProperty()).thenReturn(albumsProperty);
        when(repository.getArtistCatalogPublisher()).thenReturn(mock(LirpEventPublisher.class));
        return repository;
    }

    @Bean
    public ObjectProperty<NavigationController.NavigationMode> navigationModeProperty() {
        return new javafx.beans.property.SimpleObjectProperty<>(NavigationController.NavigationMode.ALL_AUDIO_ITEMS);
    }

    @Bean
    public NavigationController navigationController(ObjectProperty<NavigationController.NavigationMode> navigationModeProperty) {
        var mock = mock(NavigationController.class);
        when(mock.navigationModeProperty()).thenReturn(navigationModeProperty);
        return mock;
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
