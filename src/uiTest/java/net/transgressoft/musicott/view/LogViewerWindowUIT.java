package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.m3u.M3uImportService;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import net.transgressoft.musicott.logging.RingBufferHolder;
import net.transgressoft.musicott.service.MediaImportService;
import net.transgressoft.musicott.services.PlayerService;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.FullAudioItemTableView;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import net.transgressoft.musicott.view.itunes.ItunesImportWizard;

import ch.qos.logback.classic.Level;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.AfterEach;
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

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI test proving the end-to-end flow where a warning status signal opens the
 * "Application Logs" window. When a completed operation produces warnings or errors,
 * the notification banner appears with an "Open Log Viewer" action button; clicking it
 * publishes {@link net.transgressoft.musicott.events.OpenLogViewerEvent} which the
 * {@link LogViewerController} handles to open the viewer stage.
 *
 * @author Octavio Calleya
 */
@JavaFxSpringTest(classes = LogViewerWindowUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Application log viewer window")
class LogViewerWindowUIT extends ApplicationTestBase<BorderPane> {

    @Autowired
    FxControllerAndView<MainController, BorderPane> mainControllerAndView;

    @Autowired
    FxControllerAndView<LogViewerController, ?> logViewerControllerAndView;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected BorderPane javaFxComponent() {
        return mainControllerAndView.getView().get();
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        RingBufferHolder.INSTANCE.resetForTest();
        // Trigger FXML loading for LogViewerController before any test interacts with it.
        // The FxControllerAndView is prototype-scoped and lazy; accessing the controller here
        // causes FxWeaver to load the FXML and inject @FXML fields into the singleton @Controller
        // bean, so showWindow() can construct the Scene without a NullPointerException on root.
        logViewerControllerAndView.getController();
        super.beforeEach();
    }

    @AfterEach
    void closeLogViewerStage() {
        LogViewerController controller = logViewerControllerAndView.getController();
        Platform.runLater(() -> {
            if (controller.stage != null && controller.stage.isShowing()) {
                controller.stage.hide();
            }
            controller.stage = null;
        });
        waitForFxEvents();
    }

    @Test
    @DisplayName("clicking the notification banner action button opens the Application Logs window")
    void clickingNotificationBannerActionOpensLogViewerWindow(FxRobot fxRobot) throws Exception {
        waitForFxEvents();

        // Add a WARN record so the banner's action is available when the completion event arrives
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.test - import warning\n", Level.WARN);

        // Publish the WARN event through the real event bus so MainController's banner listener fires
        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Import finished", 1, this));
        waitForFxEvents();
        waitForFxEvents();

        // Wait until the NotificationPane is showing (driven by MainController's banner logic)
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                () -> mainControllerAndView.getController().isNotificationPaneShowing());

        assertThat(mainControllerAndView.getController().isNotificationPaneShowing())
                .as("notification banner must be showing after a WARN event")
                .isTrue();

        // Invoke the "Open Log Viewer" action programmatically — fxRobot.clickOn requires
        // visible bounds, and the NotificationPane skin may not render the button as visible
        // in a headless Monocle scene even when the pane itself is logically showing.
        mainControllerAndView.getController().invokeBannerAction();
        waitForFxEvents();

        // Poll for the Application Logs stage to appear
        Stage logStage = waitForDialogStage("Application Logs");
        assertThat(logStage).as("Application Logs stage").isNotNull();
        assertThat(logStage.isShowing()).isTrue();

        // Verify the viewer's controls are rendered inside the opened stage
        TextArea textArea = (TextArea) logStage.getScene().getRoot().lookup(".text-area");
        assertThat(textArea).as("log text area").isNotNull();
        assertThat(textArea.isEditable()).isFalse();

        @SuppressWarnings("unchecked")
        ComboBox<String> levelCombo = (ComboBox<String>) logStage.getScene().getRoot().lookup(".combo-box");
        assertThat(levelCombo).as("level filter combo").isNotNull();
        assertThat(levelCombo.getItems()).containsExactly("TRACE", "DEBUG", "INFO", "WARN", "ERROR");
    }

    @Test
    @DisplayName("opening log viewer via menu item shows the Application Logs window with log content")
    void openingLogViewerViaEventShowsWindowWithContent(FxRobot fxRobot) throws Exception {
        waitForFxEvents();

        // Pre-populate the buffer so there is content to show when the window opens
        RingBufferHolder.INSTANCE.add("INFO  net.transgressoft.test - application started\n", Level.INFO);
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft.test - configuration warning\n", Level.WARN);

        // Open the viewer directly via the event (same path as the menu item)
        LogViewerController controller = logViewerControllerAndView.getController();
        Platform.runLater(controller::showWindow);
        waitForFxEvents();

        Stage logStage = waitForDialogStage("Application Logs");
        assertThat(logStage).as("Application Logs stage").isNotNull();
        assertThat(logStage.isShowing()).isTrue();

        // The text area should show the pre-open buffer content at INFO+ level
        TextArea textArea = (TextArea) logStage.getScene().getRoot().lookup(".text-area");
        assertThat(textArea).isNotNull();
        assertThat(textArea.getText()).contains("application started");
        assertThat(textArea.getText()).contains("configuration warning");
    }

}

