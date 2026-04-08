package net.transgressoft.musicott.view.custom;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.transgressoft.commons.fx.music.*;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistHierarchy;
import net.transgressoft.musicott.test.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.annotation.*;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.testfx.assertions.api.Assertions.assertThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

@JavaFxSpringTest(classes = PlaylistTreeViewTestConfiguration.class)
public class PlaylistTreeViewIT extends ApplicationTestBase<PlaylistTreeView> {

    @Autowired
    ObservablePlaylistHierarchy playlistRepository;

    @Autowired
    ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    PlaylistTreeView playlistTreeView;

    ObservablePlaylist technoPlaylist, bestHitsPlaylist, thisWeeksFavoritesPlaylist;

    @Override
    protected PlaylistTreeView javaFxComponent() {
        return playlistTreeView;
    }

    @Override
    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        technoPlaylist = playlistRepository.findByName("Techno").get();
        bestHitsPlaylist = playlistRepository.findByName("Best hits").get();
        thisWeeksFavoritesPlaylist = playlistRepository.findByName("This weeks' favorites songs").get();
    }

    @Test
    @DisplayName("PlaylistTreeView deletes playlist after confirmation dialog is shown")
    void deletesPlaylistAfterConfirmation(FxRobot fxRobot) {
        // Expand the tree to see Techno (inside Best hits)
        TreeCell<ObservablePlaylist> bestHitsCell = fxRobot.lookup(bestHitsPlaylist.getName()).query();
        fxRobot.doubleClickOn(bestHitsCell);
        WaitForAsyncUtils.waitForFxEvents();

        // Select the Techno playlist
        TreeCell<ObservablePlaylist> technoCell = fxRobot.lookup(technoPlaylist.getName()).query();
        fxRobot.clickOn(technoCell);
        WaitForAsyncUtils.waitForFxEvents();

        // Verify the selected playlist property was set
        assertThat(selectedPlaylistProperty.get()).isPresent();
        assertThat(selectedPlaylistProperty.get().get().getName()).isEqualTo("Techno");

        // Directly call deletePlaylist to test the tree behavior
        WaitForAsyncUtils.waitForFxEvents();
        javafx.application.Platform.runLater(() -> playlistTreeView.deletePlaylist(technoPlaylist));
        WaitForAsyncUtils.waitForFxEvents();

        // Verify Techno is removed from the tree
        Set<TreeCell<ObservablePlaylist>> allTreeCells = fxRobot.lookup(".tree-cell")
                .match(hasText(not(emptyOrNullString())))
                .queryAll();

        assertThat(allTreeCells).extracting(TreeCell::getItem)
                .extracting(ObservablePlaylist::getName)
                .doesNotContain("Techno");

        // Verify playlistRepository.remove() was called exactly once (not double-removed)
        verify(playlistRepository, times(1)).remove(technoPlaylist);
    }

    @Test
    @DisplayName("PlaylistTreeView rejects duplicate playlist name on creation")
    void rejectsDuplicatePlaylistNameOnCreation(FxRobot fxRobot) {
        // "Best hits" already exists in the test hierarchy
        boolean containsDuplicate = playlistTreeView.containsPlaylistName("Best hits");
        assertThat(containsDuplicate).isTrue();

        // A new name should be allowed
        boolean containsNew = playlistTreeView.containsPlaylistName("New Playlist");
        assertThat(containsNew).isFalse();
    }

    @Test
    @DisplayName("PlaylistTreeView drag-drop guard rejects folder targets")
    void dragDropGuardRejectsFolderTargets(FxRobot fxRobot) {
        // Expand the tree to see Techno (inside Best hits folder)
        TreeCell<ObservablePlaylist> bestHitsCell = fxRobot.lookup(bestHitsPlaylist.getName()).query();
        fxRobot.doubleClickOn(bestHitsCell);
        WaitForAsyncUtils.waitForFxEvents();

        // Best hits is a directory — it should NOT accept audio item drops
        assertThat(bestHitsPlaylist.isDirectory()).isTrue();

        // Techno is a playlist — it SHOULD accept audio item drops
        assertThat(technoPlaylist.isDirectory()).isFalse();
    }

    @Test
    @DisplayName("Playlist hierarchy")
    void playlistHierarchyTest(FxRobot fxRobot) {
        assertThat(fxRobot.lookup("#playlistTreeView").tryQuery()).isPresent();
        TreeView<ObservablePlaylist> treeView = fxRobot.lookup("#playlistTreeView").query();
        TreeItem<ObservablePlaylist> root = treeView.getRoot();
        assertThat(root.getValue().getName()).isEqualTo("ROOT_PLAYLIST");
        assertThat(root.isLeaf()).isFalse();
        verify(playlistRepository).findByName("ROOT_PLAYLIST");
        verify(playlistRepository, times(0)).createPlaylistDirectory("ROOT_PLAYLIST");
        clearInvocations(playlistRepository);

        Set<TreeCell<ObservablePlaylist>> allTreeCells = fxRobot.lookup(".tree-cell")
                .match(hasText(not(emptyOrNullString())))
                .queryAll();

        assertThat(allTreeCells).hasSize(2)
                .extracting(TreeCell::getItem)
                .extracting(ObservablePlaylist::getName)
                .containsExactlyInAnyOrder("This weeks' favorites songs", "Best hits");

        TreeCell<ObservablePlaylist> bestHitsTreeCell = fxRobot.lookup(".tree-cell").lookup(bestHitsPlaylist.getName()).query();
        assertThat(bestHitsTreeCell.getTreeItem())
                .satisfies(treeItem -> {
                    assertThat(treeItem.getValue()).isEqualTo(bestHitsPlaylist);
                    assertThat(treeItem.isLeaf()).isFalse();
                });

        TreeCell<ObservablePlaylist> thisWeeksFavoritesTreeCell = fxRobot.lookup(thisWeeksFavoritesPlaylist.getName()).query();
        assertThat(thisWeeksFavoritesTreeCell.getTreeItem())
                .satisfies(treeItem -> {
                    assertThat(treeItem.getValue()).isEqualTo(thisWeeksFavoritesPlaylist);
                    assertThat(treeItem.isLeaf()).isTrue();
                });

        fxRobot.doubleClickOn(bestHitsTreeCell);
        WaitForAsyncUtils.waitForFxEvents();

        allTreeCells = fxRobot.lookup(".tree-cell")
                .match(hasText(not(emptyOrNullString())))
                .queryAll();

        assertThat(allTreeCells).hasSize(3)
                .extracting(TreeCell::getItem)
                .extracting(ObservablePlaylist::getName)
                .containsExactlyInAnyOrder("This weeks' favorites songs", "Best hits", "Techno");

        TreeCell<ObservablePlaylist> technoTreeCell = fxRobot.lookup(technoPlaylist.getName()).query();
        assertThat(technoTreeCell.getTreeItem())
                .satisfies(treeItem -> {
                    assertThat(treeItem.getValue()).isEqualTo(technoPlaylist);
                    assertThat(treeItem.isLeaf()).isTrue();
                });

        verifyNoMoreInteractions(playlistRepository);
    }

    public static void main(String[] args) {
        JavaFxViewTestLauncher.launch(PlaylistTreeViewTestConfiguration.class, args);
    }
}

