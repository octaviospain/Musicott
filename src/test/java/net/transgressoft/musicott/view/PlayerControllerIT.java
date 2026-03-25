package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;

/**
 * Integration test for {@link PlayerController}, verifying that the playback controls render
 * correctly and that button state reflects the library state via the Spring-integrated test context.
 */
@JavaFxSpringTest(classes = PlayerControllerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlayerControllerIT extends ApplicationTestBase<GridPane> {

    @Autowired
    FxControllerAndView<PlayerController, GridPane> playerControllerAndView;

    @Override
    protected GridPane javaFxComponent() {
        return playerControllerAndView.getView().get();
    }

    @Test
    @DisplayName("PlayerController renders playback controls")
    void rendersPlaybackControls(FxRobot fxRobot) {
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(fxRobot.lookup("#playerGridPane").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#playButton").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#prevButton").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#nextButton").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#volumeSlider").tryQuery()).isPresent();
        assertThat(fxRobot.lookup("#trackSlider").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("PlayerController play button is disabled when library is empty")
    void playButtonIsDisabledWhenLibraryIsEmpty(FxRobot fxRobot) {
        WaitForAsyncUtils.waitForFxEvents();

        ToggleButton playButton = fxRobot.lookup("#playButton").queryAs(ToggleButton.class);
        assertThat(playButton.isDisable()).isTrue();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                PlayerController.class,
                PlayQueueController.class
        })
})
class PlayerControllerITConfiguration {

    @Bean
    public ObservableAudioLibrary audioLibrary() {
        ReadOnlyBooleanProperty emptyProperty = new SimpleBooleanProperty(true);
        var library = mock(ObservableAudioLibrary.class);
        when(library.emptyLibraryProperty()).thenReturn(emptyProperty);
        return library;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> audioWaveformRepository() {
        return mock(AudioWaveformRepository.class);
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
