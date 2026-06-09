package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

/**
 * Unit test for {@link ItunesWizardConfirmStepController} validating the read-only recap
 * step UI rendering and the four-section refresh from the bound {@link ItunesImportDraft}
 * on {@code onEnter} using a standalone pattern (no Spring context required).
 *
 * @author Octavio Calleya
 */
@ExtendWith(ApplicationExtension.class)
@DisplayName("ItunesWizardConfirmStepController")
class ItunesWizardConfirmStepControllerTest {

    ItunesWizardConfirmStepController controller;
    ItunesImportDraft draft;

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ItunesWizardConfirmStep.fxml"));
        loader.setControllerFactory(type -> new ItunesWizardConfirmStepController());
        Parent root = loader.load();
        controller = loader.getController();
        draft = new ItunesImportDraft();
        draft.libraryPath = Path.of("/music/library.xml");
        controller.bind(draft);
        stage.setScene(new Scene(root));
        stage.show();
        waitForFxEvents();
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController renders the library, playlists tree, policy and total sections")
    void rendersLibraryPlaylistsPolicyAndTotalSections() {
        verifyThat("#libraryPathLabel", isVisible());
        verifyThat("#playlistsTree", isVisible());
        verifyThat("#policyList", isVisible());
        verifyThat("#totalLabel", isVisible());
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController is always valid")
    void isAlwaysValid() {
        assertThat(controller.invalidProperty().get()).isFalse();
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController renders library path on onEnter")
    void rendersLibraryPathOnOnEnter() {
        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        assertThat(controller.libraryPathLabel.getText()).isEqualTo(draft.libraryPath.toString());
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController renders leaf playlist nodes with name and track count")
    void rendersLeafPlaylistNodesWithNameAndTrackCount() {
        ItunesPlaylist disco = new ItunesPlaylist(
                "Disco 80s", "id-disco", null, false,
                IntStream.rangeClosed(1, 64).boxed().toList());
        ItunesPlaylist rock = new ItunesPlaylist(
                "Rock 90s", "id-rock", null, false,
                IntStream.rangeClosed(100, 109).boxed().toList());
        draft.selectedPlaylists.addAll(List.of(disco, rock));

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        TreeView<ItunesPlaylist> tree = controller.playlistsTree;
        // Root has 2 top-level children (both leaves, no parent folder selected)
        assertThat(tree.getRoot().getChildren()).hasSize(2);
        List<String> texts = rootChildTexts(tree);
        assertThat(texts)
                .anySatisfy(t -> assertThat(t).contains("Disco 80s").contains("64 tracks"))
                .anySatisfy(t -> assertThat(t).contains("Rock 90s").contains("10 tracks"));
        assertThat(controller.totalLabel.getText()).contains("74").contains("2 playlists");
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController uses singular wording for one-track and one-playlist totals")
    void usesSingularWordingForOneTrackAndOnePlaylistTotals() {
        ItunesPlaylist solo = new ItunesPlaylist(
                "Solo", "id-solo", null, false, List.of(7));
        draft.selectedPlaylists.add(solo);

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        String leafText = rootChildTexts(controller.playlistsTree).get(0);
        assertThat(leafText).contains("1 track").doesNotContain("1 tracks");
        assertThat(controller.totalLabel.getText())
                .contains("1 track")
                .contains("1 playlist")
                .doesNotContain("1 tracks")
                .doesNotContain("1 playlists");
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController renders folder node with name only and leaf as child")
    void rendersFolderNodeWithNameOnlyAndLeafAsChild() {
        ItunesPlaylist folder = new ItunesPlaylist(
                "My Folder", "id-folder", null, true, List.of());
        ItunesPlaylist leaf = new ItunesPlaylist(
                "Chill Hits", "id-chill", "id-folder", false,
                IntStream.rangeClosed(1, 5).boxed().toList());
        draft.selectedPlaylists.addAll(List.of(folder, leaf));

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        TreeView<ItunesPlaylist> tree = controller.playlistsTree;
        // Folder is top-level; leaf is nested under it
        assertThat(tree.getRoot().getChildren()).hasSize(1);
        TreeItem<ItunesPlaylist> folderItem = tree.getRoot().getChildren().get(0);
        // Folder node shows name only — no track count
        assertThat(ItunesPlaylistCells.displayText(folderItem.getValue()))
                .isEqualTo("My Folder")
                .doesNotContain("track");
        assertThat(folderItem.getChildren()).hasSize(1);
        assertThat(ItunesPlaylistCells.displayText(folderItem.getChildren().get(0).getValue()))
                .contains("Chill Hits").contains("5 tracks");
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController renders policy lines reflecting draft booleans")
    void rendersPolicyLinesReflectingDraftBooleans() {
        draft.useFileMetadata = false;
        draft.holdPlayCount = true;
        draft.writeMetadata = false;

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        assertThat(controller.policyList.getChildren()).hasSize(3);
        String all = policyText();
        assertThat(all)
                .containsIgnoringCase("itunes library")
                .containsIgnoringCase("preserve")
                .containsIgnoringCase("don't write");
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController refreshes the tree when onEnter is called twice with different draft state")
    void refreshesTreeWhenOnEnterIsCalledTwiceWithDifferentDraftState() {
        ItunesPlaylist first = new ItunesPlaylist(
                "First", "id-1", null, false, List.of(1, 2));
        draft.selectedPlaylists.add(first);

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();
        assertThat(controller.playlistsTree.getRoot().getChildren()).hasSize(1);

        ItunesPlaylist second = new ItunesPlaylist(
                "Second", "id-2", null, false, List.of(3, 4, 5));
        draft.selectedPlaylists.add(second);
        draft.useFileMetadata = false;

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        assertThat(controller.playlistsTree.getRoot().getChildren()).hasSize(2);
        assertThat(controller.totalLabel.getText()).contains("5").contains("2 playlists");
        assertThat(policyText()).containsIgnoringCase("itunes library");
    }

    @Test
    @DisplayName("ItunesWizardConfirmStepController hosts all selected playlists in a bounded TreeView with many playlists")
    void hostsAllSelectedPlaylistsInBoundedTreeViewWithManyPlaylists() {
        IntStream.rangeClosed(1, 30).forEach(i ->
                draft.selectedPlaylists.add(
                        new ItunesPlaylist("Playlist " + i, "id-" + i, null, false, List.of(i))));

        Platform.runLater(() -> controller.onEnter());
        waitForFxEvents();

        TreeView<ItunesPlaylist> tree = controller.playlistsTree;

        // All 30 playlists must be rendered as top-level items (no folder selected)
        assertThat(tree.getRoot().getChildren()).hasSize(30);

        // The tree must be bounded — maxHeight must be a finite, usable value
        assertThat(tree.getMaxHeight()).isEqualTo(150.0);
        assertThat(tree.getPrefHeight()).isEqualTo(150.0);
    }

    private List<String> rootChildTexts(TreeView<ItunesPlaylist> tree) {
        return tree.getRoot().getChildren().stream()
                .map(item -> ItunesPlaylistCells.displayText(item.getValue()))
                .toList();
    }

    private String policyText() {
        return controller.policyList.getChildren().stream()
                .map(n -> ((Label) n).getText())
                .reduce("", (a, b) -> a + "|" + b);
    }
}
