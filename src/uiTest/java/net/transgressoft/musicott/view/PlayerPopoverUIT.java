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
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.controlsfx.control.PopOver;
import org.junit.jupiter.api.AfterEach;
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

    private PopOver playQueuePopOver() {
        try {
            Field field = PlayerController.class.getDeclaredField("playQueuePopOver");
            field.setAccessible(true);
            return (PopOver) field.get(playerControllerAndView.getController());
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Unable to inspect the play queue popover", ex);
        }
    }

    private Window playQueueWindow(FxRobot robot) {
        Window primaryWindow = robot.window(0);
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(window -> window != primaryWindow)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Play queue popover window is not showing"));
    }

    private double buttonCenterX(ToggleButton playQueueButton) {
        var bounds = playQueueButton.localToScreen(playQueueButton.getBoundsInLocal());
        assertThat(bounds).isNotNull();
        return bounds.getCenterX();
    }

    private double buttonCenterY(ToggleButton playQueueButton) {
        var bounds = playQueueButton.localToScreen(playQueueButton.getBoundsInLocal());
        assertThat(bounds).isNotNull();
        return bounds.getCenterY();
    }

    private Optional<ScrollBar> verticalScrollBar(ListView<?> listView) {
        return listView.lookupAll(".scroll-bar").stream()
                .filter(ScrollBar.class::isInstance)
                .map(ScrollBar.class::cast)
                .filter(Node::isVisible)
                .filter(scrollBar -> scrollBar.getOrientation() == Orientation.VERTICAL)
                .findFirst();
    }

    @AfterEach
    void closePopupWindows() {
        Platform.runLater(() -> Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(window -> !(window instanceof Stage))
                .toList()
                .forEach(Window::hide));
        waitForFxEvents();
    }

    @Test
    @DisplayName("anchors to play-queue button on initial show")
    void anchorsToPlayQueueButtonOnInitialShow(FxRobot robot) {
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        Window popupWindow = playQueueWindow(robot);
        ListView<?> queuesListView = robot.lookup("#queuesListView").queryListView();

        assertThat(playQueueButton.isSelected()).isTrue();
        assertThat(popupWindow.isShowing()).isTrue();
        assertThat(queuesListView.isVisible()).isTrue();
        assertThat(Math.abs((popupWindow.getX() + popupWindow.getWidth() / 2.0) - buttonCenterX(playQueueButton))).isLessThan(12.0);
    }

    @Test
    @DisplayName("re-anchors to play-queue button after horizontal window resize")
    void reAnchorsToPlayQueueButtonAfterHorizontalWindowResize(FxRobot robot) {
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        Stage stage = (Stage) robot.window(0);
        double newWidth = stage.getWidth() + 100.0;
        Platform.runLater(() -> stage.setWidth(newWidth));
        waitForFxEvents();

        Window popupWindow = playQueueWindow(robot);
        assertThat(playQueueButton.isSelected()).isTrue();
        assertThat(Math.abs((popupWindow.getX() + popupWindow.getWidth() / 2.0) - buttonCenterX(playQueueButton))).isLessThan(12.0);
    }

    @Test
    @DisplayName("re-anchors to play-queue button after vertical window resize")
    void reAnchorsToPlayQueueButtonAfterVerticalWindowResize(FxRobot robot) {
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        Stage stage = (Stage) robot.window(0);
        double newHeight = stage.getHeight() + 100.0;
        Platform.runLater(() -> stage.setHeight(newHeight));
        waitForFxEvents();

        Window popupWindow = playQueueWindow(robot);
        assertThat(Math.abs((popupWindow.getX() + popupWindow.getWidth() / 2.0) - buttonCenterX(playQueueButton))).isLessThan(12.0);
        assertThat(popupWindow.getY() + popupWindow.getHeight() / 2.0).isLessThan(buttonCenterY(playQueueButton));
    }

    @Test
    @DisplayName("closes the play-queue popover when the button is toggled again")
    void closesThePlayQueuePopoverWhenTheButtonIsToggledAgain(FxRobot robot) {
        robot.clickOn("#playQueueButton");
        waitForFxEvents();

        assertThat(playQueueWindow(robot).isShowing()).isTrue();

        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        robot.clickOn(buttonCenterX(playQueueButton), buttonCenterY(playQueueButton));
        waitForFxEvents();

        assertNoPopupWindowsShowing(robot);
        assertThat(playQueueButton.isSelected()).isFalse();
    }

    private void assertNoPopupWindowsShowing(FxRobot robot) {
        Window primaryWindow = robot.window(0);
        assertThat(Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(window -> window != primaryWindow)
                .toList())
                .isEmpty();
    }

    @Test
    @DisplayName("uses auto-hide popover semantics without consuming dismissal clicks")
    void usesAutoHidePopoverSemanticsWithoutConsumingDismissalClicks() {
        PopOver popOver = playQueuePopOver();

        assertThat(popOver.isAutoHide()).isTrue();
        assertThat(popOver.getConsumeAutoHidingEvents()).isFalse();
        assertThat(popOver.getStyleClass()).contains("play-queue-popover");
    }

    @Test
    @DisplayName("closes the play-queue popover when clicking elsewhere in the player window")
    void closesThePlayQueuePopoverWhenClickingElsewhereInThePlayerWindow(FxRobot robot) {
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        assertThat(playQueueWindow(robot).isShowing()).isTrue();

        robot.clickOn("#songTitleLabel");
        waitForFxEvents();

        assertNoPopupWindowsShowing(robot);
        assertThat(playQueueButton.isSelected()).isFalse();
    }

    @Test
    @DisplayName("closes the play-queue popover when the owner window is hidden")
    void closesThePlayQueuePopoverWhenTheOwnerWindowIsHidden(FxRobot robot) {
        ToggleButton playQueueButton = robot.lookup("#playQueueButton").queryAs(ToggleButton.class);
        Platform.runLater(playQueueButton::fire);
        waitForFxEvents();

        assertThat(playQueueWindow(robot).isShowing()).isTrue();

        Stage stage = (Stage) robot.window(0);
        Platform.runLater(stage::hide);
        waitForFxEvents();

        assertThat(Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(window -> !(window instanceof Stage))
                .toList())
                .isEmpty();
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
        when(service.getTotalDuration()).thenReturn(java.time.Duration.ZERO);
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