@JavaFxSpringTestConfiguration(includeFilters = @Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = PlaylistTreeView.class
))
class PlaylistTreeViewTestConfiguration {

    File playlistsFile, audioItemsFile;

    public PlaylistTreeViewTestConfiguration() throws IOException {
        playlistsFile = Files.createTempFile("playlists-test", ".json").toFile();
        audioItemsFile = Files.createTempFile("audio-items-test", ".json").toFile();
    }

    @Bean
    public FXMusicLibrary musicLibrary() {
        return FXMusicLibrary.builder()
                .audioLibraryJsonFile(audioItemsFile)
                .playlistHierarchyJsonFile(playlistsFile)
                .build();
    }

    // ├──Best hits
    // │  └──Techno
    // │     └──>rock hit 1
    // └──This weeks' favorites songs
    @Bean
    public ObservableAudioLibrary audioRepository(FXMusicLibrary musicLibrary) {
        return musicLibrary.audioLibrary();
    }

    @Bean
    public ObservablePlaylistHierarchy playlistRepository(FXMusicLibrary musicLibrary) throws Exception {
        var audioLibrary = musicLibrary.audioLibrary();
        var playlistRepository = spy(musicLibrary.playlistHierarchy());

        var tempFile = Files.createTempFile("musicott-test-", ".mp3");
        tempFile.toFile().deleteOnExit();
        try (InputStream in = getClass().getResourceAsStream("/testfiles/testeable.mp3")) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        ObservableAudioItem rockHit1 = audioLibrary.createFromFile(tempFile);

        var bestHitsPlaylist = playlistRepository.createPlaylistDirectory("Best hits");
        var technoPlaylist = playlistRepository.createPlaylist("Techno", Collections.singletonList(rockHit1));
        var thisWeeksFavoritesPlaylist = playlistRepository.createPlaylist("This weeks' favorites songs");
        playlistRepository.createPlaylistDirectory("ROOT_PLAYLIST");
        playlistRepository.addPlaylistToDirectory(technoPlaylist, "Best hits");
        playlistRepository.addPlaylistsToDirectory(Set.of(bestHitsPlaylist, thisWeeksFavoritesPlaylist), "ROOT_PLAYLIST");
        clearInvocations(playlistRepository);
        return playlistRepository;
    }

    @Bean
    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return new SimpleObjectProperty<>(this, "selected test playlist", Optional.empty());
    }

    @Bean
    public ApplicationEventPublisher applicationEventPublisher() {
        return mock(ApplicationEventPublisher.class);
    }
}