package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Album;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.table.TrackQueueRow;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
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
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI test verifying visible row ordering in the play queue popover and cover image
 * rendering in queue rows.
 */
@JavaFxSpringTest(classes = PlayQueueOrderingUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Play queue ordering and cover rendering")
class PlayQueueOrderingUIT extends ApplicationTestBase<GridPane> {

    @Autowired
    FxControllerAndView<PlayerController, GridPane> playerControllerAndView;

    @Autowired
    PlayerService playerService;

    @Override
    protected GridPane javaFxComponent() {
        return playerControllerAndView.getView().get();
    }

    @BeforeEach
    @Override
    protected void beforeEach() {
        super.beforeEach();
        Platform.runLater(() -> playerService.clearQueue());
        waitForFxEvents();
    }

    /**
     * Creates a mock {@link ObservableAudioItem} with all properties required by
     * {@link TrackQueueRow} and {@code JavaFxPlayer.isPlayable} stubbed to playable MP3 defaults.
     */
    ObservableAudioItem mockPlayableAudioItem(String title) {
        ObservableAudioItem item = mock(ObservableAudioItem.class);
        when(item.getTitleProperty()).thenReturn(new SimpleStringProperty(title));
        Artist artist = mock(Artist.class);
        when(artist.getName()).thenReturn("Test Artist");
        when(item.getArtistProperty()).thenReturn(new SimpleObjectProperty<>(artist));
        Album album = mock(Album.class);
        when(album.getName()).thenReturn("Test Album");
        when(item.getAlbumProperty()).thenReturn(new SimpleObjectProperty<>(album));
        when(item.getCoverImageProperty()).thenReturn(new SimpleObjectProperty<>(Optional.empty()));
        // Required by JavaFxPlayer.isPlayable: extension must be a supported type,
        // encoding and encoder must be null to avoid the Apple/iTunes exclusion.
        when(item.getExtension()).thenReturn("mp3");
        when(item.getEncoding()).thenReturn(null);
        when(item.getEncoder()).thenReturn(null);
        return item;
    }

    @Test
    @DisplayName("renders next-up track at the bottom of the queue list view")
    void rendersNextUpTrackAtBottomOfQueueListView(FxRobot robot) {
        ObservableAudioItem itemA = mockPlayableAudioItem("Song A");
        ObservableAudioItem itemB = mockPlayableAudioItem("Song B");
        ObservableAudioItem itemC = mockPlayableAudioItem("Song C");

        Platform.runLater(() -> playerService.addToQueue(List.of(itemA, itemB, itemC)));
        WaitForAsyncUtils.waitForFxEvents();

        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        Platform.runLater(playQueueButton::fire);
        WaitForAsyncUtils.waitForFxEvents();

        ListView<TrackQueueRow> queuesListView = robot.lookup("#queuesListView").queryListView();
        List<TrackQueueRow> items = new ArrayList<>(queuesListView.getItems());
        assertThat(items).hasSize(3);
        // Inverted storage: index 0 is the farthest-out track (plays last), size-1 is next-up.
        // The first selected item (A) is what the user wants played first, so addToQueue([A,B,C])
        // produces storage [C, B, A]: A at size-1 (next-up, bottom of the popover) and C at index 0
        // (farthest-out, top). Display follows storage 1:1.
        assertThat(items.get(items.size() - 1).getTrack()).isSameAs(itemA);
        assertThat(items.get(0).getTrack()).isSameAs(itemC);
    }

    @Test
    @DisplayName("renders the track cover image in queue rows")
    void rendersTrackCoverImageInQueueRows(FxRobot robot) {
        Image testCover = new Image(getClass().getResourceAsStream("/images/default-cover-image-queue.png"));
        ObservableAudioItem itemWithCover = mockPlayableAudioItem("With Cover");
        when(itemWithCover.getCoverImageProperty())
                .thenReturn(new SimpleObjectProperty<>(Optional.of(testCover)));

        ObservableAudioItem itemWithoutCover = mockPlayableAudioItem("No Cover");
        when(itemWithoutCover.getCoverImageProperty())
                .thenReturn(new SimpleObjectProperty<>(Optional.empty()));

        Platform.runLater(() -> playerService.addToQueue(List.of(itemWithCover, itemWithoutCover)));
        WaitForAsyncUtils.waitForFxEvents();

        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        Platform.runLater(playQueueButton::fire);
        WaitForAsyncUtils.waitForFxEvents();

        ListView<TrackQueueRow> queuesListView = robot.lookup("#queuesListView").queryListView();
        Image defaultCover = ApplicationImage.DEFAULT_COVER.get();
        for (TrackQueueRow row : queuesListView.getItems()) {
            // Use per-row lookup to avoid ambiguity when multiple coverImage nodes exist
            ImageView iv = (ImageView) row.lookup("#coverImage");
            assertThat(iv.getImage()).isNotNull();
            if (row.getTrack() == itemWithCover) {
                assertThat(iv.getImage()).isSameAs(testCover);
            } else {
                assertThat(iv.getImage()).isSameAs(defaultCover);
            }
        }
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                PlayerController.class,
                PlayQueueController.class
        })
})
class PlayQueueOrderingUITConfiguration {

    final SimpleBooleanProperty emptyLibraryProperty = new SimpleBooleanProperty(false);

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public PlayerService playerService(ApplicationEventPublisher publisher) {
        return new PlayerService(publisher);   // REAL instance — UIT drives real list mutations
    }

    @Bean
    @SuppressWarnings("unchecked")
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> audioWaveformRepository() {
        return mock(AudioWaveformRepository.class);
    }

    @Bean
    public ObservableAudioLibrary audioLibrary() {
        ObservableAudioLibrary library = mock(ObservableAudioLibrary.class);
        when(library.getEmptyLibraryProperty()).thenReturn(emptyLibraryProperty);
        return library;
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
