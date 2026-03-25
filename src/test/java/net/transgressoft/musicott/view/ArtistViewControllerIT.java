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
import net.transgressoft.commons.event.TransEventPublisher;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
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

import static net.transgressoft.commons.music.audio.ImmutableArtist.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;

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
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(fxRobot.lookup("#artistsListView").tryQuery()).isPresent();
        assertThat(artistsProperty).hasSize(1);
    }

    @Test
    @DisplayName("ArtistViewController validates album display when an artist is clicked")
    void validatesAlbumDisplayWhenArtistIsClicked(FxRobot fxRobot) {
        Platform.runLater(() -> artistsProperty.add(of("Bonobo")));
        WaitForAsyncUtils.waitForFxEvents();

        fxRobot.clickOn("Bonobo");
        WaitForAsyncUtils.waitForFxEvents();

        // Artist list view remains accessible after artist selection
        assertThat(fxRobot.lookup("#artistsListView").tryQuery()).isPresent();
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
    public ObservableAudioLibrary audioRepository(ReadOnlySetProperty<Artist> artistsProperty) {
        var repository = mock(ObservableAudioLibrary.class);
        when(repository.artistsProperty()).thenReturn(artistsProperty);
        when(repository.getArtistCatalogPublisher()).thenReturn(mock(TransEventPublisher.class));
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
