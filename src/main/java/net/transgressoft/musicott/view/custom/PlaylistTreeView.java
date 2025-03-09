/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package net.transgressoft.musicott.view.custom;

import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylistJsonRepository;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.view.custom.table.AudioItemTableViewBase;

import com.google.common.collect.ImmutableList;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.scene.input.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * Class that extends from a {@link TreeView} representing a tree of
 * {@link ObservablePlaylist} items, which some of them are folders and could have other
 * playlists inside them.
 *
 * @author Octavio Calleya
 */
@Component
public class PlaylistTreeView extends TreeView<ObservablePlaylist> {

    private static final DataFormat PLAYLIST_DATA_FORMAT = new DataFormat("application/observable-playlist");

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final ObservablePlaylistJsonRepository playlistRepository;
    private final ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public PlaylistTreeView(ObservablePlaylistJsonRepository playlistRepository, ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty) {
        this.playlistRepository = playlistRepository;
        this.selectedPlaylistProperty = selectedPlaylistProperty;
        setCellFactory(PlaylistTreeViewCell::new);
        setContextMenu(new PlaylistTreeViewContextMenu());
        setRoot(createRootPlaylistTreeViewItem());
        setShowRoot(false);
        setEditable(true);
        setId("playlistTreeView");
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        subscribe(getSelectionModel().selectedItemProperty(), newItem -> {
            if (newItem != null) {
                selectedPlaylistProperty.set(Optional.of(newItem.getValue()));
            }
            else {
                selectedPlaylistProperty.set(Optional.empty());
            }
        });
    }

    private PlaylistTreeViewItem createRootPlaylistTreeViewItem() {
        ObservablePlaylist rootPlaylist = playlistRepository.findByName("ROOT_PLAYLIST")
                .orElseGet(() -> playlistRepository.createPlaylistDirectory("ROOT_PLAYLIST"));

        var rootTreeVieItem = new PlaylistTreeViewItem(rootPlaylist);

        addPlaylistsToTreeViewRecursively(rootPlaylist.getPlaylists(), rootTreeVieItem);

        return rootTreeVieItem;
    }

    private void addPlaylistsToTreeViewRecursively(Set<ObservablePlaylist> playlists, PlaylistTreeViewItem playlistTreeViewItem) {
        playlists.forEach(it -> {
            var newPlaylistTreeViewItem = new PlaylistTreeViewItem(it);
            playlistTreeViewItem.getChildren().add(newPlaylistTreeViewItem);
            if (it.isDirectory()) {
                addPlaylistsToTreeViewRecursively(it.getPlaylists(), newPlaylistTreeViewItem);
            }
        });
    }

    /**
     * Includes a new playlist into the TreeView. If playlist was selected during the creation,
     * and if it is a {@link ObservablePlaylist}, it will get added inside that one.
     * Otherwise, it will be added to the first level hierarchy.
     *
     * @param newPlaylist The new {@link ObservablePlaylist} to include in this TreeView
     */
    public void addNewPlaylist(ObservablePlaylist newPlaylist) {
        selectedPlaylistProperty.get().ifPresentOrElse(selectedPlaylist -> addNewPlaylist(selectedPlaylist, newPlaylist),
                                                       () -> addNewPlaylistsToFirstLevel(newPlaylist));
    }

    private void addNewPlaylist(ObservablePlaylist selectedPlaylist, ObservablePlaylist newPlaylist) {
        if (selectedPlaylist.isDirectory()) {
            var playlistFolderTreeViewItem = findPlaylistTreeItemGivenPlaylist(selectedPlaylist);
            if (playlistFolderTreeViewItem != null) {
                addNewPlaylist(newPlaylist, playlistFolderTreeViewItem);
            } else {
                addNewPlaylistsToFirstLevel(newPlaylist);
            }
        } else {
            addNewPlaylistsToFirstLevel(newPlaylist);
        }
    }

    /**
     * Includes a {@link Set} of {@link ObservablePlaylist}s into the first level hierarchy of this TreeView.
     *
     * @param newPlaylist The new {@link ObservablePlaylist}s to include in the TreeView
     */
    private void addNewPlaylistsToFirstLevel(ObservablePlaylist newPlaylist) {
        addNewPlaylist(newPlaylist, getRoot().getValue());
    }

