package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Unit test for {@link ItunesWizardPlaylistsStepController} validating the
 * {@link org.controlsfx.control.CheckTreeView}-based picker rendering, hierarchy mirroring,
 * and the cascading-check semantics that flow into the draft.
 *
 * @author Octavio Calleya
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("ItunesWizardPlaylistsStepController")
class ItunesWizardPlaylistsStepControllerTest {

    ItunesWizardPlaylistsStepController controller;
    ItunesImportDraft draft;

    ItunesPlaylist vinylRipsFolder;
    ItunesPlaylist techFolder;
    ItunesPlaylist adamX;
    ItunesPlaylist sote;
    ItunesPlaylist spirit;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItunesWizardPlaylistsStep.fxml"));
        loader.setControllerFactory(type -> new ItunesWizardPlaylistsStepController());
        Parent root = loader.load();
        controller = loader.getController();
        draft = new ItunesImportDraft();
        controller.bind(draft);

        vinylRipsFolder = new ItunesPlaylist("Vinyl Rips", "id-vinyl", null, true, Collections.emptyList());
        techFolder = new ItunesPlaylist("Tech", "id-tech", "id-vinyl", true, Collections.emptyList());
        adamX = new ItunesPlaylist("Adam X", "id-adamx", "id-tech", false, List.of(1, 2, 3));
        sote = new ItunesPlaylist("Sote", "id-sote", "id-vinyl", false, List.of(4));
        spirit = new ItunesPlaylist("SPIRIT", "id-spirit", null, false, List.of(5, 6, 7, 8));

