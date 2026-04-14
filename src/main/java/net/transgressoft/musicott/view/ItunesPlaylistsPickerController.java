package net.transgressoft.musicott.view;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;
import net.transgressoft.musicott.service.MediaImportService;
import net.transgressoft.musicott.view.custom.ItunesPlaylistListCell;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Controller for the iTunes playlists picker window, allowing the user to select
 * which playlists to import from a parsed iTunes library.
 *
 * @author Octavio Calleya
 */
@FxmlView ("/fxml/ItunesPlaylistsPickerController.fxml")
@Controller
public class ItunesPlaylistsPickerController {

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final MediaImportService mediaImportService;

    @FXML
    private ListView<ItunesPlaylist> sourcePlaylists;
    @FXML
    private ListView<ItunesPlaylist> targetPlaylists;
    @FXML
    private Button addSelectedButton;
    @FXML
    private Button removeSelectedButton;
    @FXML
    private Button addAllButton;
    @FXML
    private Button removeAllButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button importButton;

    @Autowired
    public ItunesPlaylistsPickerController(MediaImportService mediaImportService) {
        this.mediaImportService = mediaImportService;
        logger.debug("ItunesPlaylistsPickerController created {}", this);
    }

    @FXML
    public void initialize() {
        addSelectedButton.setOnAction(e -> moveSelected(sourcePlaylists, targetPlaylists));
        removeSelectedButton.setOnAction(e -> moveSelected(targetPlaylists, sourcePlaylists));
        addAllButton.setOnAction(e -> moveAll(sourcePlaylists, targetPlaylists));
        removeAllButton.setOnAction(e -> moveAll(targetPlaylists, sourcePlaylists));
        sourcePlaylists.setCellFactory(cell -> new ItunesPlaylistListCell(this));
        sourcePlaylists.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        targetPlaylists.setCellFactory(cell -> new ItunesPlaylistListCell(this));
        targetPlaylists.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cancelButton.setOnAction(e -> {
            mediaImportService.cancelImport();
            cancelButton.getScene().getWindow().hide();
        });
        importButton.setOnAction(e -> {
            try {
                mediaImportService.importSelectedPlaylists(new ArrayList<>(targetPlaylists.getItems()));
            } catch (IllegalStateException ex) {
                logger.error("iTunes import failed: {}", ex.getMessage(), ex);
            }
            cancelButton.getScene().getWindow().hide();
        });
        logger.debug("ItunesPlaylistsPickerController initialized {}", this);
    }

    private void moveSelected(ListView<ItunesPlaylist> from, ListView<ItunesPlaylist> to) {
        var selectedItems = from.getSelectionModel().getSelectedItems();
        to.getItems().addAll(selectedItems);
        from.getItems().removeAll(selectedItems);
        FXCollections.sort(to.getItems(), Comparator.comparing(ItunesPlaylist::getName));
    }

    private void moveAll(ListView<ItunesPlaylist> from, ListView<ItunesPlaylist> to) {
        to.getItems().addAll(from.getItems());
        from.getItems().clear();
        FXCollections.sort(to.getItems(), Comparator.comparing(ItunesPlaylist::getName));
    }

    public void setStage(Stage stage) {
        stage.setTitle("Choose iTunes playlists");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(true);
        stage.setMinWidth(670);
        stage.setMinHeight(515);
    }

    public void pickPlaylists(List<ItunesPlaylist> playlists) {
        sourcePlaylists.getItems().clear();
        targetPlaylists.getItems().clear();
        sourcePlaylists.getItems().addAll(playlists);
        FXCollections.sort(sourcePlaylists.getItems(), Comparator.comparing(ItunesPlaylist::getName));
    }

    public void movePlaylist(ItunesPlaylist playlist) {
        if (sourcePlaylists.getItems().contains(playlist)) {
            targetPlaylists.getItems().add(playlist);
            sourcePlaylists.getItems().remove(playlist);
            FXCollections.sort(targetPlaylists.getItems(), Comparator.comparing(ItunesPlaylist::getName));
        }
        else if (targetPlaylists.getItems().contains(playlist)) {
            sourcePlaylists.getItems().add(playlist);
            targetPlaylists.getItems().remove(playlist);
            FXCollections.sort(sourcePlaylists.getItems(), Comparator.comparing(ItunesPlaylist::getName));
        }
    }
}
