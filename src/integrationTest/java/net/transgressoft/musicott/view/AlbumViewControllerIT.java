package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
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
import java.util.Collections;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import org.controlsfx.control.GridView;
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
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.transgressoft.commons.music.audio.Artist.of;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ALBUMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link AlbumViewController}, verifying the albums grid backing by
 * {@link ObservableAudioLibrary#getAlbumsProperty()} and reactive propagation of set-changes
 * into the grid's item list using the Spring-integrated JavaFX test pattern.
 */
@JavaFxSpringTest(classes = AlbumViewControllerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AlbumViewControllerIT extends ApplicationTestBase<StackPane> {

    @Autowired
    ListProperty<ObservableAlbum> albumsProperty;

    @Autowired
    FxControllerAndView<AlbumViewController, StackPane> albumViewAndController;

    @Override
    protected StackPane javaFxComponent() {
        return albumViewAndController.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        // The AlbumViewController singleton and its albumsProperty are shared across methods
        // (context is dirtied only after the class). Reset the catalog and clear any active search
        // filter before each test so methods stay independent regardless of execution order.
        Platform.runLater(() -> {
            albumsProperty.clear();
            albumViewAndController.getController().applyMatchIds("", Collections.emptySet());
        });
        waitForFxEvents();
        super.beforeEach();
    }

    @Test
    @DisplayName("AlbumViewController grid is backed by albumsProperty from the repository")
    void gridIsBackedByAlbumsProperty() throws Exception {
        assertThat(ALBUMS).isNotNull();
        assertThat(ALBUMS.toString()).isEqualTo("Albums");

        ObservableAlbum album = mockAlbum("Black Sands", of("Bonobo"));
        Platform.runLater(() -> albumsProperty.add(album));
        waitForFxEvents();

        StackPane root = albumViewAndController.getView().get();
        assertThat(root.getChildren()).isNotEmpty();
        // The first child is the GridView added in initialize()
        assertThat(root.getChildren().get(0)).isInstanceOf(GridView.class);

        @SuppressWarnings("unchecked")
        GridView<ObservableAlbum> grid = (GridView<ObservableAlbum>) root.getChildren().get(0);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();

        assertThat(grid.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("AlbumViewController propagates albumsProperty additions into the grid backing list")
    void propagatesAlbumsPropertyAdditionsIntoGrid() throws Exception {
        Platform.runLater(() -> albumsProperty.clear());
        waitForFxEvents();

        StackPane root = albumViewAndController.getView().get();
        @SuppressWarnings("unchecked")
        GridView<ObservableAlbum> grid = (GridView<ObservableAlbum>) root.getChildren().get(0);

        ObservableAlbum album1 = mockAlbum("Black Sands", of("Bonobo"));
        ObservableAlbum album2 = mockAlbum("Melt!", of("Can"));

        Platform.runLater(() -> albumsProperty.add(album1));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();
        assertThat(grid.getItems()).hasSize(1);

        Platform.runLater(() -> albumsProperty.add(album2));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 2);
        waitForFxEvents();
        assertThat(grid.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("AlbumViewController propagates albumsProperty removals into the grid backing list")
    void propagatesAlbumsPropertyRemovalsFromGrid() throws Exception {
        Platform.runLater(() -> albumsProperty.clear());
        waitForFxEvents();

        StackPane root = albumViewAndController.getView().get();
        @SuppressWarnings("unchecked")
        GridView<ObservableAlbum> grid = (GridView<ObservableAlbum>) root.getChildren().get(0);

        ObservableAlbum album = mockAlbum("Black Sands", of("Bonobo"));

        Platform.runLater(() -> albumsProperty.add(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();

        Platform.runLater(() -> albumsProperty.remove(album));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().isEmpty());
        waitForFxEvents();

        assertThat(grid.getItems()).isEmpty();
    }

    @Test
    @DisplayName("AlbumViewController filters the grid to albums with a track matching the search query")
    void filtersGridToAlbumsMatchingSearchQuery() throws Exception {
        Platform.runLater(() -> albumsProperty.clear());
        waitForFxEvents();

        StackPane root = albumViewAndController.getView().get();
        AlbumViewController controller = albumViewAndController.getController();
        @SuppressWarnings("unchecked")
        GridView<ObservableAlbum> grid = (GridView<ObservableAlbum>) root.getChildren().get(0);

        ObservableAlbum blackSands = mockAlbum("Black Sands", of("Bonobo"));
        ObservableAlbum melt = mockAlbum("Melt!", of("Can"));
        Platform.runLater(() -> {
            albumsProperty.add(blackSands);
            albumsProperty.add(melt);
        });
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 2);
        waitForFxEvents();

        // Query matches only the "Black Sands" album (by its album name); "Melt!" has no matching track.
        Platform.runLater(() -> controller.applyMatchIds("black sands", Set.of("Black Sands")));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();
        // containsExactly walks the grid's FX-backed list; snapshot it on the FX thread to avoid
        // racing the filter predicate's concurrent mutation.
        assertThat(queryFx(() -> List.copyOf(grid.getItems()))).containsExactly(blackSands);

        // Clearing the query restores every album.
        Platform.runLater(() -> controller.applyMatchIds("", Collections.emptySet()));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 2);
        waitForFxEvents();
        assertThat(grid.getItems()).hasSize(2);
    }

    private static ObservableAlbum mockAlbum(String albumName, Artist artist) {
        ObservableAlbum album = mock(ObservableAlbum.class);
        when(album.getAlbumName()).thenReturn(albumName);
        when(album.getAlbumArtist()).thenReturn(artist);
        when(album.isCompilation()).thenReturn(false);
        when(album.getTracks()).thenReturn(List.of(audioItem("Track 1", artist, albumName, artist, 1, Set.of(artist))));
        // Comparable required for SortedList
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
            attributes.setDiscNumber((short) 1);
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
class AlbumViewControllerITConfiguration {

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
    public net.transgressoft.musicott.search.SearchCoordinator searchCoordinator() {
        return mock(net.transgressoft.musicott.search.SearchCoordinator.class);
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
