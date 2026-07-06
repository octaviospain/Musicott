package net.transgressoft.musicott.view;

import javafx.beans.property.*;
import net.transgressoft.commons.fx.music.FXMusicLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.music.m3u.M3uImportService;
import net.transgressoft.musicott.events.ErrorEvent;
import net.transgressoft.musicott.events.ExportSelectedPlaylistsEvent;
import net.transgressoft.musicott.events.ImportPlaylistsFromM3uEvent;
import net.transgressoft.musicott.events.SelectCurrentPlayingAudioItemEvent;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import net.transgressoft.musicott.events.StatusProgressUpdateEvent;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;
import net.transgressoft.musicott.view.custom.alerts.AlertFactory;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ALBUMS;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ALL_AUDIO_ITEMS;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ARTISTS;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.GENRES;
import static org.fxmisc.easybind.EasyBind.map;
import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * Controller for the application navigation sidebar, managing navigation modes (All Tracks,
 * Artists, Albums, Genres) and the playlist tree view. Keeps nav-mode selection and playlist-tree
 * selection mutually exclusive: selecting a playlist clears the nav list, and switching to a nav
 * mode clears the playlist tree. Handles M3U playlist export and import via Spring application events.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/NavigationController.fxml")
@Controller
public class NavigationController {

    public enum NavigationMode {
        ALL_AUDIO_ITEMS("All tracks"),
        ARTISTS("Artists"),
        ALBUMS("Albums"),
        GENRES("Genres"),
        PLAYLIST("Playlists");

        final String name;

        NavigationMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String GREEN_STATUS_COLOUR = "-fx-text-fill: rgb(99, 255, 109);";
    private static final String GRAY_STATUS_COLOUR = "-fx-text-fill: rgb(170, 170, 170);";

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    final PlaylistTreeView playlistTreeView;
    final ObjectProperty<NavigationMode> navigationModeProperty;

    @FXML
    private VBox navigationPaneVBox;
    @FXML
    private VBox navigationVBox;
    @FXML
    private VBox playlistsVBox;
    @FXML
    private Button newPlaylistButton;
    @FXML
    private ProgressBar taskProgressBar;
    @FXML
    private Label statusLabel;

    final KeyCombination.Modifier operativeSystemKeyModifier;
    final ApplicationEventPublisher applicationEventPublisher;
    final FXMusicLibrary musicLibrary;
    final AlertFactory alertFactory;
    final Supplier<DirectoryChooser> directoryChooserSupplier;
    final Supplier<FileChooser> fileChooserSupplier;
    final M3uImportService<ObservableAudioItem, ObservablePlaylist> m3uImportService;

    private MenuItem newPlaylistMI;
    private MenuItem newFolderPlaylistMI;
    private Optional<ObservablePlaylist> currentPlayingPlaylist;

    @Autowired
    public NavigationController(PlaylistTreeView playlistTreeView,
                                KeyCombination.Modifier operativeSystemKeyModifier,
                                ApplicationEventPublisher applicationEventPublisher,
                                FXMusicLibrary musicLibrary,
                                AlertFactory alertFactory,
                                Supplier<DirectoryChooser> directoryChooserSupplier,
                                Supplier<FileChooser> fileChooserSupplier,
                                M3uImportService<ObservableAudioItem, ObservablePlaylist> m3uImportService) {
        this.playlistTreeView = playlistTreeView;
        this.operativeSystemKeyModifier = operativeSystemKeyModifier;
        this.applicationEventPublisher = applicationEventPublisher;
        this.musicLibrary = musicLibrary;
        this.alertFactory = alertFactory;
        this.directoryChooserSupplier = directoryChooserSupplier;
        this.fileChooserSupplier = fileChooserSupplier;
        this.m3uImportService = m3uImportService;
        this.navigationModeProperty = new SimpleObjectProperty<>(this, "navigation mode", NavigationMode.ARTISTS);
    }

