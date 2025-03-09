package net.transgressoft.musicott.view;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.musicott.config.SettingsRepository;
import net.transgressoft.musicott.events.ExportSelectedPlaylistsEvent;
import net.transgressoft.musicott.events.SelectCurrentPlayingAudioItemEvent;
import net.transgressoft.musicott.events.StatusMessageUpdateEvent;
import net.transgressoft.musicott.events.StatusProgressUpdateEvent;
import net.transgressoft.musicott.view.custom.PlaylistTreeView;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.function.Function;

import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ALL_AUDIO_ITEMS;
import static net.transgressoft.musicott.view.NavigationController.NavigationMode.ARTISTS;
import static org.fxmisc.easybind.EasyBind.map;
import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/NavigationController.fxml")
@Controller
public class NavigationController {

    private static final String GREEN_STATUS_COLOUR = "-fx-text-fill: rgb(99, 255, 109);";
    private static final String GRAY_STATUS_COLOUR = "-fx-text-fill: rgb(73, 73, 73);";

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final PlaylistTreeView playlistTreeView;
    private final ObjectProperty<NavigationMode> navigationModeProperty;
    private final ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;

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

    private MenuItem newPlaylistMI;
    private MenuItem newFolderPlaylistMI;
    private NavigationMenuListView navigationMenuListView;
    private Optional<ObservablePlaylist> currentPlayingPlaylist;

    @Autowired
    public NavigationController(PlaylistTreeView playlistTreeView, ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty) {
        this.playlistTreeView = playlistTreeView;
        this.navigationModeProperty = new SimpleObjectProperty<>(this, "navigation mode", NavigationMode.ARTISTS);
        this.selectedPlaylistProperty = selectedPlaylistProperty;
    }

    @FXML
    public void initialize() {
        currentPlayingPlaylist = Optional.empty();
        navigationMenuListView = new NavigationMenuListView();

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

        subscribe(selectedPlaylistProperty,
                  playlist -> playlist.ifPresent(
                          p -> navigationModeProperty.set(NavigationMode.PLAYLIST))
        );

        subscribe(playlistTreeView.getSelectionModel().selectedItemProperty(), newItem -> {
           if (newItem != null) {
               selectedPlaylistProperty.set(Optional.of(newItem.getValue()));
           } else {
               selectedPlaylistProperty.set(Optional.empty());
           }
        });
    }

    public void setOnNewPlaylistAction(EventHandler<ActionEvent> handler) {
        newPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, SettingsRepository.OS_SPECIFIC_KEY_MODIFIER));
        newPlaylistMI.setOnAction(handler);
    }

    public void setOnNewFolderPlaylistAction(EventHandler<ActionEvent> handler) {
        newFolderPlaylistMI.setAccelerator(new KeyCodeCombination(KeyCode.N, SettingsRepository.OS_SPECIFIC_KEY_MODIFIER, KeyCombination.SHIFT_DOWN));
        newFolderPlaylistMI.setOnAction(handler);
    }

    public void addNewPlaylist(ObservablePlaylist newPlaylist) {
        playlistTreeView.addNewPlaylist(newPlaylist);
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

    @EventListener (classes = ExportSelectedPlaylistsEvent.class)
    public void exportSelectedPlaylists() {
        var selectedPlaylists = playlistTreeView.selectedPlaylists();
        // TODO open dialog box to select destination
        // selectedPlaylists.forEach(playlist -> playlist.exportToM3uFile(path));
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
        selectedPlaylistProperty.set(Optional.of(playlist));
    }

    public ReadOnlyObjectProperty<NavigationMode> navigationModeProperty() {
        return navigationModeProperty;
    }

    /**
     * Class enum that represents a view modality of the application
     *
     * @author Octavio Calleya
     */
    public enum NavigationMode {

        /**
         * All tracks in Musicott are shown on the table
         */
        ALL_AUDIO_ITEMS("All tracks"),

        /**
         * A section with all the artists is shown with an adapted inner view
         */
        ARTISTS("Artists"),

        /**
         * The {@link ObservableAudioItem}s of a selected {@link ObservablePlaylist} are shown on the table
         */
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

    /**
     * ListView for the application navigation showing modes.
     */
    private class NavigationMenuListView extends ListView<NavigationMode> {

        public NavigationMenuListView() {
            super();
            setId("navigationModeListView");
            setPrefHeight(USE_COMPUTED_SIZE);
            setPrefWidth(USE_COMPUTED_SIZE);

            NavigationMode[] navigationModes = { ALL_AUDIO_ITEMS, ARTISTS};
            setItems(FXCollections.observableArrayList(navigationModes));
            setCellFactory(listView -> new NavigationListCell());
            EasyBind.subscribe(getSelectionModel().selectedItemProperty(), navigationModeProperty::set);
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
        }

        private void disablePseudoClassesStates() {
            pseudoClassStateChanged(audioItems, false);
            pseudoClassStateChanged(audioItemsSelected, false);
            pseudoClassStateChanged(artists, false);
            pseudoClassStateChanged(artistsSelected, false);
        }
    }
}
