package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.m3u.M3uImportException;
import net.transgressoft.commons.music.m3u.M3uImportService;
import net.transgressoft.musicott.events.ErrorEvent;
import net.transgressoft.musicott.events.ImportPlaylistsFromM3uEvent;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.testfx.util.WaitForAsyncUtils;

import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for the M3U import feature of {@link NavigationController},
 * verifying that a file selected via the {@link FileChooser} is imported via
 * {@link M3uImportService}, nested under {@code ROOT_PLAYLIST}, and that the
 * correct feedback events are published on success and hard failure.
 */
@JavaFxSpringTest(classes = NavigationControllerM3uImportITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("NavigationController M3U import")
class NavigationControllerM3uImportIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    FXMusicLibrary musicLibrary;

    @Autowired
    FileChooserHolder fileChooserHolder;

    @Autowired
    @SuppressWarnings("unchecked")
    M3uImportService<ObservableAudioItem, ObservablePlaylist> m3uImportService;

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @Test
    @DisplayName("imports m3u file and nests the playlist under ROOT_PLAYLIST")
    void importsM3uFileAndNestsUnderRootPlaylist(FxRobot fxRobot, @TempDir Path tempDir) throws IOException, TimeoutException {
        waitForFxEvents();

        Path m3uFile = tempDir.resolve("MyImportedPlaylist.m3u");
        Files.writeString(m3uFile, "#EXTM3U\n");

        // Create the playlist the service will return
        ObservablePlaylist importedPlaylist = musicLibrary.playlistHierarchy().createPlaylist("MyImportedPlaylist");
        doReturn(CompletableFuture.completedFuture(importedPlaylist)).when(m3uImportService).importAsync(any());
        when(fileChooserHolder.mock.showOpenDialog(any())).thenReturn(m3uFile.toFile());

        NavigationController controller = navigationControllerAndView.getController();
        controller.importPlaylistFromM3u();

        // The controller nests the imported playlist under ROOT_PLAYLIST off the FX thread; poll
        // until it appears there rather than sleeping for a fixed duration.
        ObservablePlaylistHierarchy hierarchy = musicLibrary.playlistHierarchy();
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> hierarchy.findByName(PlaylistTreeView.ROOT_PLAYLIST_NAME)
                .map(root -> root.getPlaylists().stream().anyMatch(p -> p.getName().equals("MyImportedPlaylist")))
                .orElse(false));

        // The imported playlist must be nested under ROOT_PLAYLIST, not merely present in the repository
        ObservablePlaylist root = hierarchy.findByName(PlaylistTreeView.ROOT_PLAYLIST_NAME).orElseThrow();
        assertThat(root.getPlaylists()).anyMatch(p -> p.getName().equals("MyImportedPlaylist"));

        // A StatusMessageUpdateEvent with the playlist name must have been published
        verify(applicationEventPublisher).publishEvent(
                argThat(event -> event instanceof StatusMessageUpdateEvent &&
                        ((StatusMessageUpdateEvent) event).statusMessage.contains("MyImportedPlaylist")));
    }

    @Test
    @DisplayName("publishes error event on hard import failure")
    void publishesErrorEventOnHardFailure(FxRobot fxRobot, @TempDir Path tempDir) throws IOException, InterruptedException {
        waitForFxEvents();

        Path m3uFile = tempDir.resolve("BadPlaylist.m3u");
        Files.writeString(m3uFile, "#EXTM3U\n");

        // Configure the service to fail with M3uImportException
        CompletableFuture<ObservablePlaylist> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new M3uImportException("Playlist with name 'BadPlaylist' already exists"));
        doReturn(failedFuture).when(m3uImportService).importAsync(any());
        when(fileChooserHolder.mock.showOpenDialog(any())).thenReturn(m3uFile.toFile());

        NavigationController controller = navigationControllerAndView.getController();
        controller.importPlaylistFromM3u();
        waitForFxEvents();

        // The already-completed failed future resolves synchronously; the error is published on the
        // next FX pulse, so pumping the FX queue is sufficient without a fixed sleep.
        WaitForAsyncUtils.waitForFxEvents();

        // An ErrorEvent must have been published
        verify(applicationEventPublisher).publishEvent(argThat(event -> event instanceof ErrorEvent));
    }
}

/**
 * Holder for the shared mutable {@link FileChooser} mock, allowing the test body
 * to configure its return value after the Spring context is fully built.
 */
class FileChooserHolder {
    final FileChooser mock = mock(FileChooser.class);
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class NavigationControllerM3uImportITConfiguration {

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) {
        var repository = musicLibrary.playlistHierarchy();
        repository.createPlaylistDirectory("ROOT_PLAYLIST");
        return repository;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected it import playlist", Optional.empty());
    }

    @Bean
    public ObservableAudioLibrary audioRepository() {
        return mock(ObservableAudioLibrary.class);
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }

    @Bean
    public AlertFactory alertFactory() {
        return mock(AlertFactory.class);
    }

    @Bean
    public FileChooserHolder fileChooserHolder() {
        return new FileChooserHolder();
    }

    @Bean
    public Supplier<DirectoryChooser> directoryChooserSupplier() {
        return () -> mock(DirectoryChooser.class);
    }

    @Bean
    public Supplier<FileChooser> fileChooserSupplier(FileChooserHolder holder) {
        // Return the shared mock so tests can configure showOpenDialog() before firing the event
        return () -> holder.mock;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public M3uImportService<ObservableAudioItem, ObservablePlaylist> m3uImportService() {
        // Mocked so individual tests can configure importAsync() return values as needed
        return mock(M3uImportService.class);
    }

    @Bean
    public KeyCombination.Modifier operativeSystemKeyModifier() {
        return KeyCombination.CONTROL_DOWN;
    }

    // destroyMethod = "" prevents Spring from auto-inferring shutdown(), which would call
    // Platform.exit() and kill the JavaFX Application Thread between test classes
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
