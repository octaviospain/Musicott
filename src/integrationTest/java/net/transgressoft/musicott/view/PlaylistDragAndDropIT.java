package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.musicott.test.ApplicationTestBase;
import net.transgressoft.musicott.test.JavaFxSpringTest;
import net.transgressoft.musicott.test.JavaFxSpringTestConfiguration;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.springframework.context.annotation.ComponentScan.Filter;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Integration tests verifying that the dragboard payload for a playlist is a serializable
 * {@link Integer} id, and that it resolves back to the correct playlist via the hierarchy.
 */
@JavaFxSpringTest(classes = PlaylistDragAndDropITConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlaylistDragAndDropIT extends ApplicationTestBase<VBox> {

    @Autowired
    FxControllerAndView<NavigationController, VBox> navigationControllerAndView;

    @Autowired
    ObservablePlaylistHierarchy playlistRepository;

    @Override
    protected VBox javaFxComponent() {
        return navigationControllerAndView.getView().get();
    }

    @Test
    @DisplayName("PlaylistTreeView playlist id is an Integer and is serializable")
    void playlistIdIsIntegerAndSerializable(FxRobot fxRobot) {
        waitForFxEvents();

        var hierarchy = playlistRepository;
        var playlist = hierarchy.findByName("Drag Playlist").orElseThrow();

        // The id returned by getId() must be an Integer (the dragboard payload type)
        Object id = playlist.getId();
        assertThat(id).isInstanceOf(Integer.class);

        // The Integer must be serializable (required for JavaFX dragboard)
        assertThatCode(() -> {
            try (var baos = new ByteArrayOutputStream();
                 var oos = new ObjectOutputStream(baos)) {
                oos.writeObject(id);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("PlaylistTreeView drag payload integer id resolves back to the same playlist via hierarchy")
    void dragPayloadIdResolvesBackToPlaylist(FxRobot fxRobot) {
        waitForFxEvents();

        var hierarchy = playlistRepository;
        var playlist = hierarchy.findByName("Drag Playlist").orElseThrow();
        var id = (Integer) playlist.getId();

        var resolved = hierarchy.findById(id);

        assertThat(resolved).isPresent();
        assertThat(resolved.get()).isEqualTo(playlist);
    }

    @Test
    @DisplayName("PlaylistTreeView resolves drag payload id and moves playlist into a sub-folder without exception")
    void movePlaylistIntoFolderWithoutException(FxRobot fxRobot) {
        waitForFxEvents();

        // "Inner Playlist" lives inside "Drag Folder", so findParentPlaylist returns "Drag Folder"
        // (not ROOT_PLAYLIST), making movePlaylist resolvable via findPlaylistTreeItemGivenPlaylist.
        var hierarchy = playlistRepository;
        var playlist = hierarchy.findByName("Inner Playlist").orElseThrow();
        var targetFolder = hierarchy.findByName("Target Folder").orElseThrow();

        var treeView = playlistTreeView(fxRobot);

        Platform.runLater(() -> treeView.getRoot().setExpanded(true));
        waitForFxEvents();

        // Simulate what the drop handler does: resolve by id and call movePlaylist
        var id = (Integer) playlist.getId();
        var resolvedOpt = hierarchy.findById(id);
        assertThat(resolvedOpt).isPresent();

        var resolved = resolvedOpt.get();
        var exception = new AtomicReference<Throwable>();
        Platform.runLater(() -> {
            try {
                treeView.movePlaylist(resolved, targetFolder);
            } catch (Throwable t) {
                exception.set(t);
            }
        });
        waitForFxEvents();

        assertThat(exception.get()).isNull();
    }

    @Test
    @DisplayName("PlaylistTreeView moves a top-level playlist into a folder without a null-parent exception")
    void moveTopLevelPlaylistIntoFolderWithoutException(FxRobot fxRobot) {
        waitForFxEvents();

        // "Drag Playlist" sits directly under ROOT_PLAYLIST, so findParentPlaylist returns the root,
        // whose tree node is never one of its own children — the case that previously NPE'd at the
        // null old-parent tree item.
        var hierarchy = playlistRepository;
        var playlist = hierarchy.findByName("Drag Playlist").orElseThrow();
        var targetFolder = hierarchy.findByName("Target Folder").orElseThrow();

        var treeView = playlistTreeView(fxRobot);
        Platform.runLater(() -> treeView.getRoot().setExpanded(true));
        waitForFxEvents();

        var resolved = hierarchy.findById((Integer) playlist.getId()).orElseThrow();
        var exception = new AtomicReference<Throwable>();
        Platform.runLater(() -> {
            try {
                treeView.movePlaylist(resolved, targetFolder);
            } catch (Throwable t) {
                exception.set(t);
            }
        });
        waitForFxEvents();

        assertThat(exception.get()).isNull();
        assertThat(hierarchy.findParentPlaylist(resolved))
                .isPresent()
                .get()
                .extracting(p -> ((ObservablePlaylist) p).getName())
                .isEqualTo(targetFolder.getName());
    }

    @Test
    @DisplayName("PlaylistTreeView moves a top-level folder into another folder, preserving it and its children under the new parent")
    void moveTopLevelFolderIntoFolderKeepsItAndChildren(FxRobot fxRobot) {
        waitForFxEvents();

        // "Drag Folder" is a root-level directory containing "Inner Playlist". Moving a directory
        // previously corrupted the hierarchy (double repository insert) so it vanished from the model.
        var hierarchy = playlistRepository;
        var dragFolder = hierarchy.findByName("Drag Folder").orElseThrow();
        var innerPlaylist = hierarchy.findByName("Inner Playlist").orElseThrow();
        var targetFolder = hierarchy.findByName("Target Folder").orElseThrow();

        var treeView = playlistTreeView(fxRobot);
        Platform.runLater(() -> treeView.getRoot().setExpanded(true));
        waitForFxEvents();

        var resolved = hierarchy.findById((Integer) dragFolder.getId()).orElseThrow();
        var exception = new AtomicReference<Throwable>();
        Platform.runLater(() -> {
            try {
                treeView.movePlaylist(resolved, targetFolder);
            } catch (Throwable t) {
                exception.set(t);
            }
        });
        waitForFxEvents();

        assertThat(exception.get()).isNull();
        // The folder is now under the target, and is no longer a child of the root.
        assertThat(hierarchy.findParentPlaylist(resolved))
                .isPresent()
                .get()
                .extracting(p -> ((ObservablePlaylist) p).getName())
                .isEqualTo(targetFolder.getName());
        // Its child playlist still belongs to it — the subtree was preserved, not dropped.
        assertThat(hierarchy.findParentPlaylist(innerPlaylist))
                .isPresent()
                .get()
                .extracting(p -> ((ObservablePlaylist) p).getName())
                .isEqualTo(dragFolder.getName());
    }

    @Test
    @DisplayName("PlaylistTreeView moves a nested playlist back to the first level without exception")
    void moveNestedPlaylistToFirstLevel(FxRobot fxRobot) {
        waitForFxEvents();

        // Destination is ROOT_PLAYLIST, whose tree node is the root itself — previously unresolvable,
        // so first-level moves threw IllegalStateException.
        var hierarchy = playlistRepository;
        var innerPlaylist = hierarchy.findByName("Inner Playlist").orElseThrow();
        var rootPlaylist = hierarchy.findByName("ROOT_PLAYLIST").orElseThrow();

        var treeView = playlistTreeView(fxRobot);
        Platform.runLater(() -> treeView.getRoot().setExpanded(true));
        waitForFxEvents();

        var resolved = hierarchy.findById((Integer) innerPlaylist.getId()).orElseThrow();
        var exception = new AtomicReference<Throwable>();
        Platform.runLater(() -> {
            try {
                treeView.movePlaylist(resolved, rootPlaylist);
            } catch (Throwable t) {
                exception.set(t);
            }
        });
        waitForFxEvents();

        assertThat(exception.get()).isNull();
        assertThat(hierarchy.findParentPlaylist(resolved))
                .isPresent()
                .get()
                .extracting(p -> ((ObservablePlaylist) p).getName())
                .isEqualTo("ROOT_PLAYLIST");
    }

    @SuppressWarnings("unchecked")
    private PlaylistTreeView playlistTreeView(FxRobot fxRobot) {
        return (PlaylistTreeView) fxRobot.lookup("#playlistTreeView").query();
    }
}

@JavaFxSpringTestConfiguration(includeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                NavigationController.class,
                PlaylistTreeView.class
        })
})
class PlaylistDragAndDropITConfiguration {

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder().build();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) {
        var repository = musicLibrary.playlistHierarchy();
        repository.createPlaylistDirectory("ROOT_PLAYLIST");
        var folder = repository.createPlaylistDirectory("Drag Folder");
        var targetFolder = repository.createPlaylistDirectory("Target Folder");
        var dragPlaylist = repository.createPlaylist("Drag Playlist");
        repository.addPlaylistsToDirectory(Set.of(folder, targetFolder, dragPlaylist), "ROOT_PLAYLIST");
        // "Inner Playlist" is nested inside "Drag Folder" so its parent is resolvable in the tree
        var innerPlaylist = repository.createPlaylist("Inner Playlist");
        repository.addPlaylistsToDirectory(Set.of(innerPlaylist), "Drag Folder");
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
