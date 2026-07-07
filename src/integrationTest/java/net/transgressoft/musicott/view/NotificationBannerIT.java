package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.m3u.M3uImportService;
import net.transgressoft.commons.music.waveform.AudioWaveform;
import net.transgressoft.commons.music.waveform.AudioWaveformRepository;
import net.transgressoft.musicott.events.OpenLogViewerEvent;
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test verifying the notification banner routing contract on {@link MainController}.
 *
 * <p>Drives {@link StatusMessageUpdateEvent} through the real Spring event bus and asserts
 * observable outcomes on {@code MainController}'s {@code NotificationPane}. The tests define
 * the accessor contract ({@code isNotificationPaneShowing()} and banner text/action lookups)
 * that the production implementation must satisfy.
 *
 * <p>These tests are intentionally RED until the {@code NotificationPane} integration is
 * implemented in {@code MainController}.
 *
 * @author Octavio Calleya
 */
@JavaFxSpringTest(classes = NotificationBannerITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("NotificationBanner — outcome message display")
class NotificationBannerIT extends ApplicationTestBase<BorderPane> {

    @Autowired
    FxControllerAndView<MainController, BorderPane> mainControllerAndView;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    OpenLogViewerEventCaptor openLogViewerEventCaptor;

    @Override
    protected BorderPane javaFxComponent() {
        return mainControllerAndView.getView().get();
    }

    @BeforeEach
    void resetBuffer() {
        RingBufferHolder.INSTANCE.resetForTest();
        openLogViewerEventCaptor.reset();
    }

    @Test
    @DisplayName("outcome event with warnings shows banner with full unclipped text")
    void outcomeEventWithWarningsShowsBannerWithFullText() throws TimeoutException {
        waitForFxEvents();

        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Exported 5 playlist(s)", 12, this));

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                () -> mainControllerAndView.getController().isNotificationPaneShowing());

        assertThat(mainControllerAndView.getController().isNotificationPaneShowing()).isTrue();
        String bannerText = mainControllerAndView.getController().getNotificationBannerText();
        assertThat(bannerText).contains("Exported 5 playlist(s)");
        assertThat(bannerText).contains("12 warning(s)/error(s)");
    }

    @Test
    @DisplayName("warn/error banner remains visible past the auto-dismiss window")
    void warnErrorBannerIsStickyAndDoesNotAutoDismiss() throws TimeoutException, InterruptedException {
        waitForFxEvents();

        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Exported 5 playlist(s)", 3, this));

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                () -> mainControllerAndView.getController().isNotificationPaneShowing());

        // Wait longer than any auto-dismiss timer would fire (6 seconds > 5 second default)
        Thread.sleep(6_000);
        waitForFxEvents();

        assertThat(mainControllerAndView.getController().isNotificationPaneShowing())
                .as("warn/error banner must stay visible and not auto-dismiss")
                .isTrue();
    }

    @Test
    @DisplayName("clean outcome banner shows then auto-dismisses within a bounded wait")
    void cleanOutcomeBannerAutoDismissesAfterShowing() throws TimeoutException {
        waitForFxEvents();

        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Exported 3 playlist(s)", 0, this));

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                () -> mainControllerAndView.getController().isNotificationPaneShowing());

        assertThat(mainControllerAndView.getController().isNotificationPaneShowing())
                .as("banner must appear initially")
                .isTrue();

        // Wait for auto-dismiss: the banner should hide itself within 10 seconds
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                () -> !mainControllerAndView.getController().isNotificationPaneShowing());

        assertThat(mainControllerAndView.getController().isNotificationPaneShowing())
                .as("banner must auto-dismiss after a clean-outcome event")
                .isFalse();
    }

    @Test
    @DisplayName("warn/error banner action button publishes an open log viewer event")
    void warnErrorBannerActionButtonPublishesOpenLogViewerEvent() throws TimeoutException {
        waitForFxEvents();

        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Import finished", 5, this));

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                () -> mainControllerAndView.getController().isNotificationPaneShowing());

        // Invoke the banner's "Open Log Viewer" action
        mainControllerAndView.getController().invokeBannerAction();
        waitForFxEvents();

        // Assert the OpenLogViewerEvent was observed on the bus via the test-layer captor
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                openLogViewerEventCaptor::wasReceived);

        assertThat(openLogViewerEventCaptor.wasReceived())
                .as("banner action must publish OpenLogViewerEvent")
                .isTrue();
    }

    @Test
    @DisplayName("transient and import-progress messages do not show the banner")
    void transientAndImportProgressMessagesDoNotOpenBanner() throws TimeoutException {
        waitForFxEvents();

        List<String> sidebarOnlyMessages = List.of(
                "Searching...",
                "",
                "Importing files...",
                "Importing from iTunes...",
                "Importing: some-track.mp3"
        );

        for (String msg : sidebarOnlyMessages) {
            applicationEventPublisher.publishEvent(
                    new StatusMessageUpdateEvent(msg, this));
            waitForFxEvents();
            waitForFxEvents();

            assertThat(mainControllerAndView.getController().isNotificationPaneShowing())
                    .as("banner must NOT appear for transient/import-progress message: '%s'", msg)
                    .isFalse();
        }

        // A discrete outcome event DOES open the banner, confirming the routing is active
        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Import process completed", 0, this));

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                () -> mainControllerAndView.getController().isNotificationPaneShowing());

        assertThat(mainControllerAndView.getController().isNotificationPaneShowing())
                .as("discrete outcome event must open the banner")
                .isTrue();
    }

    @Test
    @DisplayName("import-progress messages reach the sidebar progress bar while banner stays hidden")
    void importProgressMessagesReachSidebarProgressBar() throws TimeoutException {
        waitForFxEvents();

        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Importing: my-track.mp3", this));
        waitForFxEvents();
        waitForFxEvents();

        assertThat(mainControllerAndView.getController().isNotificationPaneShowing())
                .as("banner must not appear for Importing: ... message")
                .isFalse();

        ProgressBar sidebarProgressBar = (ProgressBar) mainControllerAndView.getView().get()
                .lookup("#taskProgressBar");
        assertThat(sidebarProgressBar)
                .as("the sidebar progress bar must be mounted in the full scene")
                .isNotNull();
        assertThat(sidebarProgressBar.getTooltip())
                .as("sidebar progress bar tooltip must be set for the import-progress message")
                .isNotNull();
        assertThat(sidebarProgressBar.getTooltip().getText())
                .as("sidebar progress bar tooltip must show the import-progress text")
                .contains("Importing: my-track.mp3");
    }

    @Test
    @DisplayName("rapid successive outcome events replace rather than stack — banner shows only the latest message")
    void rapidSuccessiveOutcomeEventsReplaceInsteadOfStack() throws TimeoutException {
        waitForFxEvents();

        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("First outcome", 0, this));
        applicationEventPublisher.publishEvent(
                new StatusMessageUpdateEvent("Second outcome", 0, this));
        waitForFxEvents();
        waitForFxEvents();

        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS,
                () -> mainControllerAndView.getController().isNotificationPaneShowing());

        String bannerText = mainControllerAndView.getController().getNotificationBannerText();
        assertThat(bannerText)
                .as("banner must show the latest outcome message only")
                .contains("Second outcome");
        assertThat(bannerText)
                .as("banner must not contain the earlier replaced message")
                .doesNotContain("First outcome");
    }
}

/**
 * Captures {@link OpenLogViewerEvent} publications on the Spring event bus so
 * {@link NotificationBannerIT} can assert that the banner action fires the event without
 * embedding test state into the production controller.
 */
@Component
class OpenLogViewerEventCaptor {

    private final AtomicBoolean received = new AtomicBoolean(false);

    @EventListener
    public void onOpenLogViewerEvent(OpenLogViewerEvent event) {
        received.set(true);
    }

    boolean wasReceived() {
        return received.get();
    }

    void reset() {
        received.set(false);
    }
}

/**
 * Spring test configuration for {@link NotificationBannerIT}. Mounts the full main-controller
 * scene so the {@link MainController}'s notification banner event listener fires when
 * {@link StatusMessageUpdateEvent} is published through the real context delegate.
 *
 * <p>Uses the real context publisher (not a mock) so that {@code @EventListener} annotations
 * on {@code MainController} and {@code NavigationController} fire during test execution.
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
                OpenLogViewerEventCaptor.class
        })
})
class NotificationBannerITConfiguration {

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
        return new SimpleObjectProperty<>(this, "selected banner it playlist", Optional.empty());
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
        // Delegate to the real context publisher so @EventListener on MainController fires
        // when StatusMessageUpdateEvent is published.
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
