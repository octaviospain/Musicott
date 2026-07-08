package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.musicott.test.FxAudioItems;
import net.transgressoft.commons.fx.music.audio.ObservableAlbum;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.musicott.events.PlayItemEvent;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI regression tests for the Albums navigation view overlay drawer. Drives real {@link FxRobot}
 * interactions against a headless JavaFX scene: the drawer opens with the album's tracks, multi-disc
 * albums show disc section labels, clicking the dim region or pressing Esc closes the drawer, and
 * double-clicking a track row triggers playback.
 */
@JavaFxSpringTest(classes = AlbumViewControllerUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Album view drawer")
class AlbumViewControllerUIT extends ApplicationTestBase<StackPane> {

    @Autowired
    ListProperty<ObservableAlbum> albumsProperty;

    @Autowired
    FxControllerAndView<AlbumViewController, StackPane> albumViewAndController;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected StackPane javaFxComponent() {
        return albumViewAndController.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        // Each test reuses the same AlbumViewController singleton. If a previous test left the
        // drawer open (or its animation incomplete), reset the controller to a known-closed state
        // before mounting a fresh scene: null the drawerPane guard and strip any overlay children
        // from the StackPane so openDrawer() can always proceed from a clean initial state.
        // Force-remove extra children synchronously first; the animation's own setOnFinished removal
        // is harmless if the nodes are already gone from the list.
        AlbumViewController controller = albumViewAndController.getController();
        StackPane root = albumViewAndController.getView().get();
        Platform.runLater(() -> {
            controller.closeDrawer();
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
    @DisplayName("opening the drawer shows the album's tracks")
    void openingDrawerShowsAlbumTracks(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        assertThat(root.getChildren().size()).isGreaterThan(1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.lookup("Kiara").tryQuery().isPresent());
        waitForFxEvents();
        assertThat(fxRobot.lookup("Kiara").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("multi-disc album shows a Disc N label for each disc section in the drawer")
    void multiDiscAlbumShowsDiscLabelForEachDiscSectionInDrawer(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem disc1Track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 1);
        ObservableAudioItem disc2Track = audioItem("Kong", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo), 2);
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(disc1Track, disc2Track));

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.lookup("Disc 1").tryQuery().isPresent()
                        && fxRobot.lookup("Disc 2").tryQuery().isPresent());
        waitForFxEvents();

        assertThat(fxRobot.lookup("Disc 1").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("Disc 2").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("clicking the dim region closes the overlay drawer")
    void clickingDimRegionClosesOverlayDrawer(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(album));
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
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        fxRobot.press(KeyCode.ESCAPE).release(KeyCode.ESCAPE);

        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() == 1);
        waitForFxEvents();

        assertThat(root.getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("double-clicking a track row in the drawer triggers playback")
    void doubleClickingTrackRowInDrawerTriggersPlayback(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(album));
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

    @Test
    @DisplayName("opened album cell carries the selected pseudo-class; closed drawer clears it")
    void openedAlbumCellCarriesSelectedPseudoClass(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        AlbumViewController controller = albumViewAndController.getController();
        StackPane root = albumViewAndController.getView().get();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        assertThat(controller.selectedAlbum).isEqualTo(album);

        // Close the drawer and verify the selection is cleared.
        Platform.runLater(controller::closeDrawer);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() == 1);
        waitForFxEvents();

        assertThat(controller.selectedAlbum).isNull();
    }

    @Test
    @DisplayName("drawer width is at most 80% of root pane width after opening")
    void drawerWidthIsAtMostEightyPercentOfRootPaneWidth(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        AlbumViewController controller = albumViewAndController.getController();
        StackPane root = albumViewAndController.getView().get();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        // The drawer is the last child added; find it by style class.
        Region drawer = (Region) root.getChildren().stream()
                .filter(n -> n.getStyleClass().contains("album-drawer"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No album-drawer found"));

        double rootWidth = root.getWidth();
        double drawerWidth = drawer.getPrefWidth();

        // Drawer must not exceed 80% of root width, subject to a minimum-width floor for narrow
        // windows (plus a small rounding tolerance).
        assertThat(drawerWidth).isLessThanOrEqualTo(Math.max(rootWidth * 0.8, 280.0) + 1.0);
    }

    @Test
    @DisplayName("dragging the resize handle clamps the drawer width between the minimum and the maximum")
    void draggingResizeHandleClampsDrawerWidth(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        AlbumViewController controller = albumViewAndController.getController();
        StackPane root = albumViewAndController.getView().get();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();

        Region drawer = (Region) root.getChildren().stream()
                .filter(n -> n.getStyleClass().contains("album-drawer"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No album-drawer found"));
        Node handle = drawer.lookup(".drawer-resize-handle");
        assertThat(handle).as("resize handle present on the drawer").isNotNull();

        double maxWidth = Math.max(root.getWidth() * 0.8, 280.0);
        assertThat(drawer.getPrefWidth()).isCloseTo(maxWidth, within(1.0));

        // Press on the handle, then drag far to the right: the drawer narrows but clamps at the
        // minimum (210px) that keeps the row cover art visible. Firing synthetic events with explicit
        // scene coordinates is deterministic in headless Monocle, unlike robot-driven drags.
        Platform.runLater(() -> {
            handle.fireEvent(dragEvent(javafx.scene.input.MouseEvent.MOUSE_PRESSED, 1000));
            handle.fireEvent(dragEvent(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, 4000));
        });
        waitForFxEvents();
        assertThat(drawer.getPrefWidth()).isCloseTo(210.0, within(1.0));

        // Drag far to the left from the same press: the drawer widens but clamps back at the maximum.
        Platform.runLater(() -> handle.fireEvent(dragEvent(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, -5000)));
        waitForFxEvents();
        assertThat(drawer.getPrefWidth()).isCloseTo(maxWidth, within(1.0));
    }

    private static javafx.scene.input.MouseEvent dragEvent(
            javafx.event.EventType<javafx.scene.input.MouseEvent> type, double sceneX) {
        return new javafx.scene.input.MouseEvent(
                type,
                sceneX, 0, sceneX, 0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null);
    }

    @Test
    @DisplayName("disposes an open drawer when the view leaves the scene on a navigation switch")
    void disposesOpenDrawerWhenViewLeavesScene(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track));

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();
        assertThat(root.getChildren().size()).isGreaterThan(1);

        // A navigation-mode switch removes the albums layout from the scene graph, nulling this view's
        // scene while the drawer is still open. The overlay must be torn down (not left as a stuck,
        // non-dismissable dim), and the open guard cleared so the drawer reopens on return.
        javafx.scene.Scene scene = root.getScene();
        StackPane placeholder = new StackPane();
        Platform.runLater(() -> scene.setRoot(placeholder));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> root.getScene() == null && root.getChildren().size() == 1);
        waitForFxEvents();
        assertThat(root.getChildren()).hasSize(1);

        // Returning to the Albums view must reopen the drawer — the guard is no longer stuck.
        Platform.runLater(() -> scene.setRoot(root));
        waitForFxEvents();
        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        waitForFxEvents();
        assertThat(root.getChildren().size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("select all and deselect all operate on the open drawer's tracks")
    void selectAllAndDeselectAllOperateOnDrawerTracks(FxRobot fxRobot) throws Exception {
        Artist bonobo = of("Bonobo");
        ObservableAudioItem track1 = audioItem("Kiara", bonobo, "Black Sands", bonobo, 1, Set.of(bonobo));
        ObservableAudioItem track2 = audioItem("Kong", bonobo, "Black Sands", bonobo, 2, Set.of(bonobo));
        ObservableAlbum album = mockAlbum("Black Sands", bonobo, List.of(track1, track2));

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();

        Platform.runLater(() -> controller.openDrawer(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> root.getChildren().size() > 1);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> fxRobot.lookup("Kiara").tryQuery().isPresent());
        waitForFxEvents();

        // getSelectedTracks() iterates the drawer rows and each row's selected-items FX collection,
        // so every read of it must run on the FX thread — both the waitFor predicates and the
        // final assertions read a snapshot taken via queryFx.
        Platform.runLater(controller::selectAllTracks);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> queryFx(() -> controller.getSelectedTracks().size()) == 2);
        waitForFxEvents();
        assertThat(queryFx(() -> List.copyOf(controller.getSelectedTracks())))
                .containsExactlyInAnyOrder(track1, track2);

        Platform.runLater(controller::deselectAllTracks);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> queryFx(() -> controller.getSelectedTracks().isEmpty()));
        waitForFxEvents();
        assertThat(queryFx(() -> List.copyOf(controller.getSelectedTracks()))).isEmpty();
    }

    private static ObservableAlbum mockAlbum(String albumName, Artist artist, List<ObservableAudioItem> tracks) {
        ObservableAlbum album = mock(ObservableAlbum.class);
        when(album.getAlbumName()).thenReturn(albumName);
        when(album.getAlbumArtist()).thenReturn(artist);
        when(album.isCompilation()).thenReturn(false);
        when(album.getTracks()).thenReturn(tracks);
        when(album.compareTo(any())).thenReturn(0);
        @SuppressWarnings("unchecked")
        javafx.beans.property.ReadOnlyObjectProperty<Optional<javafx.scene.image.Image>> coverProp =
                (javafx.beans.property.ReadOnlyObjectProperty<Optional<javafx.scene.image.Image>>) mock(
                        javafx.beans.property.ReadOnlyObjectProperty.class);
        when(coverProp.get()).thenReturn(Optional.empty());
        when(album.getCoverProperty()).thenReturn(coverProp);
        return album;
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
                AlbumViewController.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class AlbumViewControllerUITConfiguration {

    @Bean
    public ListProperty<ObservableAlbum> albumsProperty() {
        return new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ObservableAudioLibrary audioRepository(ReadOnlyListProperty<ObservableAlbum> albumsProperty) {
        var repository = mock(ObservableAudioLibrary.class);
        when(repository.getAlbumsProperty()).thenReturn(albumsProperty);
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
