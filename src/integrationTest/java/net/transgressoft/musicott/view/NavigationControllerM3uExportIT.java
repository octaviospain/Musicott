package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.m3u.M3uImportService;
import net.transgressoft.musicott.events.ErrorEvent;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxControllerAndView;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import net.rgielen.fxweaver.spring.SpringFxWeaver;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
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

import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test for the M3U export feature of {@link NavigationController},
 * verifying that selected playlists are written to {@code .m3u} files in the chosen directory
 * and that correct feedback events are published on success and failure.
 */
@JavaFxSpringTest(classes = NavigationControllerM3uExportITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("NavigationController M3U export")
class NavigationControllerM3uExportIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @AfterAll
    static void cleanUpExportedFiles() throws IOException {
        // deleteOnExit() only removes the (then non-empty) directory; recursively delete the .m3u
        // files written during the tests so nothing leaks into the OS temp directory.
        FileUtils.deleteDirectory(NavigationControllerM3uExportITConfiguration.TEMP_DIR.toFile());
    }

    @Test
    @DisplayName("exports selected playlists to .m3u files in the chosen directory")
    void exportsSelectedPlaylistsToM3uFiles(FxRobot fxRobot) throws TimeoutException {
        waitForFxEvents();

        NavigationController controller = navigationControllerAndView.getController();

        // Expand root and select "Playlist Alpha" in the tree
        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        Platform.runLater(() -> treeView.getRoot().setExpanded(true));
        waitForFxEvents();

        fxRobot.clickOn("Playlist Alpha");
        waitForFxEvents();

        // Invoke the event listener directly — the mocked DirectoryChooser returns TEMP_DIR
        controller.exportSelectedPlaylists();
        waitForFxEvents();

        // Poll for the off-thread export to write the file rather than sleeping a fixed duration
        File exported = new File(NavigationControllerM3uExportITConfiguration.TEMP_DIR.toFile(), "Playlist Alpha.m3u");
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, exported::exists);

        // The success StatusMessageUpdateEvent is published later on the FX thread, after the export
        // future completes — poll for it too so the assertion below cannot race the file write.
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> eventPublished(applicationEventPublisher, StatusMessageUpdateEvent.class));
        waitForFxEvents();

        // The selected playlist's .m3u file must exist in TEMP_DIR
        assertThat(exported).exists();

        // A StatusMessageUpdateEvent must have been published via the mocked publisher
        verify(applicationEventPublisher).publishEvent(
                argThat(event -> event instanceof StatusMessageUpdateEvent &&
                        ((StatusMessageUpdateEvent) event).statusMessage.contains("playlist")));
    }

    @Test
    @DisplayName("publishes error event on collision without overwriting the existing file")
    void publishesErrorEventOnCollision(FxRobot fxRobot) throws IOException, TimeoutException {
        waitForFxEvents();

        // Pre-create the target file to force a collision for "Playlist Beta"
        File collision = new File(NavigationControllerM3uExportITConfiguration.TEMP_DIR.toFile(), "Playlist Beta.m3u");
        Files.writeString(collision.toPath(), "# pre-existing");

        // Expand root and select "Playlist Beta"
        NavigationController controller = navigationControllerAndView.getController();
        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        Platform.runLater(() -> treeView.getRoot().setExpanded(true));
        waitForFxEvents();

        fxRobot.clickOn("Playlist Beta");
        waitForFxEvents();

        // Invoke the export listener directly
        controller.exportSelectedPlaylists();
        waitForFxEvents();

        // Poll until the aggregated failure has been published as an ErrorEvent rather than sleeping
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> eventPublished(applicationEventPublisher, ErrorEvent.class));
        waitForFxEvents();

        // An ErrorEvent must have been published
        verify(applicationEventPublisher).publishEvent(
                argThat(event -> event instanceof ErrorEvent));

        // The pre-existing file must not have been overwritten
        assertThat(Files.readString(collision.toPath())).isEqualTo("# pre-existing");
    }

    private static boolean eventPublished(ApplicationEventPublisher publisher, Class<?> eventType) {
        return Mockito.mockingDetails(publisher).getInvocations().stream()
                .flatMap(invocation -> Arrays.stream(invocation.getArguments()))
                .anyMatch(eventType::isInstance);
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class NavigationControllerM3uExportITConfiguration {

    // Shared temp directory created eagerly so it can be returned by both the bean and the test
    static final Path TEMP_DIR;
    static {
        try {
            TEMP_DIR = Files.createTempDirectory("musicott-m3u-export-it");
            TEMP_DIR.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Could not create temp dir for M3U export IT", e);
        }
    }

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) {
        var repository = musicLibrary.playlistHierarchy();
        repository.createPlaylistDirectory("ROOT_PLAYLIST");
        var alpha = repository.createPlaylist("Playlist Alpha");
        var beta = repository.createPlaylist("Playlist Beta");
        repository.addPlaylistsToDirectory(Set.of(alpha, beta), "ROOT_PLAYLIST");
        return repository;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected it export playlist", Optional.empty());
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
    public Supplier<DirectoryChooser> directoryChooserSupplier() {
        // Mock DirectoryChooser always returns the shared temp dir so tests can inspect written files
        DirectoryChooser mockChooser = mock(DirectoryChooser.class);
        when(mockChooser.showDialog(any())).thenReturn(TEMP_DIR.toFile());
        return () -> mockChooser;
    }

    @Bean
    public Supplier<FileChooser> fileChooserSupplier() {
        // Stub; not used by the export flow but required by the NavigationController constructor
        FileChooser mockChooser = mock(FileChooser.class);
        return () -> mockChooser;
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
