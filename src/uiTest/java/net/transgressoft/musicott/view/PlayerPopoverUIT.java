package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
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

import java.lang.reflect.Field;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI test verifying that the play-queue popover stays aligned to the play-queue button after
 * the application window is resized horizontally or vertically.
 */
@JavaFxSpringTest(classes = PlayerPopoverUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Play queue popover")
class PlayerPopoverUIT extends ApplicationTestBase<GridPane> {

    @Autowired
    FxControllerAndView<PlayerController, GridPane> playerControllerAndView;

    @Override
    protected GridPane javaFxComponent() {
        return playerControllerAndView.getView().get();
    }

    /**
     * Returns the {@code playQueueLayout} field from the controller. Since the node is removed
     * from the scene graph when hidden (see {@code PlayerController.hidePlayQueue}), CSS id lookup
     * via robot cannot find it; we retrieve the reference directly via reflection.
     */
    AnchorPane playQueueLayout() {
        try {
            Field field = PlayerController.class.getDeclaredField("playQueueLayout");
            field.setAccessible(true);
            return (AnchorPane) field.get(playerControllerAndView.getController());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot access PlayerController.playQueueLayout", e);
        }
    }

    @Test
    @DisplayName("anchors to play-queue button on initial show")
    void anchorsToPlayQueueButtonOnInitialShow(FxRobot robot) {
        // playQueueLayout is removed from the scene by hidePlayQueue() during initialize();
        // fire the toggle button first so showPlayQueue() re-adds it to the scene graph.
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        AnchorPane playQueueLayout = playQueueLayout();
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        assertThat(playQueueLayout.isVisible()).isTrue();
        double popoverCenterX = playQueueLayout.localToScene(playQueueLayout.getBoundsInLocal()).getCenterX();
        double buttonCenterX = playQueueButton.localToScene(playQueueButton.getBoundsInLocal()).getCenterX();
        assertThat(Math.abs(popoverCenterX - buttonCenterX)).isLessThan(2.0);
    }

    @Test
    @DisplayName("re-anchors to play-queue button after horizontal window resize")
    void reAnchorsToPlayQueueButtonAfterHorizontalWindowResize(FxRobot robot) {
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        AnchorPane playQueueLayout = playQueueLayout();
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        assertThat(playQueueLayout.isVisible()).isTrue();

        Stage stage = (Stage) robot.window(0);
        double newWidth = stage.getWidth() + 100.0;
        Platform.runLater(() -> stage.setWidth(newWidth));
        waitForFxEvents();

        double popoverCenterX = playQueueLayout.localToScene(playQueueLayout.getBoundsInLocal()).getCenterX();
        double buttonCenterX = playQueueButton.localToScene(playQueueButton.getBoundsInLocal()).getCenterX();
        assertThat(Math.abs(popoverCenterX - buttonCenterX)).isLessThan(2.0);
    }

    @Test
    @DisplayName("re-anchors to play-queue button after vertical window resize")
    void reAnchorsToPlayQueueButtonAfterVerticalWindowResize(FxRobot robot) {
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        AnchorPane playQueueLayout = playQueueLayout();
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        assertThat(playQueueLayout.isVisible()).isTrue();

        Stage stage = (Stage) robot.window(0);
        double popoverTopBefore = playQueueLayout.localToScene(playQueueLayout.getBoundsInLocal()).getMinY();

        double newHeight = stage.getHeight() + 100.0;
        Platform.runLater(() -> stage.setHeight(newHeight));
        waitForFxEvents();

        var popoverBounds = playQueueLayout.localToScene(playQueueLayout.getBoundsInLocal());
        var buttonBounds = playQueueButton.localToScene(playQueueButton.getBoundsInLocal());
        assertThat(Math.abs(popoverBounds.getCenterX() - buttonBounds.getCenterX())).isLessThan(2.0);

        // configurePlayQueuePopupSizing pins popover.minY ≈ topReserved (30px) regardless of scene
        // height: bottomMargin = max(bottomReserved, sceneH - popupH - topReserved), so growing the
        // scene grows bottomMargin in lockstep, keeping the popover top fixed near the top edge.
        // Without the heightProperty listener firing, bottomMargin would stay stale and popover.minY
        // would drift downward as the scene grows.
        double popoverTopAfter = popoverBounds.getMinY();
        assertThat(Math.abs(popoverTopAfter - popoverTopBefore)).isLessThan(5.0);
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                PlayerController.class,
                PlayQueueController.class
        })
})
class PlayerPopoverUITConfiguration {

    final SimpleBooleanProperty emptyLibraryProperty = new SimpleBooleanProperty(false);

    @Bean
    public PlayerService playerService() {
        var service = mock(PlayerService.class);
        when(service.getPlayQueueList()).thenReturn(FXCollections.observableArrayList());
        when(service.getHistoryQueueList()).thenReturn(FXCollections.observableArrayList());
        when(service.currentTrack()).thenReturn(Optional.empty());
        when(service.getTotalDuration()).thenReturn(javafx.util.Duration.ZERO);
        return service;
    }

    @Bean
    public ObservableAudioLibrary audioLibrary() {
        var library = mock(ObservableAudioLibrary.class);
        when(library.getEmptyLibraryProperty()).thenReturn(emptyLibraryProperty);
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
