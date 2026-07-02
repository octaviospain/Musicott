package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableGenreIndex;
import net.transgressoft.commons.music.audio.Genre;
import net.transgressoft.musicott.events.SearchTextTypedEvent;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link GenreViewController}, verifying the genres grid backing by
 * {@link ObservableAudioLibrary#getGenreIndexesProperty()} and reactive propagation of set-changes
 * into the grid's item list using the Spring-integrated JavaFX test pattern.
 */
@JavaFxSpringTest(classes = GenreViewControllerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GenreViewControllerIT extends ApplicationTestBase<StackPane> {

    @Autowired
    SetProperty<ObservableGenreIndex> genreIndexesProperty;

    @Autowired
    FxControllerAndView<GenreViewController, StackPane> genreViewAndController;

    @Override
    protected StackPane javaFxComponent() {
        return genreViewAndController.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        // The GenreViewController singleton and its genreIndexesProperty are shared across methods
        // (context is dirtied only after the class). Reset the catalog before each test so methods
        // stay independent regardless of execution order.
        Platform.runLater(() -> {
            genreIndexesProperty.clear();
            // Also reset any search predicate a prior test may have left active on this shared
            // singleton, so an earlier assertion failure cannot leak a narrowed grid into later tests.
            genreViewAndController.getController().searchTextTypedEvent(new SearchTextTypedEvent("", this));
        });
        waitForFxEvents();
        super.beforeEach();
    }

    @Test
    @DisplayName("GenreViewController grid is backed by genreIndexesProperty from the repository")
    void gridIsBackedByGenreIndexesProperty() throws Exception {
        ObservableGenreIndex genre = mockGenre("Techno");
        Platform.runLater(() -> genreIndexesProperty.add(genre));
        waitForFxEvents();

        StackPane root = genreViewAndController.getView().get();
        assertThat(root.getChildren()).isNotEmpty();
        // The first child is the GridView added in initialize()
        assertThat(root.getChildren().get(0)).isInstanceOf(GridView.class);

        @SuppressWarnings("unchecked")
        GridView<ObservableGenreIndex> grid = (GridView<ObservableGenreIndex>) root.getChildren().get(0);
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();

        assertThat(grid.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("GenreViewController propagates genreIndexesProperty additions into the grid backing list")
    void propagatesGenreIndexesPropertyAdditionsIntoGrid() throws Exception {
        Platform.runLater(() -> genreIndexesProperty.clear());
        waitForFxEvents();

        StackPane root = genreViewAndController.getView().get();
        @SuppressWarnings("unchecked")
        GridView<ObservableGenreIndex> grid = (GridView<ObservableGenreIndex>) root.getChildren().get(0);

        ObservableGenreIndex genre1 = mockGenre("Techno");
        ObservableGenreIndex genre2 = mockGenre("Jazz");

        Platform.runLater(() -> genreIndexesProperty.add(genre1));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();
        assertThat(grid.getItems()).hasSize(1);

        Platform.runLater(() -> genreIndexesProperty.add(genre2));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 2);
        waitForFxEvents();
        assertThat(grid.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("GenreViewController propagates genreIndexesProperty removals into the grid backing list")
    void propagatesGenreIndexesPropertyRemovalsFromGrid() throws Exception {
        Platform.runLater(() -> genreIndexesProperty.clear());
        waitForFxEvents();

        StackPane root = genreViewAndController.getView().get();
        @SuppressWarnings("unchecked")
        GridView<ObservableGenreIndex> grid = (GridView<ObservableGenreIndex>) root.getChildren().get(0);

        ObservableGenreIndex genre = mockGenre("Techno");

        Platform.runLater(() -> genreIndexesProperty.add(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();

        Platform.runLater(() -> genreIndexesProperty.remove(genre));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().isEmpty());
        waitForFxEvents();

        assertThat(grid.getItems()).isEmpty();
    }

    @Test
    @DisplayName("GenreViewController filters the grid to genres with a track matching the search query")
    void filtersGridToGenresMatchingSearchQuery() throws Exception {
        Platform.runLater(() -> genreIndexesProperty.clear());
        waitForFxEvents();

        StackPane root = genreViewAndController.getView().get();
        @SuppressWarnings("unchecked")
        GridView<ObservableGenreIndex> grid = (GridView<ObservableGenreIndex>) root.getChildren().get(0);

        ObservableGenreIndex techno = mockGenre("Techno", List.of(mockTrack("Strobe")));
        ObservableGenreIndex jazz = mockGenre("Jazz", List.of(mockTrack("Take Five")));

        Platform.runLater(() -> {
            genreIndexesProperty.add(techno);
            genreIndexesProperty.add(jazz);
        });
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 2);
        waitForFxEvents();

        GenreViewController controller = genreViewAndController.getController();

        // A query matching only the Techno track's title narrows the grid to that genre.
        Platform.runLater(() -> controller.searchTextTypedEvent(new SearchTextTypedEvent("strobe", this)));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 1);
        waitForFxEvents();
        assertThat(grid.getItems()).containsExactly(techno);

        // Clearing the query restores every genre.
        Platform.runLater(() -> controller.searchTextTypedEvent(new SearchTextTypedEvent("", this)));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> grid.getItems().size() == 2);
        waitForFxEvents();
        assertThat(grid.getItems()).hasSize(2);
    }

    private static ObservableGenreIndex mockGenre(String genreName) {
        return mockGenre(genreName, List.of());
    }

    private static ObservableGenreIndex mockGenre(String genreName, List<ObservableAudioItem> tracks) {
        ObservableGenreIndex genre = mock(ObservableGenreIndex.class);
        // Genre is a sealed class; use Genre.Custom directly instead of mocking it
        Genre.Custom genreValue = new Genre.Custom(genreName);
        @SuppressWarnings("unchecked")
        ReadOnlyObjectProperty<Genre> genreProp =
                (ReadOnlyObjectProperty<Genre>) mock(ReadOnlyObjectProperty.class);
        when(genreProp.get()).thenReturn(genreValue);
        when(genre.getGenreProperty()).thenReturn(genreProp);

        ReadOnlyIntegerProperty sizeProp = mock(ReadOnlyIntegerProperty.class);
        when(sizeProp.get()).thenReturn(tracks.size());
        when(genre.getSizeProperty()).thenReturn(sizeProp);

        // Use a real ReadOnlyListWrapper so iteration in resolveCoverPool and the search filter works
        ObservableList<ObservableAudioItem> items = FXCollections.observableArrayList(tracks);
        ReadOnlyListProperty<ObservableAudioItem> tracksProp = new ReadOnlyListWrapper<>(items).getReadOnlyProperty();
        when(genre.getTracksProperty()).thenReturn(tracksProp);

        // Comparable required for SortedList
        when(genre.compareTo(any())).thenReturn(0);
        return genre;
    }

    /** A track carrying only a title; the search matcher null-guards the other fields. */
    private static ObservableAudioItem mockTrack(String title) {
        ObservableAudioItem track = mock(ObservableAudioItem.class);
        when(track.getTitle()).thenReturn(title);
        // Grid cells observe the cover property to build the hover pool; give it a real empty one.
        ReadOnlyObjectProperty<java.util.Optional<javafx.scene.image.Image>> coverProp =
                new ReadOnlyObjectWrapper<>(java.util.Optional.<javafx.scene.image.Image>empty()).getReadOnlyProperty();
        when(track.getCoverImageProperty()).thenReturn(coverProp);
        return track;
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                GenreViewController.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class GenreViewControllerITConfiguration {

    @Bean
    public SetProperty<ObservableGenreIndex> genreIndexesProperty() {
        return new SimpleSetProperty<>(FXCollections.observableSet());
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ObservableAudioLibrary audioRepository(ReadOnlySetProperty<ObservableGenreIndex> genreIndexesProperty) {
        var repository = mock(ObservableAudioLibrary.class);
        when(repository.getGenreIndexesProperty()).thenReturn(genreIndexesProperty);
        return repository;
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