/**
 * Spring test configuration for {@link LogViewerWindowUIT}. Mounts the full main-controller
 * scene (which includes {@link NavigationController}) alongside {@link LogViewerController},
 * so the banner's "Open Log Viewer" action and the event listener on {@code LogViewerController}
 * both fire within the same application context.
 *
 * <p>Uses the real context publisher so {@code @EventListener} annotations fire during test
 * execution.
 */
@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                MainController.class,
                NavigationController.class,
                PlayerController.class,
                PlayQueueController.class,
                ArtistViewController.class,
                AlbumViewController.class,
                GenreViewController.class,
                PlaylistTreeView.class,
                FullAudioItemTableView.class,
                SimpleAudioItemTableView.class,
                ArtistAlbumListRow.class,
                LogViewerController.class
        })
})
class LogViewerWindowUITConfiguration {

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservableAudioLibrary audioRepository(FXMusicLibrary musicLibrary) {
        return musicLibrary.audioLibrary();
    }

    // PlayerController resolves its emptyLibraryProperty via @Value("#{audioLibrary...}"),
    // so the library bean must also be registered under the name "audioLibrary".
    @Bean
    public ObservableAudioLibrary audioLibrary(FXMusicLibrary musicLibrary) {
        return musicLibrary.audioLibrary();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) {
        var repository = musicLibrary.playlistHierarchy();
        repository.createPlaylistDirectory("ROOT_PLAYLIST");
        return repository;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected log viewer uit playlist", Optional.empty());
    }

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
    @SuppressWarnings("unchecked")
    public AudioWaveformRepository<AudioWaveform, ObservableAudioItem> audioWaveformRepository() {
        return mock(AudioWaveformRepository.class);
    }

    @Bean
    public AlertFactory alertFactory() {
        return mock(AlertFactory.class);
    }

    @Bean
    public MediaImportService mediaImportService() {
        return mock(MediaImportService.class);
    }

    @Bean
    public ItunesImportWizard itunesImportWizard() {
        return mock(ItunesImportWizard.class);
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher(ConfigurableApplicationContext applicationContext) {
        // Delegate to the real context publisher so @EventListener on MainController and
        // LogViewerController fires when events are published.
        return applicationContext::publishEvent;
    }

    @Bean
    public Supplier<DirectoryChooser> directoryChooserSupplier() {
        return () -> mock(DirectoryChooser.class);
    }

    @Bean
    public Supplier<FileChooser> fileChooserSupplier() {
        return () -> mock(FileChooser.class);
    }

    @Bean
    @SuppressWarnings("unchecked")
    public M3uImportService<ObservableAudioItem, ObservablePlaylist> m3uImportService() {
        return mock(M3uImportService.class);
    }

    @Bean
    public KeyCombination.Modifier operativeSystemKeyModifier() {
        return KeyCombination.CONTROL_DOWN;
    }

    @Bean
    public net.transgressoft.musicott.search.SearchCoordinator searchCoordinator() {
        return mock(net.transgressoft.musicott.search.SearchCoordinator.class);
    }

    @Bean
    public Supplier<Stage> stageSupplier() {
        return Stage::new;
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
