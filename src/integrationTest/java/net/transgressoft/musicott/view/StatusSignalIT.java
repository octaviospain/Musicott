package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.m3u.M3uImportService;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import net.transgressoft.musicott.logging.RingBufferHolder;

import ch.qos.logback.classic.Level;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration test verifying that the per-operation WARN/ERROR delta is correctly propagated
 * through the status signal path: a WARN-emitting M3U import yields a
 * {@link StatusMessageUpdateEvent} with {@code warnErrorDelta > 0}, whereas a clean import
 * yields {@code warnErrorDelta == 0}.
 *
 * <p>Scope: these tests exercise the mark/delta accounting and the status-signal wiring
 * (mark captured at operation start, delta computed at completion, {@code warnErrorDelta}
 * propagated on the event). The WARN case injects the record directly into
 * {@code RingBufferHolder} rather than through SLF4J, because duplicate {@code logback-test.xml}
 * resources from test-fixture JARs prevent the WARN from reaching the appender in this source set.
 * The full SLF4J → {@code RingBufferAppender} → buffer pipeline is therefore not covered here; the
 * ring-buffer and appender contracts are covered by their own unit tests. A true end-to-end
 * variant depends on resolving that logback-classpath conflict.
 */
@JavaFxSpringTest(classes = StatusSignalITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("StatusSignal — per-operation WARN/ERROR delta")
class StatusSignalIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    FXMusicLibrary musicLibrary;

    @Autowired
    StatusSignalITFileChooserHolder fileChooserHolder;

    @Autowired
    @SuppressWarnings("unchecked")
    M3uImportService<ObservableAudioItem, ObservablePlaylist> m3uImportService;

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @BeforeEach
    void resetBuffer() {
        RingBufferHolder.INSTANCE.resetForTest();
    }

    @Test
    @DisplayName("M3U import with no WARN/ERROR emits warnErrorDelta of zero")
    void m3uImportWithNoWarnsEmitsZeroDelta(@TempDir Path tempDir) throws IOException, TimeoutException {
        waitForFxEvents();

        Path m3uFile = tempDir.resolve("CleanImport.m3u");
        Files.writeString(m3uFile, "#EXTM3U\n");

        ObservablePlaylist importedPlaylist = musicLibrary.playlistHierarchy().createPlaylist("CleanImport");
        doReturn(CompletableFuture.completedFuture(importedPlaylist)).when(m3uImportService).importAsync(any());
        when(fileChooserHolder.mock.showOpenDialog(any())).thenReturn(m3uFile.toFile());

        // Clean import — no WARN/ERROR emitted into the ring buffer
        NavigationController controller = navigationControllerAndView.getController();
        controller.importPlaylistFromM3u();
        waitForFxEvents();

        verify(applicationEventPublisher).publishEvent(
                argThat(event -> event instanceof StatusMessageUpdateEvent &&
                        ((StatusMessageUpdateEvent) event).statusMessage.contains("CleanImport") &&
                        ((StatusMessageUpdateEvent) event).warnErrorDelta == 0));
    }

    @Test
    @DisplayName("M3U import with WARN emitted during the operation yields warnErrorDelta greater than zero")
    void m3uImportWithWarnYieldsPositiveDelta(@TempDir Path tempDir) throws IOException, TimeoutException, InterruptedException {
        waitForFxEvents();

        Path m3uFile = tempDir.resolve("WarnImport.m3u");
        Files.writeString(m3uFile, "#EXTM3U\n");

        ObservablePlaylist importedPlaylist = musicLibrary.playlistHierarchy().createPlaylist("WarnImport");

        // The future is a manually-completable one. The test drives it to completion only AFTER
        // the mark has been captured inside importPlaylistFromM3u (which runs on the FX thread).
        // This prevents a race where the WARN is logged before the mark is set.
        CompletableFuture<ObservablePlaylist> controllableFuture = new CompletableFuture<>();
        doReturn(controllableFuture).when(m3uImportService).importAsync(any());
        when(fileChooserHolder.mock.showOpenDialog(any())).thenReturn(m3uFile.toFile());

        NavigationController controller = navigationControllerAndView.getController();
        controller.importPlaylistFromM3u();

        // Pump the FX queue so importPlaylistFromM3u's Platform.runLater body runs, capturing the mark.
        waitForFxEvents();

        // Inject a WARN record directly into the ring buffer — the mark has already been captured,
        // so this increments warnErrorCount within the bracketed operation window. Calling add()
        // directly avoids a logback-classpath conflict (duplicate logback-test.xml resources from
        // test fixture JARs) that would otherwise prevent the WARN from reaching the appender.
        RingBufferHolder.INSTANCE.add("WARN  net.transgressoft - Simulated import warning", Level.WARN);

        // Complete the future with the imported playlist, triggering the whenComplete callback.
        controllableFuture.complete(importedPlaylist);
        waitForFxEvents();
        waitForFxEvents();

        // Poll until the event is captured by the mock before asserting
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () ->
                Mockito.mockingDetails(applicationEventPublisher).getInvocations().stream()
                        .flatMap(inv -> Arrays.stream(inv.getArguments()))
                        .anyMatch(e -> e instanceof StatusMessageUpdateEvent &&
                                ((StatusMessageUpdateEvent) e).statusMessage.contains("WarnImport")));

        verify(applicationEventPublisher).publishEvent(
                argThat(event -> event instanceof StatusMessageUpdateEvent &&
                        ((StatusMessageUpdateEvent) event).statusMessage.contains("WarnImport") &&
                        ((StatusMessageUpdateEvent) event).warnErrorDelta > 0));
    }
}

/**
 * Holds the shared mutable {@link FileChooser} mock for {@link StatusSignalIT},
 * allowing each test to configure its return value after the Spring context is built.
 */
class StatusSignalITFileChooserHolder {
    final FileChooser mock = mock(FileChooser.class);
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class StatusSignalITConfiguration {

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
        return new SimpleObjectProperty<>(this, "selected it signal playlist", Optional.empty());
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
    public StatusSignalITFileChooserHolder fileChooserHolder() {
        return new StatusSignalITFileChooserHolder();
    }

    @Bean
    public Supplier<DirectoryChooser> directoryChooserSupplier() {
        return () -> mock(DirectoryChooser.class);
    }

    @Bean
    public Supplier<FileChooser> fileChooserSupplier(StatusSignalITFileChooserHolder holder) {
        return () -> holder.mock;
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
