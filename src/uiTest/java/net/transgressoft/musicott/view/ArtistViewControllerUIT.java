package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.lirp.event.LirpEventPublisher;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.ArtistViewTestFixtures;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
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

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static net.transgressoft.commons.music.audio.ImmutableArtist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI regression tests for artist-list selection in the Artist view.
 */
@JavaFxSpringTest(classes = ArtistViewControllerUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Artist view selection")
class ArtistViewControllerUIT extends ApplicationTestBase<SplitPane> {

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

    @Test
    @DisplayName("clicking an involved-only artist replaces the previously displayed artist tracks")
    void clickingInvolvedOnlyArtistReplacesPreviouslyDisplayedArtistTracks(FxRobot fxRobot) throws Exception {
        Artist akkya = of("Akkya");
        Artist triggerLive = of("Trigger Live");
        Artist bonobo = of("Bonobo");
        ObservableAudioItem bonoboTrack = ArtistViewTestFixtures.audioItem(
                "Kiara",
                bonobo,
                ArtistViewTestFixtures.album("Black Sands", bonobo, "Ninja Tune"),
                Set.of(bonobo),
                1);
        ObservableAudioItem akkyaTrack = ArtistViewTestFixtures.audioItem(
                "Circle (Akkya Remix)",
                triggerLive,
                ArtistViewTestFixtures.album("Diffusion 7.0 - Electronic Arrangment of Techno", of(""), "Gastspiel Records"),
                Set.of(triggerLive, akkya),
                21);

        Platform.runLater(() -> {
            audioItemsProperty.addAll(bonoboTrack, akkyaTrack);
            artistsProperty.addAll(Set.of(bonobo, triggerLive, akkya));
        });
        waitForFxEvents();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS,
                () -> fxRobot.lookup("Bonobo").tryQuery().isPresent()
                        && fxRobot.lookup("Akkya").tryQuery().isPresent());

        fxRobot.clickOn("Bonobo");
        waitForTrackText(fxRobot, "Kiara");

        fxRobot.clickOn("Akkya");
        waitForTrackText(fxRobot, "Circle (Akkya Remix)");
        assertThat(fxRobot.lookup("Kiara").tryQuery()).isEmpty();
    }

    private static void waitForTrackText(FxRobot fxRobot, String title) throws Exception {
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> fxRobot.lookup(title).tryQuery().isPresent());
        waitForFxEvents();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                ArtistViewController.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class
        })
})
class ArtistViewControllerUITConfiguration {

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
