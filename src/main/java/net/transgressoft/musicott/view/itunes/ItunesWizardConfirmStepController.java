package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only step that recaps the user's choices before the import is dispatched. Renders
 * four sections — library path, selected playlists as a structure-mirroring read-only
 * {@link TreeView}, three policy lines in plain language, and an aggregate track total.
 * Every section is rebuilt on each {@code onEnter} so a Back-navigation that mutates an
 * earlier step is always reflected here.
 *
 * <p>The playlist tree mirrors the iTunes folder hierarchy: folder nodes show the name
 * only; leaf nodes show {@code name  (X tracks)}. Parent-linking is computed strictly
 * among the <em>selected</em> playlists — a leaf whose folder was not selected attaches
 * to the hidden root. Cell rendering is shared with the selection step via
 * {@link ItunesPlaylistCells} to guarantee visual parity.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/ItunesWizardConfirmStep.fxml")
@Controller
public class ItunesWizardConfirmStepController {

    private final SimpleBooleanProperty invalid = new SimpleBooleanProperty(this, "invalid", false);

    @FXML
    Label libraryPathLabel;
    @FXML
    TreeView<ItunesPlaylist> playlistsTree;
    @FXML
    VBox policyList;
    @FXML
    Label totalLabel;

    private ItunesImportDraft draft;

    /**
     * Binds this step controller to the supplied wizard draft. Called by the wizard owner
     * once per wizard session, before the first page is rendered.
     */
    void bind(ItunesImportDraft draft) {
        this.draft = draft;
    }

    /**
     * Refreshes all four recap sections from the current draft. Called every time this
     * step becomes the current page so a Back-navigation that changed any earlier step
     * is always reflected on screen.
     */
    void onEnter() {
        if (draft == null) {
            return;
        }
        libraryPathLabel.setText(draft.libraryPath != null ? draft.libraryPath.toString() : "");

        buildPlaylistsTree();

        policyList.getChildren().clear();
        policyList.getChildren().add(wrappingLabel(draft.useFileMetadata
                ? "• Read metadata from the audio files"
                : "• Use the metadata recorded in the iTunes library"));
        policyList.getChildren().add(wrappingLabel(draft.holdPlayCount
                ? "• Preserve existing play counts"
                : "• Reset play counts on import"));
        policyList.getChildren().add(wrappingLabel(draft.writeMetadata
                ? "• Write metadata back to audio files"
                : "• Don't write metadata back to files"));

        int totalTracks = draft.selectedPlaylists.stream()
                .mapToInt(p -> p.getTrackIds().size())
                .sum();
        int playlistCount = draft.selectedPlaylists.size();
        String tracksWord = totalTracks == 1 ? "track" : "tracks";
        String playlistsWord = playlistCount == 1 ? "playlist" : "playlists";
        totalLabel.setText("Importing " + totalTracks + " " + tracksWord
                + " across " + playlistCount + " " + playlistsWord);
    }

    public ReadOnlyBooleanProperty invalidProperty() {
        return invalid;
    }

    /**
     * Builds the read-only playlist tree from {@code draft.selectedPlaylists}. Parent-linking
     * uses {@code parentPersistentId} within the selected set only — a leaf whose folder was
     * not selected attaches directly to the hidden root. All nodes are expanded and sorted
     * by name for a predictable read-only recap layout.
     */
    private void buildPlaylistsTree() {
        TreeItem<ItunesPlaylist> root = new TreeItem<>(null);

        Map<String, TreeItem<ItunesPlaylist>> byId = new LinkedHashMap<>();
        for (ItunesPlaylist p : draft.selectedPlaylists) {
            TreeItem<ItunesPlaylist> item = new TreeItem<>(p);
            item.setExpanded(true);
            byId.put(p.getPersistentId(), item);
        }
        for (ItunesPlaylist p : draft.selectedPlaylists) {
            TreeItem<ItunesPlaylist> item = byId.get(p.getPersistentId());
            String parentId = p.getParentPersistentId();
            // Only attach to a parent that is itself in the selected set; otherwise
            // the node belongs at the top level of this recap tree.
            TreeItem<ItunesPlaylist> parent = (parentId != null) ? byId.getOrDefault(parentId, root) : root;
            parent.getChildren().add(item);
        }
        sortChildren(root);

        playlistsTree.setShowRoot(false);
        playlistsTree.setRoot(root);
        playlistsTree.setCellFactory(tv -> new ItunesPlaylistCells.ReadOnlyTreeCell());
    }

    private void sortChildren(TreeItem<ItunesPlaylist> node) {
        node.getChildren().sort(Comparator.comparing(t -> t.getValue() == null ? "" : t.getValue().getName()));
        for (TreeItem<ItunesPlaylist> child : node.getChildren()) {
            sortChildren(child);
        }
    }

    /** Builds a recap line label that wraps long policy strings. */
    private static Label wrappingLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