    @FXML
    public void initialize() {
        currentPlayingPlaylist = Optional.empty();
        NavigationMenuListView navigationMenuListView = new NavigationMenuListView();

        ContextMenu newPlaylistButtonContextMenu = new ContextMenu();
        newPlaylistMI = new MenuItem("New Playlist");
        newFolderPlaylistMI = new MenuItem("New Playlist Folder");
        newPlaylistButtonContextMenu.getItems().addAll(newPlaylistMI, newFolderPlaylistMI);
        newPlaylistButton.setContextMenu(newPlaylistButtonContextMenu);

        newPlaylistButton.setContextMenu(newPlaylistButtonContextMenu);
        newPlaylistButton.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            double newPlaylistButtonX = e.getScreenX() + 10.0;
            double newPlaylistButtonY = e.getScreenY() + 10.0;
            newPlaylistButtonContextMenu.show(newPlaylistButton, newPlaylistButtonX, newPlaylistButtonY);
        });

        navigationVBox.getChildren().add(1, navigationMenuListView);
        taskProgressBar.visibleProperty().bind(map(taskProgressBar.progressProperty().isEqualTo(0).not(), Function.identity()));
        taskProgressBar.setProgress(0);

        VBox.setVgrow(navigationVBox, Priority.ALWAYS);

        playlistsVBox.getChildren().add(1, playlistTreeView);
        VBox.setVgrow(playlistTreeView, Priority.ALWAYS);

        // When a playlist is selected, switch navigation mode and clear the nav-list selection
        // so re-clicking a nav mode always re-fires (no stale same-value skip).
        subscribe(selectedPlaylistProperty(),
                  playlist -> playlist.ifPresent(p -> {
                      navigationMenuListView.getSelectionModel().clearSelection();
                      navigationModeProperty.set(NavigationMode.PLAYLIST);
                  })
        );