    /**
     * Includes a new {@link ObservablePlaylist} into an existing {@link PlaylistTreeViewItem} in this TreeView.
     * If the playlist to add is an instance of {@link ObservablePlaylist} all its included playlists
     * are added to the TreeView recursively too.
     *
     * @param playlist                   The {@link ObservablePlaylist} to add to this TreeView
     * @param parentPlaylistTreeViewItem The {@link PlaylistTreeViewItem} that exists in this TreeView where to add the given playlists
     */
    private void addNewPlaylist(ObservablePlaylist playlist, PlaylistTreeViewItem parentPlaylistTreeViewItem) {
        if (parentPlaylistTreeViewItem.getValue().isDirectory()) {
            var playlistTreeItem = new PlaylistTreeViewItem(playlist);
            parentPlaylistTreeViewItem.getChildren().add(playlistTreeItem);

            if (playlist.isDirectory()) {
                var includedPlaylists = playlist.getPlaylists();
                includedPlaylists.forEach(p -> addNewPlaylist(p, playlistTreeItem));
            }
        } else {
            throw new IllegalArgumentException("Inclusion of a playlist is only allowed for a playlist directory");
        }
    }

    /**
     * Finds the {@link PlaylistTreeViewItem} whose value is the given <tt>playlist</tt>, or null
     * if there is not {@link PlaylistTreeViewItem} in the TreeView with it.
     *
     * @param playlist The {@link ObservablePlaylist}
     *
     * @return The found {@link PlaylistTreeViewItem} or null otherwise
     */
    private PlaylistTreeViewItem findPlaylistTreeItemGivenPlaylist(ObservablePlaylist playlist) {
        return findPlaylistTreeItemRecursively(getRoot(), playlist);
    }

    private PlaylistTreeViewItem findPlaylistTreeItemRecursively(TreeItem<ObservablePlaylist> playlistTreeItem, ObservablePlaylist playlist) {
        for (TreeItem<ObservablePlaylist> treeItem : playlistTreeItem.getChildren()) {
            if (treeItem.getValue().equals(playlist))
                return (PlaylistTreeViewItem) treeItem;
            else if (! treeItem.getChildren().isEmpty())
                return findPlaylistTreeItemRecursively(treeItem, playlist);
        }
        return null;
    }

    public void movePlaylist(ObservablePlaylist playlistToMove, ObservablePlaylist destinationPlaylist) {
        var destinationPlaylistTreeItem = findPlaylistTreeItemGivenPlaylist(destinationPlaylist);
        if (destinationPlaylistTreeItem == null) {
            logger.error("Destination playlist tree item not found for: {}", destinationPlaylist);
            throw new RuntimeException("Destination playlist tree item not found for:" + destinationPlaylist);   // TODO improve
        } else if (! destinationPlaylistTreeItem.getValue().isDirectory()) {
            logger.error("Destination playlist is not a directory: {}", destinationPlaylistTreeItem.getValue());
            throw new RuntimeException("Destination playlist is not a directory: " + destinationPlaylistTreeItem.getValue());   // TODO improve
        } else {
            var oldPlaylistFolder = playlistRepository.findParentPlaylist(playlistToMove);
            oldPlaylistFolder.ifPresent(it -> {

                // The playlist folder where it was before should exist by forAudioPlaylistRepositoryExceptionce because it was included in some other one
                var oldPlayListFolderTreeItem = findPlaylistTreeItemGivenPlaylist(it);
                assert(oldPlayListFolderTreeItem != null);

                oldPlayListFolderTreeItem.removePlaylistChild(playlistToMove);
                addNewPlaylist(playlistToMove, destinationPlaylistTreeItem);

                playlistRepository.movePlaylist(playlistToMove.getName(), destinationPlaylist.getName());
                selectPlaylist(destinationPlaylist);
            });
        }
    }

    /**
     * Removes a given {@link ObservablePlaylist} from this TreeView. Included playlists will also
     * be removed from the child {@link PlaylistTreeViewItem}s if any.
     *
     * @param playlist The {@link ObservablePlaylist} to remove from this {@link TreeView}
     */
    public void deletePlaylist(ObservablePlaylist playlist) {
        PlaylistTreeViewItem playlistTreeViewItem = findPlaylistTreeItemGivenPlaylist(playlist);
        if (playlistTreeViewItem != null) {
            PlaylistTreeViewItem playlistTreeViewItemParent = (PlaylistTreeViewItem) playlistTreeViewItem.getParent();
            playlistTreeViewItemParent.removePlaylistChild(playlist);
            playlistRepository.remove(playlist);
        }
   }

    public List<ObservablePlaylist> selectedPlaylists() {
        return getSelectionModel().getSelectedItems().stream()
                .map(TreeItem::getValue)
                .collect(ImmutableList.toImmutableList());
    }

    public void selectPlaylist(ObservablePlaylist playlist) {
        getSelectionModel().select(findPlaylistTreeItemGivenPlaylist(playlist));
    }

