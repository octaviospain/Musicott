package net.transgressoft.musicott.view.itunes;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.CheckBoxTreeCell;
import net.rgielen.fxweaver.core.FxmlView;
import org.controlsfx.control.CheckTreeView;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Step controller for choosing which iTunes playlists to import. Renders a
 * {@link CheckTreeView} that mirrors the iTunes folder hierarchy: folder playlists
 * appear as expandable nodes and regular playlists as leaves with a track-count
 * suffix. Checking a folder cascades to all descendants; checking a leaf records
 * the user's intent without auto-checking ancestor folders, and the import service
 * handles preserving the surrounding hierarchy when only leaves are selected.
 *
 * <p>The wizard's per-page validity gate is mirrored on this controller's
 * {@code invalidProperty()}, which is {@code true} while no node is checked.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/ItunesWizardPlaylistsStep.fxml")
@Controller
public class ItunesWizardPlaylistsStepController {

    private final SimpleBooleanProperty invalid = new SimpleBooleanProperty(this, "invalid", true);

    @FXML
    CheckTreeView<ItunesPlaylist> playlistsTree;
    @FXML
    Button selectAllButton;
    @FXML
    Button deselectAllButton;
    @FXML
    Label selectionCountLabel;

    private ItunesImportDraft draft;

    @FXML
    public void initialize() {
        playlistsTree.setShowRoot(false);
        playlistsTree.setRoot(new TreeItem<>(null));
        playlistsTree.setCellFactory(tv -> new ItunesPlaylistTreeCell());
        invalid.bind(Bindings.isEmpty(playlistsTree.getCheckModel().getCheckedItems()));
        selectionCountLabel.textProperty().bind(
                Bindings.size(playlistsTree.getCheckModel().getCheckedItems()).asString().concat(" selected")
        );
        selectAllButton.setOnAction(e -> checkAll());
        deselectAllButton.setOnAction(e -> uncheckAll());
    }

    /**
     * Binds this step controller to the supplied wizard draft. Called by the wizard
     * owner once per wizard session, before the first page is rendered.
     */
    void bind(ItunesImportDraft draft) {
        this.draft = draft;
    }

    /**
     * Called by the wizard owner when this step becomes the current page. Rebuilds
     * the tree from the parsed library and restores any previously-checked items
     * from {@code draft.selectedPlaylists} so Back-navigation preserves the user's
     * picks.
     */
    void onEnter() {
        if (draft == null || draft.parsedLibrary == null) {
            // Wipe any leftovers from a previous wizard session so re-entering the step with a
            // fresh / cleared draft doesn't surface stale playlists or write them back into the
            // new draft on onExit().
            playlistsTree.getCheckModel().clearChecks();
            playlistsTree.getRoot().getChildren().clear();
            return;
        }
        rebuildTree(draft.parsedLibrary.getPlaylists());
        restoreChecks(draft.selectedPlaylists);
    }

    /**
     * Called by the wizard owner when this step is leaving the current page (Next or Back).
     * Writes the checked playlists into {@code draft.selectedPlaylists} so subsequent
     * steps see the user's current selection.
     */
    void onExit() {
        if (draft == null) {
            return;
        }
        draft.selectedPlaylists.clear();
        for (TreeItem<ItunesPlaylist> item : playlistsTree.getCheckModel().getCheckedItems()) {
            ItunesPlaylist playlist = item.getValue();
            if (playlist != null) {
                draft.selectedPlaylists.add(playlist);
            }
        }
    }

    public ReadOnlyBooleanProperty invalidProperty() {
        return invalid;
    }

    /**
     * Rebuilds the tree from the supplied playlists and clears any previous selection.
     * Public so tests and the wizard owner can drive selection without a real
     * {@link javafx.stage.FileChooser} round-trip.
     */
    public void pickPlaylists(List<ItunesPlaylist> playlists) {
        rebuildTree(playlists);
    }

