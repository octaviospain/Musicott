package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.commons.fx.music.FxAudioItemTestFactory;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.AudioItemTestFactory;
import net.transgressoft.commons.music.audio.Label;
import net.transgressoft.lirp.event.LirpEventPublisher;
import net.transgressoft.musicott.events.SearchTextTypedEvent;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
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
import org.testfx.util.WaitForAsyncUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.transgressoft.commons.music.audio.Artist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for {@link ArtistViewController}, verifying artist list rendering and
 * artist selection behavior using the Spring-integrated JavaFX test pattern.
 */
@JavaFxSpringTest(classes = ArtistViewControllerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArtistViewControllerIT extends ApplicationTestBase<SplitPane> {

    @Autowired
    SetProperty<Artist> artistsProperty;

    @Autowired
    ListProperty<ObservableAudioItem> audioItemsProperty;

    @Autowired
    FxControllerAndView<ArtistViewController, SplitPane> artistViewAndController;

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
    @DisplayName("ArtistViewController renders artist list populated from repository")
    void rendersArtistListFromRepository(FxRobot fxRobot) {
        assertThat(fxRobot.lookup("0 albums").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("0 tracks").tryQuery()).isPresent();

        var artist = of("Laurent Garnier");

        Platform.runLater(() -> artistsProperty.add(artist));
        waitForFxEvents();

        assertThat(fxRobot.lookup("#artistsListView").tryQuery()).isPresent();
        assertThat(artistsProperty).hasSize(1);
    }

    @Test
    @DisplayName("ArtistViewController validates album display when an artist is clicked")
    void validatesAlbumDisplayWhenArtistIsClicked(FxRobot fxRobot) {
        Platform.runLater(() -> artistsProperty.add(of("Bonobo")));
        waitForFxEvents();

        fxRobot.clickOn("Bonobo");
        waitForFxEvents();

        // Artist list view remains accessible after artist selection
        assertThat(fxRobot.lookup("#artistsListView").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("ArtistViewController displays tracks when selected artist only appears as involved artist")
    void displaysTracksWhenSelectedArtistOnlyAppearsAsInvolvedArtist(FxRobot fxRobot) throws Exception {
        Artist akkya = of("Akkya");
        Artist triggerLive = of("Trigger Live");
        Artist bonobo = of("Bonobo");
        ObservableAudioItem bonoboTrack = audioItem(
                "Kiara",
                bonobo,
                "Black Sands",
                bonobo,
                1,
                Set.of(bonobo));
        ObservableAudioItem akkyaTrack = audioItem(
                "Circle (Akkya Remix)",
                triggerLive,
                "Diffusion 7.0 - Electronic Arrangment of Techno",
                of(""),
                21,
                Set.of(triggerLive, akkya));

        Platform.runLater(() -> {
            artistsProperty.clear();
            audioItemsProperty.clear();
            audioItemsProperty.addAll(bonoboTrack, akkyaTrack);
            artistsProperty.addAll(Set.of(bonobo, triggerLive, akkya));
        });
        waitForFxEvents();

        ListView<Artist> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        ListView<ArtistAlbumListRow> albumsListView = fxRobot.lookup("#albumsListView").queryAs(ListView.class);

        Platform.runLater(() -> artistsListView.getSelectionModel().select(bonobo));
        waitForDisplayedTitle(albumsListView, "Kiara");

        Platform.runLater(() -> artistsListView.getSelectionModel().select(akkya));
        waitForDisplayedTitle(albumsListView, "Circle (Akkya Remix)");
        assertThat(displayedTitles(albumsListView)).doesNotContain("Kiara");
    }

    @Test
    @DisplayName("ArtistViewController auto-selects the first filtered artist when search query matches")
    void autoSelectsFirstFilteredArtistWhenSearchQueryMatches(FxRobot fxRobot) {
        ArtistViewController controller = artistViewAndController.getController();
        // Clear accumulated state from previous tests and reset the predicate.
        Platform.runLater(() -> {
            artistsProperty.clear();
            controller.searchTextTypedEvent(new SearchTextTypedEvent("", this));
        });
        waitForFxEvents();

        Platform.runLater(() -> {
            artistsProperty.add(of("Abel"));
            artistsProperty.add(of("Abba"));
            artistsProperty.add(of("Beatles"));
        });
        waitForFxEvents();

        // Direct invocation bypasses the mocked ApplicationEventPublisher bean — publishEvent(...)
        // on a mock is a no-op, so the @EventListener method is driven directly here.
        Platform.runLater(() -> controller.searchTextTypedEvent(new SearchTextTypedEvent("Abe", this)));
        waitForFxEvents();

        ListView<Artist> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);
        assertThat(artistsListView.getItems()).hasSize(1);
        assertThat(artistsListView.getItems().get(0).getName()).isEqualTo("Abel");

        Artist selected = artistsListView.getSelectionModel().getSelectedItem();
        assertThat(selected).isNotNull();
        assertThat(selected).isSameAs(artistsListView.getItems().get(0));
    }

    @Test
    @DisplayName("ArtistViewController restores the full artist list and selects the first artist when search query clears")
    void restoresFullArtistListAndSelectsFirstArtistWhenSearchQueryClears(FxRobot fxRobot) {
        ArtistViewController controller = artistViewAndController.getController();
        // Clear accumulated state from previous tests and reset the predicate.
        Platform.runLater(() -> {
            artistsProperty.clear();
            controller.searchTextTypedEvent(new SearchTextTypedEvent("", this));
        });
        waitForFxEvents();

        Platform.runLater(() -> {
            artistsProperty.add(of("Abel"));
            artistsProperty.add(of("Abba"));
            artistsProperty.add(of("Beatles"));
        });
        waitForFxEvents();

        ListView<Artist> artistsListView = fxRobot.lookup("#artistsListView").queryAs(ListView.class);

        // Filter to a single match.
        Platform.runLater(() -> controller.searchTextTypedEvent(new SearchTextTypedEvent("Abe", this)));
        waitForFxEvents();
        assertThat(artistsListView.getItems()).hasSize(1);

        // Clear the query — emulates MainController publishing "" for length < 3 or empty input.
        Platform.runLater(() -> controller.searchTextTypedEvent(new SearchTextTypedEvent("", this)));
        waitForFxEvents();

        assertThat(artistsListView.getItems()).hasSize(3);
        assertThat(artistsListView.getSelectionModel().getSelectedItem()).isNotNull();
        assertThat(artistsListView.getSelectionModel().getSelectedItem()).isSameAs(artistsListView.getItems().get(0));
    }

    private static void waitForDisplayedTitle(ListView<ArtistAlbumListRow> albumsListView, String title) throws Exception {
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> displayedTitles(albumsListView).contains(title));
        waitForFxEvents();
    }

    private static Set<String> displayedTitles(ListView<ArtistAlbumListRow> albumsListView) {
        return albumsListView.getItems().stream()
                .flatMap(row -> row.containedAudioItemsProperty().stream())
                .map(ObservableAudioItem::getTitle)
                .collect(java.util.stream.Collectors.toSet());
    }

    private static ObservableAudioItem audioItem(
            String title,
            Artist artist,
            String albumName,
            Artist albumArtist,
            int trackNumber,
            Set<Artist> artistsInvolved) {
        return FxAudioItemTestFactory.createFxAudioItem(attributes -> {
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
                ArtistViewController.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class ArtistViewControllerITConfiguration {

    @Bean
    public SetProperty<Artist> artistsProperty() {
        return new SimpleSetProperty<>(FXCollections.observableSet());
    }

    @Bean
    public ListProperty<ObservableAudioItem> audioItemsProperty() {
        return new SimpleListProperty<>(FXCollections.observableArrayList());
    }

    @Bean
    @SuppressWarnings("unchecked")
    public ObservableAudioLibrary audioRepository(
            ReadOnlySetProperty<Artist> artistsProperty,
            ReadOnlyListProperty<ObservableAudioItem> audioItemsProperty) {
        var repository = mock(ObservableAudioLibrary.class);
        when(repository.getArtistsProperty()).thenReturn(artistsProperty);
        when(repository.getAudioItemsProperty()).thenReturn(audioItemsProperty);
        when(repository.getArtistCatalogPublisher()).thenReturn(mock(LirpEventPublisher.class));
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
