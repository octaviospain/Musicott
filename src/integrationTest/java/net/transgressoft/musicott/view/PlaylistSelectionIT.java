package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.commons.music.m3u.M3uImportService;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ALL_AUDIO_ITEMS;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ARTISTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration tests for playlist/navigation selection wiring: sole selection on creation,
 * mutual exclusivity between playlist tree and nav-mode list, and re-selectability of nav modes.
 */
@JavaFxSpringTest(classes = PlaylistSelectionITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlaylistSelectionIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Autowired
    ObservablePlaylistHierarchy playlistRepository;

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @Test
    @DisplayName("NavigationController selects only the newly added playlist after creation")
    void selectsOnlyNewPlaylistAfterCreation(FxRobot fxRobot) {
        waitForFxEvents();

        Platform.runLater(() -> treeView(fxRobot).getRoot().setExpanded(true));
        waitForFxEvents();

        // Click "Test Playlist" first so there is an existing selection
        fxRobot.clickOn("Test Playlist");
        waitForFxEvents();

        // Create a new playlist via the controller on the FX thread
        var newPlaylist = new AtomicReference<ObservablePlaylist>();
        Platform.runLater(() -> {
            var p = playlistRepository.createPlaylist("New Playlist");
            playlistRepository.addPlaylistsToDirectory(Set.of(p), "ROOT_PLAYLIST");
            newPlaylist.set(p);
            navigationControllerAndView.getController().addNewPlaylist(p);
        });
        waitForFxEvents();

        var selectedItems = treeView(fxRobot).getSelectionModel().getSelectedItems();
        assertThat(selectedItems).hasSize(1);
        assertThat(selectedItems.get(0).getValue()).isEqualTo(newPlaylist.get());
    }

    @Test
    @DisplayName("NavigationController clears playlist tree when a nav mode is selected")
    void clearsPlaylistTreeOnNavModeSelection(FxRobot fxRobot) {
        waitForFxEvents();

        Platform.runLater(() -> treeView(fxRobot).getRoot().setExpanded(true));
        waitForFxEvents();

        // Select a playlist first
        fxRobot.clickOn("Test Playlist");
        waitForFxEvents();
        assertThat(treeView(fxRobot).getSelectionModel().getSelectedItems()).isNotEmpty();

        // Clicking "All tracks" should clear the tree selection
        fxRobot.clickOn("All tracks");
        waitForFxEvents();

        assertThat(treeView(fxRobot).getSelectionModel().getSelectedItem()).isNull();
        assertThat(navigationControllerAndView.getController().navigationModeProperty().get())
                .isEqualTo(ALL_AUDIO_ITEMS);
    }

    @Test
    @DisplayName("NavigationController clears nav list selection when a playlist is clicked")
    void clearsNavListSelectionWhenPlaylistIsClicked(FxRobot fxRobot) {
        waitForFxEvents();

        Platform.runLater(() -> treeView(fxRobot).getRoot().setExpanded(true));
        waitForFxEvents();

        // Start on "Artists" nav mode (selected by default)
        assertThat(navigationControllerAndView.getController().navigationModeProperty().get())
                .isEqualTo(ARTISTS);

        // Click a playlist
        fxRobot.clickOn("Test Playlist");
        waitForFxEvents();

        // Nav list should have no selection
        var navListView = fxRobot.lookup("#navigationModeListView").<javafx.scene.control.ListView<?>>query();
        assertThat(navListView.getSelectionModel().getSelectedItem()).isNull();
    }

    @Test
    @DisplayName("NavigationController re-fires navigationModeProperty when the same nav mode is re-selected")
    void reFiresNavigationModePropertyOnReSelection(FxRobot fxRobot) {
        waitForFxEvents();

        Platform.runLater(() -> treeView(fxRobot).getRoot().setExpanded(true));
        waitForFxEvents();

        // Select a playlist so nav list gets cleared
        fxRobot.clickOn("Test Playlist");
        waitForFxEvents();

        var modeChanges = new AtomicReference<Integer>(0);
        navigationControllerAndView.getController().navigationModeProperty()
                .addListener((obs, old, newMode) -> modeChanges.updateAndGet(c -> c + 1));

        // Click "All tracks" — nav list was cleared so this is a fresh selection → fires
        fxRobot.clickOn("All tracks");
        waitForFxEvents();

        assertThat(modeChanges.get()).isGreaterThanOrEqualTo(1);
        assertThat(navigationControllerAndView.getController().navigationModeProperty().get())
                .isEqualTo(ALL_AUDIO_ITEMS);
    }

    @SuppressWarnings("unchecked")
    private TreeView<ObservablePlaylist> treeView(FxRobot fxRobot) {
        return (TreeView<ObservablePlaylist>) fxRobot.lookup("#playlistTreeView").query();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class PlaylistSelectionITConfiguration {

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) {
        var repository = musicLibrary.playlistHierarchy();
        repository.createPlaylistDirectory("ROOT_PLAYLIST");
        var testPlaylist = repository.createPlaylist("Test Playlist");
        repository.addPlaylistsToDirectory(Set.of(testPlaylist), "ROOT_PLAYLIST");
        return repository;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected playlist", Optional.empty());
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