    public void selectFirstPlaylist() {
        getSelectionModel().clearAndSelect(1);
    }

    public boolean containsPlaylistName(String name) {
        return playlistRepository.findByName(name).isPresent();
    }

    private static class PlaylistTreeViewItem extends TreeItem<ObservablePlaylist> implements Comparable<ObservablePlaylist> {

        public PlaylistTreeViewItem(ObservablePlaylist value) {
            super(value);
        }

        /**
         * Removes the {@link TreeItem} from the children if the value equals the given {@link ObservablePlaylist}.
         *
         * @param playlist The {@link ObservablePlaylist} to match
         * @return <tt>true</tt> if a {@link TreeItem} was removed, <tt>false</tt> otherwise
         */
        public boolean removePlaylistChild(ObservablePlaylist playlist) {
            return getChildren().removeIf(child -> child.getValue().equals(playlist));
        }

        @Override
        public boolean isLeaf() {
            return ! getValue().isDirectory();
        }

        @Override
        public int compareTo(@NonNull ObservablePlaylist observablePlaylist) {
            return getValue().compareTo(observablePlaylist);
        }

        @Override
        public int hashCode() {
            return getValue().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PlaylistTreeViewItem) {
                return getValue().equals(((PlaylistTreeViewItem) obj).getValue());
            } else
                return false;
        }
    }

    /**
     * Custom {@link TreeCell} that define the style of his {@link ObservablePlaylist}
     * managed by pseudo classes.
     *
     * @author Octavio Calleya
     */
    private class PlaylistTreeViewCell extends TreeCell<ObservablePlaylist> {

        private static final String dragOverStyle = "-fx-effect: dropshadow(one-pass-box, rgb(99, 255, 109), 1, 1.0, 0, 0);";
        private static final String dragOverRootPlaylistStyle = "-fx-border-color: rgb(99, 255, 109); -fx-border-width: 1px;";

        private final PseudoClass playlist = PseudoClass.getPseudoClass("playlist");
        private final PseudoClass playlistSelected = PseudoClass.getPseudoClass("playlist-selected");
        private final PseudoClass folder = PseudoClass.getPseudoClass("folder");
        private final PseudoClass folderSelected = PseudoClass.getPseudoClass("folder-selected");

        private boolean dragOnRoot;

        public PlaylistTreeViewCell(TreeView<ObservablePlaylist> treeView) {
            super();
            if (getItem() != null) {
                setId(getItem().getUniqueId());
            }

            ChangeListener<Boolean> isSelectedListener = (wasSelectedValue, isSelectedVale, observable) -> {
                var isFolder = ! getTreeItem().isLeaf();
                updatePseudoClassesStates(isFolder, isSelectedVale);
            };

            itemProperty().addListener((obs, oldPlaylist, newPlaylist) -> {
                if (oldPlaylist != null) {
                    textProperty().unbind();
                    setText("");
                    selectedProperty().removeListener(isSelectedListener);
                }

                if (newPlaylist != null) {
                    textProperty().bind(newPlaylist.getNameProperty());
                    selectedProperty().addListener(isSelectedListener);

                    var isFolder = getTreeItem().isLeaf();
                    updatePseudoClassesStates(isFolder, selectedProperty().get());
                } else
                    disablePseudoClassesStates();
            });

            setOnDragOver(event -> {
                var isFolder = ! getTreeItem().isLeaf();
                if (getItem() != null && ! isFolder) {
                    event.acceptTransferModes(TransferMode.ANY);
                    event.consume();
                }
            });
            setOnMouseClicked(event -> {
                var thisPlaylist = getItem();
                var isEmpty = thisPlaylist != null && thisPlaylist.getAudioItemsProperty().isEmpty();

                if (event.getButton().equals(MouseButton.SECONDARY)) {
                    // This makes available the selected playlist when opening the Context Menu, therefore enabling creating a new playlist
                    // inside the selected one, if it's a folder, or at the same level.
                    updateSelected(true);
                } else if (! isEmpty && event.getClickCount() == 2) {
                    applicationEventPublisher.publishEvent(new PlayPlaylistRandomlyEvent(thisPlaylist, event.getSource()));
                }
            });
            setOnDragOver(this::onDragOver);
            setOnDragDropped(this::onDragDropped);
            setOnDragExited(this::onDragExited);
            setOnDragDetected(this::onDragDetected);
        }

        private void updatePseudoClassesStates(boolean isFolder, boolean isSelected) {
            pseudoClassStateChanged(folder, isFolder && ! isSelected);
            pseudoClassStateChanged(folderSelected, isFolder && isSelected);
            pseudoClassStateChanged(playlist, ! isFolder && ! isSelected);
            pseudoClassStateChanged(playlistSelected, ! isFolder && isSelected);
        }

        private void disablePseudoClassesStates() {
            pseudoClassStateChanged(folder, false);
            pseudoClassStateChanged(folderSelected, false);
            pseudoClassStateChanged(playlist, false);
            pseudoClassStateChanged(playlistSelected, false);
        }

        private void onDragOver(DragEvent event) {
            if (getItem() != null) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                setStyle(dragOverStyle);
                setOpacity(0.10);
            } else if (event.getDragboard().hasContent(PLAYLIST_DATA_FORMAT)) {
                getTreeView().setStyle(dragOverRootPlaylistStyle);
                dragOnRoot = true;
            }
            event.consume();
        }

        private void onDragExited(DragEvent event) {
            dragOnRoot = false;
            setStyle("");
            getTreeView().setStyle("");
            setOpacity(1.0);
            event.consume();
        }

        @SuppressWarnings ("unchecked")
        private void onDragDropped(DragEvent event) {
            var dragBoard = event.getDragboard();
            if (dragBoard.hasContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT) && isValidAudioItemToDragAndDrop()) {
                var selectedAudioItemIds = (List<Integer>) dragBoard.getContent(AudioItemTableViewBase.TRACKS_DATA_FORMAT);
//                getItem().addAudioItems(selectedAudioItemIds); // TODO fix
            } else if (dragBoard.hasContent(PLAYLIST_DATA_FORMAT)) {
                var playlist = (ObservablePlaylist) dragBoard.getContent(PLAYLIST_DATA_FORMAT);
                var isFolder = getItem() != null && ! getTreeItem().isLeaf();

                if (isFolder && ! playlist.equals(getItem()))
                    movePlaylist(playlist, getItem());
                else if (dragOnRoot)
                    movePlaylistToFirstLevelHierarchy(playlist);
            }
            event.consume();
        }

        /**
         * Moves a {@link ObservablePlaylist}  to the top level hierarchy of this {@link TreeView}
         *
         * @param playlist The {@link ObservablePlaylist} to move
         */
        private void movePlaylistToFirstLevelHierarchy(ObservablePlaylist playlist) {
            movePlaylist(playlist, getRoot().getValue());
        }

        private void onDragDetected(MouseEvent event) {
            if (getItem() != null) {
                var dragBoard = startDragAndDrop(TransferMode.MOVE);
                dragBoard.setDragView(snapshot(null, null));
                var clipboardContent = new ClipboardContent();
                clipboardContent.put(PLAYLIST_DATA_FORMAT, getItem());
                dragBoard.setContent(clipboardContent);
            }
            event.consume();
        }

        private boolean isValidAudioItemToDragAndDrop() {
            var validDrag = getItem() != null && getTreeItem().getValue().isDirectory();
            if (validDrag) {
                var selectedPlaylist = selectedPlaylistProperty.get();
                validDrag = ! (selectedPlaylist.isPresent() && selectedPlaylist.get().equals(getItem()));
            }
            return validDrag;
        }
    }

    /**
     * Context menu to appear on the {@link PlaylistTreeView} when right-clicking over any listed {@link PlaylistTreeViewItem}.
     * Contains {@link MenuItem}s that perform operations related to {@link ObservablePlaylist}s of the application.
     */
    private class PlaylistTreeViewContextMenu extends ContextMenu {

        public PlaylistTreeViewContextMenu() {
            super();
            var addPlaylistMenuItem = new MenuItem("Add new playlist");
            addPlaylistMenuItem.setOnAction(e -> applicationEventPublisher.publishEvent(new CreatePlaylistEvent(e.getSource())));

            var addPlaylistFolderMenuItem = new MenuItem("Add new playlist folder");
            addPlaylistFolderMenuItem.setOnAction(e -> applicationEventPublisher.publishEvent(new CreatePlaylistDirectoryEvent(e.getSource())));

            var deletePlaylistMenuItem = new MenuItem("Delete playlist");
            deletePlaylistMenuItem.setOnAction(e -> applicationEventPublisher.publishEvent(new DeleteSelectedPlaylistEvent(e.getSource())));

            var exportPlaylistsMenuItem = new MenuItem("Export playlist(s) as m3u file(s)");
            exportPlaylistsMenuItem.setOnAction(e -> applicationEventPublisher.publishEvent(new ExportSelectedPlaylistsEvent(e.getSource())));
            getItems().addAll(addPlaylistMenuItem, addPlaylistFolderMenuItem, deletePlaylistMenuItem, exportPlaylistsMenuItem);
        }
    }
}
