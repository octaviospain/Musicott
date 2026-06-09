package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.m3u.M3uImportService;
import net.transgressoft.musicott.events.ExportSelectedPlaylistsEvent;
import net.transgressoft.musicott.events.ImportPlaylistsFromM3uEvent;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;

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

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * UI test verifying that the M3U export context-menu items in {@link PlaylistTreeView}
 * publish the expected application events when fired.
 */
@JavaFxSpringTest(classes = PlaylistTreeViewM3uUITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Playlist M3U context menu actions")
class PlaylistTreeViewM3uUIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @Test
    @DisplayName("right-click context menu fires export event when 'Export playlist(s) as m3u file(s)' is clicked")
    void rightClickContextMenuFiresExportEvent(FxRobot fxRobot) {
        waitForFxEvents();

        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();

        fxRobot.interact(() -> {
            var items = treeView.getContextMenu().getItems();
            items.stream()
                    .filter(mi -> "Export playlist(s) as m3u file(s)".equals(mi.getText()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Export menu item not found"))
                    .fire();
        });
        waitForFxEvents();

        verify(applicationEventPublisher).publishEvent(any(ExportSelectedPlaylistsEvent.class));
    }

    @Test
    @DisplayName("right-click context menu fires import event when 'Import playlist from m3u file...' is clicked")
    void rightClickContextMenuFiresImportEvent(FxRobot fxRobot) {
        waitForFxEvents();

        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();

        fxRobot.interact(() -> {
            var items = treeView.getContextMenu().getItems();
            items.stream()
                    .filter(mi -> "Import playlist from m3u file...".equals(mi.getText()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Import menu item not found"))
                    .fire();
        });
        waitForFxEvents();

        verify(applicationEventPublisher).publishEvent(any(ImportPlaylistsFromM3uEvent.class));
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class PlaylistTreeViewM3uUITConfiguration {

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) {
        var repository = musicLibrary.playlistHierarchy();
        repository.createPlaylistDirectory("ROOT_PLAYLIST");
        var playlist = repository.createPlaylist("Test M3U Playlist");
        repository.addPlaylistsToDirectory(Set.of(playlist), "ROOT_PLAYLIST");
        return repository;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected m3u ui test playlist", Optional.empty());
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
        // Stub; the export file I/O is not exercised by this UI test
        DirectoryChooser mockChooser = mock(DirectoryChooser.class);
        return () -> mockChooser;
    }

    @Bean
    public Supplier<FileChooser> fileChooserSupplier() {
        // Stub; not exercised by this UI test
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