        stage.setScene(new Scene(root));
        stage.show();
        waitForFxEvents();
    }

    @Test
    @DisplayName("renders the check tree, select-all / deselect-all buttons, and selection counter")
    void rendersTheCheckTreeButtonsAndCounter() {
        verifyThat("#playlistsTree", isVisible());
        verifyThat("#selectAllButton", isVisible());
        verifyThat("#deselectAllButton", isVisible());
        verifyThat("#selectionCountLabel", isVisible());
    }

    @Test
    @DisplayName("is invalid while no playlist is checked")
    void isInvalidWhileNothingIsChecked() {
        Platform.runLater(() -> controller.pickPlaylists(List.of(vinylRipsFolder, techFolder, adamX, sote, spirit)));
        waitForFxEvents();

        assertThat(controller.invalidProperty().get()).isTrue();
    }

    @Test
    @DisplayName("builds the tree mirroring the iTunes folder hierarchy")
    void buildsTheTreeMirroringTheItunesFolderHierarchy() {
        Platform.runLater(() -> controller.pickPlaylists(List.of(spirit, vinylRipsFolder, techFolder, adamX, sote)));
        waitForFxEvents();

        TreeItem<ItunesPlaylist> root = controller.playlistsTree.getRoot();
        List<String> rootChildNames = root.getChildren().stream().map(t -> t.getValue().getName()).toList();
        assertThat(rootChildNames).containsExactly("SPIRIT", "Vinyl Rips");

        TreeItem<ItunesPlaylist> vinylRips = root.getChildren().stream()
                .filter(t -> "Vinyl Rips".equals(t.getValue().getName())).findFirst().orElseThrow();
        List<String> vinylChildren = vinylRips.getChildren().stream().map(t -> t.getValue().getName()).toList();
        assertThat(vinylChildren).containsExactly("Sote", "Tech");

        TreeItem<ItunesPlaylist> tech = vinylRips.getChildren().stream()
                .filter(t -> "Tech".equals(t.getValue().getName())).findFirst().orElseThrow();
        assertThat(tech.getChildren().stream().map(t -> t.getValue().getName())).containsExactly("Adam X");
    }

    @Test
    @DisplayName("checking a folder cascades to all descendant items")
    void checkingAFolderCascadesToAllDescendantItems() {
        Platform.runLater(() -> {
            controller.pickPlaylists(List.of(spirit, vinylRipsFolder, techFolder, adamX, sote));
            CheckBoxTreeItem<ItunesPlaylist> vinylNode = (CheckBoxTreeItem<ItunesPlaylist>)
                    findInTree(controller.playlistsTree.getRoot(), vinylRipsFolder);
            vinylNode.setSelected(true);
        });
        waitForFxEvents();

        List<String> checkedNames = controller.playlistsTree.getCheckModel().getCheckedItems().stream()
                .map(item -> item.getValue().getName()).toList();
        assertThat(checkedNames).containsExactlyInAnyOrder("Vinyl Rips", "Tech", "Adam X", "Sote");
    }

    @Test
    @DisplayName("checking a leaf with un-checked siblings keeps the parent folder indeterminate, not in checkedItems")
    void checkingALeafWithUncheckedSiblingsKeepsTheParentIndeterminate() {
        Platform.runLater(() -> {
            controller.pickPlaylists(List.of(spirit, vinylRipsFolder, techFolder, adamX, sote));
            CheckBoxTreeItem<ItunesPlaylist> soteNode = (CheckBoxTreeItem<ItunesPlaylist>)
                    findInTree(controller.playlistsTree.getRoot(), sote);
            soteNode.setSelected(true);
        });
        waitForFxEvents();

        List<String> checkedNames = controller.playlistsTree.getCheckModel().getCheckedItems().stream()
                .map(item -> item.getValue().getName()).toList();
        assertThat(checkedNames).containsExactly("Sote");
    }

    @Test
    @DisplayName("select-all checks every node in the tree")
    void selectAllChecksEveryNodeInTheTree() {
        Platform.runLater(() -> {
            controller.pickPlaylists(List.of(spirit, vinylRipsFolder, techFolder, adamX, sote));
            controller.checkAll();
        });
        waitForFxEvents();

        assertThat(controller.playlistsTree.getCheckModel().getCheckedItems()).hasSize(5);
    }

    @Test
    @DisplayName("deselect-all clears every check")
    void deselectAllClearsEveryCheck() {
        Platform.runLater(() -> {
            controller.pickPlaylists(List.of(spirit, vinylRipsFolder, techFolder, adamX, sote));
            controller.checkAll();
            controller.uncheckAll();
        });
        waitForFxEvents();

        assertThat(controller.playlistsTree.getCheckModel().getCheckedItems()).isEmpty();
    }

    @Test
    @DisplayName("onExit writes the checked playlists into the draft, including any folder marked fully selected by cascade")
    void onExitWritesTheCheckedPlaylistsIntoTheDraft() {
        Platform.runLater(() -> {
            controller.pickPlaylists(List.of(spirit, vinylRipsFolder, techFolder, adamX, sote));
            CheckBoxTreeItem<ItunesPlaylist> spiritNode = (CheckBoxTreeItem<ItunesPlaylist>)
                    findInTree(controller.playlistsTree.getRoot(), spirit);
            spiritNode.setSelected(true);
            CheckBoxTreeItem<ItunesPlaylist> adamXNode = (CheckBoxTreeItem<ItunesPlaylist>)
                    findInTree(controller.playlistsTree.getRoot(), adamX);
            adamXNode.setSelected(true);
            controller.onExit();
        });
        waitForFxEvents();

        List<String> draftNames = draft.selectedPlaylists.stream().map(ItunesPlaylist::getName).toList();
        // Tech also lands in the draft because Adam X is its only child — JavaFX CheckBoxTreeItem
        // marks Tech fully selected when all its children are selected. The import service in
        // music-commons de-duplicates ancestors, so this is harmless.
        assertThat(draftNames).containsExactlyInAnyOrder("SPIRIT", "Adam X", "Tech");
    }

    @Test
    @DisplayName("onEnter restores previously-selected playlists into the tree")
    void onEnterRestoresPreviouslySelectedPlaylistsIntoTheTree() {
        Platform.runLater(() -> {
            draft.parsedLibrary = new net.transgressoft.commons.music.itunes.ItunesLibrary(
                    java.util.Collections.emptyMap(),
                    List.of(spirit, vinylRipsFolder, techFolder, adamX, sote)
            );
            draft.selectedPlaylists.clear();
            draft.selectedPlaylists.add(spirit);
            draft.selectedPlaylists.add(sote);
            controller.onEnter();
        });
        waitForFxEvents();

        List<String> checkedNames = new ArrayList<>(controller.playlistsTree.getCheckModel().getCheckedItems().stream()
                .map(item -> item.getValue().getName()).toList());
        assertThat(checkedNames).containsExactlyInAnyOrder("SPIRIT", "Sote");
    }

    private static TreeItem<ItunesPlaylist> findInTree(TreeItem<ItunesPlaylist> node, ItunesPlaylist target) {
        if (node == null) return null;
        if (target.equals(node.getValue())) return node;
        for (TreeItem<ItunesPlaylist> child : node.getChildren()) {
            TreeItem<ItunesPlaylist> hit = findInTree(child, target);
            if (hit != null) return hit;
        }
        return null;
    }
}