        // When switching to a non-playlist nav mode, clear the playlist tree.
        subscribe(navigationModeProperty, mode -> {
            if (mode == ALL_AUDIO_ITEMS || mode == ARTISTS || mode == ALBUMS || mode == GENRES) {
                playlistTreeView.getSelectionModel().clearSelection();
            }
        });
    }

    public void setOnNewPlaylistAction(EventHandler<ActionEvent> handler) {
        newPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, operativeSystemKeyModifier));
        newPlaylistMI.setOnAction(handler);
    }

    public void setOnNewFolderPlaylistAction(EventHandler<ActionEvent> handler) {
        newFolderPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, operativeSystemKeyModifier, KeyCombination.SHIFT_DOWN));
        newFolderPlaylistMI.setOnAction(handler);
    }

    public void addNewPlaylist(ObservablePlaylist newPlaylist) {
        playlistTreeView.addNewPlaylist(newPlaylist);
        playlistTreeView.selectPlaylist(newPlaylist);
    }

    /**
     * Deletes the playlist selected in the TreeView component
     * @param selectedPlaylist
     */
    public void deletePlaylist(ObservablePlaylist selectedPlaylist) {
        playlistTreeView.deletePlaylist(selectedPlaylist);
        navigationModeProperty.set(ALL_AUDIO_ITEMS);
    }

    @EventListener
    public void selectCurrentAudioItemEventListener(SelectCurrentPlayingAudioItemEvent selectCurrentPlayingAudioItemEvent) {
        ObservableAudioItem currentAudioItem = selectCurrentPlayingAudioItemEvent.audioItem;
        var audioItemWasPlayedWhenOnPlaylist = currentPlayingPlaylist.isPresent();

        if (audioItemWasPlayedWhenOnPlaylist) {
            var playlist = currentPlayingPlaylist.get();
            audioItemWasPlayedWhenOnPlaylist = playlist.getAudioItems().contains(currentAudioItem);
            if (audioItemWasPlayedWhenOnPlaylist)
                playlistTreeView.selectPlaylist(playlist);
            else {
                // If an Audio Item currently played is not in the playlist that was set as the current one, something is wrong here
                // somehow the playlist is not the correct one, or it is but the Audio Item does not look to be inside it
                navigationModeProperty.set(ALL_AUDIO_ITEMS);
                logger.warn("Attempt to select current playlist of an Audio Item played from a playlist unsuccessful");
            }
        }
    }

    /**
     * Handles an {@link ExportSelectedPlaylistsEvent} by opening a {@link DirectoryChooser} on the
     * FX thread (required — JavaFX dialogs must run on the Application Thread), then exporting
     * each selected playlist to an M3U file in a {@link CompletableFuture} off the FX thread to
     * keep the UI responsive.
     *
     * <p>File collisions are treated as per-playlist failures — the underlying
     * {@code exportToM3uFile} throws {@link java.io.IOException} if the destination already exists,
     * and this method surfaces that as a named failure rather than overwriting, preventing accidental
     * data loss. All failures are aggregated into a single {@link ErrorEvent} rather than shown
     * individually.
     */
    @EventListener(classes = ExportSelectedPlaylistsEvent.class)
    public void exportSelectedPlaylists() {
        Platform.runLater(() -> {
            var selected = playlistTreeView.selectedPlaylists();
            if (selected.isEmpty()) {
                applicationEventPublisher.publishEvent(
                        new StatusMessageUpdateEvent("No playlists selected to export", this));
                return;
            }
            DirectoryChooser chooser = directoryChooserSupplier.get();
            chooser.setTitle("Export playlist(s) to folder");
            File folder = chooser.showDialog(playlistTreeView.getScene().getWindow());
            if (folder == null) {
                return;
            }
            List<String> failures = Collections.synchronizedList(new ArrayList<>());
            var futures = selected.stream()
                    .map(pl -> exportPlaylistAsync(pl, folder, failures))
                    .toList();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .whenComplete((ignored, ex) -> Platform.runLater(() -> publishExportOutcome(selected.size(), failures)));
        });
    }

    /**
     * Exports a single playlist to an {@code .m3u} file under {@code folder} off the FX thread,
     * recording a {@code "<name>: <reason>"} entry in {@code failures} if the write fails rather
     * than propagating the error, so one failure does not abort the sibling exports.
     */
    private CompletableFuture<Void> exportPlaylistAsync(ObservablePlaylist playlist, File folder, List<String> failures) {
        return CompletableFuture.runAsync(() -> {
            try {
                playlist.exportToM3uFile(folder.toPath().resolve(toSafeFileName(playlist.getName()) + ".m3u"));
            } catch (Exception ex) {
                throw new CompletionException(ex);
            }
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            failures.add(playlist.getName() + ": " + cause.getMessage());
            return null;
        });
    }

    private void publishExportOutcome(int exportedCount, List<String> failures) {
        if (failures.isEmpty()) {
            applicationEventPublisher.publishEvent(
                    new StatusMessageUpdateEvent("Exported " + exportedCount + " playlist(s)", this));
        } else {
            applicationEventPublisher.publishEvent(
                    new ErrorEvent("Export failed", String.join("\n", failures), this));
        }
    }

    /**
     * Replaces path separators and filesystem-reserved characters in a playlist name with
     * underscores so it can be resolved into the export folder without escaping it or producing
     * an invalid path. A playlist name is user-controlled and validated only for non-emptiness and
     * uniqueness, so it may legitimately contain characters that are illegal in a file name.
     */
    private static String toSafeFileName(String playlistName) {
        return playlistName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * Handles an {@link ImportPlaylistsFromM3uEvent} by opening a {@link FileChooser} on the FX thread
     * (required — JavaFX dialogs must run on the Application Thread), importing the chosen M3U file
     * asynchronously via {@link M3uImportService#importAsync}, and nesting the resulting playlist
     * under {@code ROOT_PLAYLIST} so it becomes visible in the {@link PlaylistTreeView}.
     *
     * <p>The playlist is nested under {@code ROOT_PLAYLIST} via
     * {@code playlistHierarchy().addPlaylistsToDirectory()} because imported playlists are not
     * automatically inserted into the tree — they must be explicitly placed in a directory. Feedback
     * is limited to the status-bar imported-count; skipped entries are not counted and no
     * partial-import modal is shown.
     *
     * <p>Hard failures ({@link net.transgressoft.commons.music.m3u.M3uImportException},
     * {@link net.transgressoft.commons.music.m3u.M3uParseException},
     * {@link net.transgressoft.commons.music.m3u.M3uCycleException}) are surfaced as an
     * {@link ErrorEvent} modal; no playlist is nested on failure.
     */
    @EventListener(classes = ImportPlaylistsFromM3uEvent.class)
    public void importPlaylistFromM3u() {
        Platform.runLater(() -> {
            FileChooser chooser = fileChooserSupplier.get();
            chooser.setTitle("Import playlist from M3U file");
            if (chooser.getExtensionFilters() != null) {
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("M3U Playlists", "*.m3u", "*.m3u8"));
            }
            File file = chooser.showOpenDialog(playlistTreeView.getScene().getWindow());
            if (file == null) {
                return;
            }
            m3uImportService.importAsync(file.toPath())
                    .whenComplete((playlist, ex) -> Platform.runLater(() -> {
                        if (ex != null) {
                            logger.error("M3U import failed", ex);
                            applicationEventPublisher.publishEvent(
                                    new ErrorEvent("Import failed", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), this));
                        } else {
                            musicLibrary.playlistHierarchy().addPlaylistsToDirectory(Set.of(playlist), PlaylistTreeView.ROOT_PLAYLIST_NAME);
                            // Count tracks recursively: a folder playlist's direct audioItems exclude tracks
                            // held by its nested playlists, so the total imported must span the whole subtree.
                            applicationEventPublisher.publishEvent(
                                    new StatusMessageUpdateEvent(
                                            "Imported playlist '" + playlist.getName() + "' (" + playlist.getAudioItemsRecursive().size() + " tracks)", this));
                        }
                    }));
        });
    }

    @EventListener
    public void setStatusMessage(StatusMessageUpdateEvent statusMessageUpdateEvent) {
        Platform.runLater(() -> {
            if (Double.isNaN(taskProgressBar.getProgress()))
                statusLabel.setStyle(GREEN_STATUS_COLOUR);
            else
                statusLabel.setStyle(GRAY_STATUS_COLOUR);
            statusLabel.setText(String.valueOf(statusMessageUpdateEvent.statusMessage));
        });
    }

    @EventListener
    public void setStatusProgress(StatusProgressUpdateEvent statusProgressUpdateEvent) {
        Platform.runLater(() -> taskProgressBar.setProgress(statusProgressUpdateEvent.statusProgress));
    }

    public boolean containsPlaylistName(String name) {
        return playlistTreeView.containsPlaylistName(name);
    }

    public void selectFirstPlaylist() {
        playlistTreeView.selectFirstPlaylist();
    }

    public void selectPlaylist(ObservablePlaylist playlist) {
        playlistTreeView.selectPlaylist(playlist);
        selectedPlaylistProperty().set(Optional.of(playlist));
    }

    public ReadOnlyObjectProperty<NavigationMode> navigationModeProperty() {
        return navigationModeProperty;
    }

    public ReadOnlySetProperty<ObservablePlaylist> playlistsProperty() {
        return playlistTreeView.playlistsProperty();
    }

    public List<ObservablePlaylist> selectedPlaylists() {
        return playlistTreeView.selectedPlaylists();
    }

    public ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty() {
        return playlistTreeView.selectedPlaylistProperty();
    }

    /**
     * ListView for the application navigation showing modes.
     */
    private class NavigationMenuListView extends ListView<NavigationMode> {

        // Row height for the navigation modes. Large enough for the nav icons with comfortable
        // padding; the list is pinned to exactly one row per mode so no empty rows appear below the
        // last mode (which would otherwise read as a gap before the playlist tree).
        private static final double NAV_CELL_HEIGHT = 30.0;

        public NavigationMenuListView() {
            super();
            setId("navigationModeListView");
            setPrefWidth(USE_COMPUTED_SIZE);

            NavigationMode[] navigationModes = { ALL_AUDIO_ITEMS, ARTISTS, ALBUMS, GENRES};
            double listHeight = navigationModes.length * NAV_CELL_HEIGHT;
            setFixedCellSize(NAV_CELL_HEIGHT);
            setMinHeight(listHeight);
            setPrefHeight(listHeight);
            setMaxHeight(listHeight);
            setItems(FXCollections.observableArrayList(navigationModes));
            setCellFactory(listView -> new NavigationListCell());
            // Clear the nav-list selection first so re-clicking the same mode always
            // re-fires the selectedItemProperty change (no dead same-value skip).
            getSelectionModel().selectedItemProperty().addListener((obs, oldMode, newMode) -> {
                if (newMode != null) {
                    navigationModeProperty.set(newMode);
                }
            });
            getSelectionModel().select(ARTISTS);
        }
    }

    /**
     * Custom {@link ListCell} that define the style of each row in
     * the {@link NavigationMenuListView}, managed by pseudo classes.
     */
    private class NavigationListCell extends ListCell<NavigationMode> {

        private final PseudoClass audioItems = PseudoClass.getPseudoClass("tracks");
        private final PseudoClass audioItemsSelected = PseudoClass.getPseudoClass("tracks-selected");
        private final PseudoClass artists = PseudoClass.getPseudoClass("artists");
        private final PseudoClass artistsSelected = PseudoClass.getPseudoClass("artists-selected");
        private final PseudoClass albums = PseudoClass.getPseudoClass("albums");
        private final PseudoClass albumsSelected = PseudoClass.getPseudoClass("albums-selected");
        private final PseudoClass genres = PseudoClass.getPseudoClass("genres");
        private final PseudoClass genresSelected = PseudoClass.getPseudoClass("genres-selected");

        public NavigationListCell() {
            super();

            ChangeListener<Boolean> isSelectedListener = (obs, oldModeSelected, newModeSelected) -> {
                var mode = itemProperty().getValue();
                updatePseudoClassStates(mode, newModeSelected);
            };

            itemProperty().addListener((obs, oldMode, newMode) -> {
                if (oldMode != null) {
                    setText("");
                    selectedProperty().removeListener(isSelectedListener);
                }

                if (newMode != null) {
                    setText(newMode.toString());
                    selectedProperty().addListener(isSelectedListener);
                    updatePseudoClassStates(newMode, selectedProperty().get());
                } else
                    disablePseudoClassesStates();
            });
        }

        private void updatePseudoClassStates(NavigationMode mode, boolean isSelected) {
            pseudoClassStateChanged(audioItems, mode.equals(ALL_AUDIO_ITEMS) && ! isSelected);
            pseudoClassStateChanged(audioItemsSelected, mode.equals(ALL_AUDIO_ITEMS) && isSelected);
            pseudoClassStateChanged(artists, mode.equals(ARTISTS) && ! isSelected);
            pseudoClassStateChanged(artistsSelected, mode.equals(ARTISTS) && isSelected);
            pseudoClassStateChanged(albums, mode.equals(ALBUMS) && ! isSelected);
            pseudoClassStateChanged(albumsSelected, mode.equals(ALBUMS) && isSelected);
            pseudoClassStateChanged(genres, mode.equals(GENRES) && ! isSelected);
            pseudoClassStateChanged(genresSelected, mode.equals(GENRES) && isSelected);
        }

        private void disablePseudoClassesStates() {
            pseudoClassStateChanged(audioItems, false);
            pseudoClassStateChanged(audioItemsSelected, false);
            pseudoClassStateChanged(artists, false);
            pseudoClassStateChanged(artistsSelected, false);
            pseudoClassStateChanged(albums, false);
            pseudoClassStateChanged(albumsSelected, false);
            pseudoClassStateChanged(genres, false);
            pseudoClassStateChanged(genresSelected, false);
        }
    }
}
