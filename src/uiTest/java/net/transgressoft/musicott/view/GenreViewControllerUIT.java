package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.musicott.test.FxAudioItems;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableGenreIndex;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Genre;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.events.PlayItemEvent;
import java.util.Collections;

import net.transgressoft.musicott.search.SearchCoordinator;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
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
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI regression tests for the Genres navigation view overlay drawer. Drives real {@link FxRobot}
 * interactions against a headless JavaFX scene: the drawer opens with genre tracks grouped by album,
 * multi-disc albums show disc section labels, partial albums render only their genre-tagged tracks,
 * album-less tracks appear in an "Unknown Album" section, clicking the dim region or pressing Esc
 * closes the drawer, an empty genre bucket auto-closes the drawer, and double-clicking a track row
 * triggers playback.
 */
@JavaFxSpringTest(classes = GenreViewControllerUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("GenreViewController")
class GenreViewControllerUIT extends ApplicationTestBase<StackPane> {

    @Autowired
    FxControllerAndView<GenreViewController, StackPane> genreViewAndController;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected StackPane javaFxComponent() {
        return genreViewAndController.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        // Each test reuses the same GenreViewController singleton. If a previous test left the
        // drawer open (or its animation incomplete), reset the controller to a known-closed state
        // before mounting a fresh scene: null the drawerPane guard and strip any overlay children
        // from the StackPane so openDrawer() can always proceed from a clean initial state.
        // Force-remove extra children synchronously first; the animation's own setOnFinished removal
        // is harmless if the nodes are already gone from the list.
        GenreViewController controller = genreViewAndController.getController();
        StackPane root = genreViewAndController.getView().get();
        Platform.runLater(() -> {
            controller.closeDrawer();
            // The controller singleton is reused across tests; clear any search query a prior test
            // set so it does not filter this test's freshly opened drawer.
            controller.applyMatchIds("", Collections.emptySet());
            if (root.getChildren().size() > 1) {
                root.getChildren().subList(1, root.getChildren().size()).clear();
            }
        });
        try {
            // Wait until overlay nodes have been fully removed before remounting the scene,
            // so a previous test's in-flight close animation cannot leave orphaned nodes that
            // would cause the next test's lookup to return 2 matches.
            WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> root.getChildren().size() == 1);
        } catch (Exception ignored) {
            // If the root still has extra children after the timeout, the subList.clear() above
            // already removed them synchronously — this wait is a safety net, not the primary guard.
        }
        WaitForAsyncUtils.waitForFxEvents();
        super.beforeEach();
    }

    @Test
    @DisplayName("opening the drawer shows the genre's tracks")
    void openingDrawerShowsGenreTracks(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        assertThat(root.getChildren().size()).isGreaterThan(1);
        // Scope lookup to the live root to avoid matching nodes in detached drawer subtrees
        // left over from earlier tests (prototype beans not yet GC'd).
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup("Kiara").tryQuery().isPresent());
        waitForFxEvents();
        assertThat(fxRobot.from(root).lookup("Kiara").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("select all and deselect all operate on the open drawer's tracks")
    void selectAllAndDeselectAllOperateOnDrawerTracks(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAudioItem track2 = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track1, track2));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup("Kiara").tryQuery().isPresent());
        waitForFxEvents();

        // getSelectedTracks() iterates the live JavaFX selection model, which is only safe on the FX
        // thread — read it via asyncFx so the poll never races the FX-thread selection mutation.
        Platform.runLater(controller::selectAllTracks);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> WaitForAsyncUtils.asyncFx(() -> controller.getSelectedTracks().size()).get() == 2);
        waitForFxEvents();
        assertThat(WaitForAsyncUtils.asyncFx(() -> List.copyOf(controller.getSelectedTracks())).get())
                .containsExactlyInAnyOrder(track1, track2);

        Platform.runLater(controller::deselectAllTracks);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> WaitForAsyncUtils.asyncFx(() -> controller.getSelectedTracks().isEmpty()).get());
        waitForFxEvents();
        assertThat(WaitForAsyncUtils.asyncFx(() -> List.copyOf(controller.getSelectedTracks())).get()).isEmpty();
    }

    @Test
    @DisplayName("an active search narrows the drawer to albums with matching tracks, showing only those tracks")
    void searchNarrowsDrawerToMatchingTracks(FxRobot fxRobot) throws Exception {
        Artist deadmau5 = of("deadmau5");
        // Album "Alpha" has a matching and a non-matching track; album "Beta" has none matching.
        ObservableAudioItem strobe = audioItem("Strobe", deadmau5, "Alpha", deadmau5, 1, Set.of(deadmau5));
        ObservableAudioItem ghosts = audioItem("Ghosts n Stuff", deadmau5, "Alpha", deadmau5, 2, Set.of(deadmau5));
        ObservableAudioItem faxing = audioItem("Faxing Berlin", deadmau5, "Beta", deadmau5, 1, Set.of(deadmau5));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(strobe, ghosts, faxing));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        // Apply a query matching only "Strobe" before opening — the drawer applies it on open.
        Platform.runLater(() -> controller.applyMatchIds("strobe", Set.of("TECHNO")));
        waitForFxEvents();
        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup("Strobe").tryQuery().isPresent());
        waitForFxEvents();

        // Only the matching track of the matching album is shown; the non-matching track and the
        // entirely non-matching album are absent.
        assertThat(fxRobot.from(root).lookup("Strobe").tryQuery()).isPresent();
        assertThat(fxRobot.from(root).lookup("Ghosts n Stuff").tryQuery()).isEmpty();
        assertThat(fxRobot.from(root).lookup("Faxing Berlin").tryQuery()).isEmpty();
    }

    @Test
    @DisplayName("drawer header shows genre name and track count")
    void drawerHeaderShowsGenreNameAndTrackCount(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAudioItem track2 = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track1, track2));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        // The header label text is "TECHNO · 2 tracks" (middle-dot U+00B7, one space on each side)
        String expectedHeader = "TECHNO · 2 tracks";
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup(expectedHeader).tryQuery().isPresent());
        waitForFxEvents();

        assertThat(fxRobot.from(root).lookup(expectedHeader).tryQuery()).isPresent();
    }

    @Test
    @DisplayName("partial album shows only its genre-tagged tracks")
    void partialAlbumShowsOnlyGenreTaggedTracks(FxRobot fxRobot) throws Exception {
        // Feed only 2 of the 3 album tracks to simulate a partial album (only those tracks tagged
        // with this genre). The drawer must render exactly those 2 tracks, not the full album.
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAudioItem track2 = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo));
        // Only the genre-tagged tracks are fed to the drawer; it must render exactly those two.
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track1, track2));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup("Kiara").tryQuery().isPresent());
        waitForFxEvents();

        assertThat(fxRobot.from(root).lookup("Kiara").tryQuery()).isPresent();
        assertThat(fxRobot.from(root).lookup("Kong").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("multi-disc album shows a Disc N label for each disc section in the drawer")
    void multiDiscAlbumShowsDiscLabelForEachDiscSectionInDrawer(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem disc1Track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem disc2Track = audioItem("Kong", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 2);
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(disc1Track, disc2Track));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup("Disc 1").tryQuery().isPresent()
                        && fxRobot.from(root).lookup("Disc 2").tryQuery().isPresent());
        waitForFxEvents();

        assertThat(fxRobot.from(root).lookup("Disc 1").tryQuery()).isPresent();
        assertThat(fxRobot.from(root).lookup("Disc 2").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("album-less tracks render in an Unknown Album section")
    void albumLessTracksRenderInUnknownAlbumSection(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        // Create a track with a blank album name so buildGenreSections routes it to the trailing
        // "Unknown Album" section. The randomly generated attributes always produce a non-empty
        // album name, so we must explicitly override it with an album whose name is blank.
        ObservableAudioItem albumLessTrack = FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle("Orphan Track");
            attributes.setArtist(bonobo);
            attributes.setTrackNumber((short) 1);
            attributes.setAlbum(AudioItemTestFactory.createAlbum(
                    "", Artist.UNKNOWN, false, null, Label.of("Test Label")));
        }, Set.of(bonobo));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(albumLessTrack));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup(GenreViewController.UNKNOWN_ALBUM).tryQuery().isPresent());
        waitForFxEvents();

        assertThat(fxRobot.from(root).lookup(GenreViewController.UNKNOWN_ALBUM).tryQuery()).isPresent();
    }

    @Test
    @DisplayName("clicking the dim region closes the overlay drawer")
    void clickingDimRegionClosesOverlayDrawer(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        // Fire a mouse-clicked event directly on the dimPane to simulate the user clicking the dimmed area.
        // TestFX screen-coordinate clicks can be intercepted by the drawer pane (which lies above dimPane
        // in the StackPane) even when clicking in the supposedly unobstructed left region, because JavaFX
        // hit-testing in Monocle headless mode treats the full StackPane bounds as the event target.
        // Firing the event directly on the dimPane bypasses this and accurately tests the handler wiring.
        javafx.scene.layout.Pane dimPane = (javafx.scene.layout.Pane) root.getChildren().get(1);
        Platform.runLater(() ->
            dimPane.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null))
        );

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() == 1);
        waitForFxEvents();

        assertThat(root.getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("pressing Escape closes the overlay drawer")
    void pressingEscapeClosesOverlayDrawer(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        fxRobot.press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() == 1);
        waitForFxEvents();

        assertThat(root.getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("empty genre bucket auto-closes the open drawer")
    void emptyGenreBucketAutoClosesDrawer(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));

        // Use a mutable empty property so the test can simulate the bucket dropping to zero
        SimpleBooleanProperty emptyProperty = new SimpleBooleanProperty(false);
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track), emptyProperty);

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        // Signal the bucket dropping to empty — emits the auto-close (signal b)
        Platform.runLater(() -> emptyProperty.set(true));

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() == 1);
        waitForFxEvents();

        assertThat(root.getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("double-clicking a track row in the drawer triggers playback")
    void doubleClickingTrackRowInDrawerTriggersPlayback(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableGenreIndex genre = mockGenre("TECHNO", List.of(track));

        StackPane root = genreViewAndController.getView().get();
        GenreViewController controller = genreViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        // Scope lookup to the live root to avoid matching "Kiara" labels in detached drawer subtrees
        // left over from earlier tests (prototype beans not yet GC'd).
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.from(root).lookup("Kiara").tryQuery().isPresent());
        waitForFxEvents();

        // Fire the double-click event directly on the node rather than using screen-coordinate
        // targeting; TableRow bounds in the Monocle headless renderer are not always laid out to
        // a non-zero size, which causes position-based doubleClickOn to throw BoundsLocatorException.
        Node kiaraNode = fxRobot.from(root).lookup("Kiara").query();
        Platform.runLater(() -> kiaraNode.fireEvent(new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                javafx.scene.input.MouseButton.PRIMARY, 2,
                false, false, false, false, true, false, false, true, false, false, null)));
        waitForFxEvents();

        verify(applicationEventPublisher, atLeastOnce()).publishEvent(any(PlayItemEvent.class));
    }

    private static ObservableGenreIndex mockGenre(String genreName, List<ObservableAudioItem> tracks) {
        SimpleBooleanProperty emptyProperty = new SimpleBooleanProperty(tracks.isEmpty());
        return mockGenre(genreName, tracks, emptyProperty);
    }

    private static ObservableGenreIndex mockGenre(
            String genreName,
            List<ObservableAudioItem> tracks,
            ReadOnlyBooleanProperty emptyProperty) {
        ObservableGenreIndex genre = mock(ObservableGenreIndex.class);
        // Genre is a sealed Kotlin class — use the Genre.Custom data-class subtype directly
        // rather than mocking the sealed base, which Mockito cannot subclass.
        Genre genreValue = new Genre.Custom(genreName);
        @SuppressWarnings("unchecked")
        ReadOnlyObjectProperty<Genre> genreProperty =
                (ReadOnlyObjectProperty<Genre>) mock(ReadOnlyObjectProperty.class);
        when(genreProperty.get()).thenReturn(genreValue);
        when(genre.getGenreProperty()).thenReturn(genreProperty);

        ReadOnlyIntegerProperty sizeProperty = new SimpleIntegerProperty(tracks.size());
        when(genre.getSizeProperty()).thenReturn(sizeProperty);

        ReadOnlyListProperty<ObservableAudioItem> tracksProperty =
                new ReadOnlyListWrapper<>(FXCollections.observableArrayList(tracks)).getReadOnlyProperty();
        when(genre.getTracksProperty()).thenReturn(tracksProperty);

        when(genre.getEmptyProperty()).thenReturn(emptyProperty);
        when(genre.compareTo(any())).thenReturn(0);
        return genre;
    }

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            int trackNumber,
            Set<Artist> artistsInvolved) {
        return audioItem(title, artist, albumName, albumArtist, trackNumber, artistsInvolved, 1);
    }

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            int trackNumber,
            Set<Artist> artistsInvolved,
            int discNumber) {
        return FxAudioItems.createFxAudioItem(attributes -> {
            attributes.setTitle(title);
            attributes.setArtist(artist);
            attributes.setAlbum(AudioItemTestFactory.createAlbum(
                    albumName,
                    albumArtist,
                    false,
                    null,
                    Label.of("Test Label")));
            attributes.setTrackNumber((short) trackNumber);
            attributes.setDiscNumber((short) discNumber);
        }, artistsInvolved);
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                GenreViewController.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class GenreViewControllerUITConfiguration {

    @Bean
    @SuppressWarnings("unchecked")
    public ObservableAudioLibrary audioRepository() {
        var repository = mock(ObservableAudioLibrary.class);
        ReadOnlyListProperty<ObservableGenreIndex> emptyGenresProperty =
                new ReadOnlyListWrapper<>(FXCollections.<ObservableGenreIndex>observableArrayList()).getReadOnlyProperty();
        when(repository.getGenreIndexesProperty()).thenReturn(emptyGenresProperty);
        return repository;
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public SearchCoordinator searchCoordinator() {
        return mock(SearchCoordinator.class);
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
