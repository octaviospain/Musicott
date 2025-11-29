package net.transgressoft.musicott.view;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.commons.event.CrudEvent;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.util.function.Consumer;

import static net.transgressoft.commons.music.audio.AudioItemTestFactory.createAlbum;
import static net.transgressoft.commons.music.audio.ImmutableArtist.of;
import static net.transgressoft.commons.music.audio.VirtualTestFileFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.matcher.control.ListViewMatchers.hasListCell;

@JavaFxSpringTest(classes = ArtistViewControllerTestConfiguration.class)
@SuppressWarnings("unchecked")
@Disabled
class ArtistViewControllerTest extends ApplicationTestBase<SplitPane> {

    @Autowired
    ObservableAudioLibrary audioRepository;

    @Autowired
    ListProperty<Artist> artistsProperty;

    @Autowired
    FxControllerAndView<ArtistViewController, SplitPane> artistViewAndController;

    Consumer<CrudEvent<Integer, ObservableAudioItem>> capturedConsumer;

    @Override
    protected SplitPane javaFxComponent() {
        return artistViewAndController.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        ArgumentCaptor<Consumer<CrudEvent<Integer, ObservableAudioItem>>> audioItemChangeSubscription = ArgumentCaptor.forClass(Consumer.class);
        verify(audioRepository).subscribe(audioItemChangeSubscription.capture());
        capturedConsumer = audioItemChangeSubscription.getValue();
    }

    @Test
    @DisplayName("Artist list should be updated on artists list property change")
    void testArtistViewController(FxRobot fxRobot) {
        assertThat(fxRobot.lookup("0 albums").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("0 tracks").tryQuery()).isPresent();

        var artist = of("Laurent Garnier & Rone");
        var albumArtist = of("Pete Tong");
//        var artistsInvolved = Set.of(
//                of("Laurent Garnier"),
//                of("Rone"),
//                of("Bonobo"),
//                albumArtist);

        var audioFile = createVirtualAudioFile(attributes -> {
            attributes.setTitle("Song title (Remix by Bonobo)");
            attributes.setArtist(artist);
            attributes.setAlbum(createAlbum("Techno Hits", albumArtist));
        });

        audioRepository.createFromFile(audioFile);

//        Platform.runLater(() -> {
//            artistsProperty.add(artist);
//            artistsProperty.addAll(artistsInvolved);
//        });

        WaitForAsyncUtils.waitForFxEvents();

        fxRobot.lookup("#artistsListView").match(hasListCell("Bonobo"));
        fxRobot.lookup("#artistsListView").match(hasListCell("Laurent Garnier & Rone"));
        fxRobot.lookup("#artistsListView").match(hasListCell("Laurent Garnier"));
        fxRobot.lookup("#artistsListView").match(hasListCell("Rone"));
        fxRobot.lookup("#artistsListView").match(hasListCell("Pete Tong"));
        assertThat(fxRobot.lookup("1 albums").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("1 tracks").tryQuery()).isPresent();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                ArtistViewController.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class ArtistViewControllerTestConfiguration {

    @Bean
    public SetProperty<Artist> artistsProperty() {
        return new SimpleSetProperty<>(FXCollections.observableSet());
    }

    @Bean
    public ObservableAudioLibrary audioRepository(ReadOnlySetProperty<Artist> artistsProperty) {
        var repository = mock(ObservableAudioLibrary.class);
        when(repository.artistsProperty()).thenReturn(artistsProperty);
        return repository;
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public FxWeaver fxWeaver(ConfigurableApplicationContext applicationContext) {
        return new SpringFxWeaver(applicationContext);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public <C, V extends Node> FxControllerAndView<C, V> controllerAndView(FxWeaver fxWeaver, InjectionPoint injectionPoint) {
        return new InjectionPointLazyFxControllerAndViewResolver(fxWeaver).resolve(injectionPoint);
    }
}