    /**
     * Checks every node in the tree. Sets the top-level {@link CheckBoxTreeItem}s as selected;
     * JavaFX cascades the selection down to every descendant.
     */
    public void checkAll() {
        for (TreeItem<ItunesPlaylist> child : playlistsTree.getRoot().getChildren()) {
            if (child instanceof CheckBoxTreeItem<ItunesPlaylist> cb) {
                cb.setSelected(true);
            }
        }
    }

    /**
     * Clears every check in the tree. Mirrors {@link #checkAll()} by setting the top-level
     * items unselected; the unselect cascades down to all descendants.
     */
    public void uncheckAll() {
        for (TreeItem<ItunesPlaylist> child : playlistsTree.getRoot().getChildren()) {
            if (child instanceof CheckBoxTreeItem<ItunesPlaylist> cb) {
                cb.setSelected(false);
            }
        }
    }

    private void rebuildTree(List<ItunesPlaylist> playlists) {
        // Mutate the existing root rather than replacing it — replacing the root resets
        // CheckTreeView's CheckModel and breaks any property already bound to its
        // `checkedItems` (the validity gate observes that list).
        TreeItem<ItunesPlaylist> root = playlistsTree.getRoot();
        playlistsTree.getCheckModel().clearChecks();
        root.getChildren().clear();

        Map<String, CheckBoxTreeItem<ItunesPlaylist>> byPersistentId = new LinkedHashMap<>();
        for (ItunesPlaylist p : playlists) {
            CheckBoxTreeItem<ItunesPlaylist> item = new CheckBoxTreeItem<>(p);
            item.setExpanded(true);
            byPersistentId.put(p.getPersistentId(), item);
        }
        for (ItunesPlaylist p : playlists) {
            CheckBoxTreeItem<ItunesPlaylist> item = byPersistentId.get(p.getPersistentId());
            String parentId = p.getParentPersistentId();
            TreeItem<ItunesPlaylist> parent = root;
            if (parentId != null) {
                CheckBoxTreeItem<ItunesPlaylist> mappedParent = byPersistentId.get(parentId);
                if (mappedParent != null) {
                    parent = mappedParent;
                }
            }
            parent.getChildren().add(item);
        }
        sortChildren(root);
    }

    private void sortChildren(TreeItem<ItunesPlaylist> node) {
        node.getChildren().sort(Comparator.comparing(t -> t.getValue() == null ? "" : t.getValue().getName()));
        for (TreeItem<ItunesPlaylist> child : node.getChildren()) {
            sortChildren(child);
        }
    }

    private void restoreChecks(List<ItunesPlaylist> previouslySelected) {
        if (previouslySelected.isEmpty()) {
            return;
        }
        Map<String, ItunesPlaylist> previouslySelectedById = new HashMap<>();
        for (ItunesPlaylist p : previouslySelected) {
            previouslySelectedById.put(p.getPersistentId(), p);
        }
        forEachItem(playlistsTree.getRoot(), item -> {
            ItunesPlaylist p = item.getValue();
            if (p != null && previouslySelectedById.containsKey(p.getPersistentId())) {
                playlistsTree.getCheckModel().check(item);
            }
        });
    }

    private void forEachItem(TreeItem<ItunesPlaylist> node, Consumer<TreeItem<ItunesPlaylist>> action) {
        if (node == null) {
            return;
        }
        action.accept(node);
        for (TreeItem<ItunesPlaylist> child : new ArrayList<>(node.getChildren())) {
            forEachItem(child, action);
        }
    }

    private static final class ItunesPlaylistTreeCell extends CheckBoxTreeCell<ItunesPlaylist> {

        @Override
        public void updateItem(ItunesPlaylist item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else if (item.isFolder()) {
                setText(item.getName());
            } else {
                int trackCount = item.getTrackIds().size();
                String unit = trackCount == 1 ? "track" : "tracks";
                setText(item.getName() + "  (" + trackCount + " " + unit + ")");
            }
        }
    }
}
