package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.binding.*;
import javafx.beans.property.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.*;
import net.rgielen.fxweaver.core.*;
import net.transgressoft.lirp.event.*;
import net.transgressoft.commons.fx.music.audio.*;
import net.transgressoft.commons.music.audio.*;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.view.custom.table.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.fxmisc.easybind.EasyBind.map;

/**
 * @author Octavio Calleya
 */
@FxmlView("/fxml/ArtistViewController.fxml")
@Controller
public class ArtistViewController {

    private static final short DEFAULT_RANDOM_PLAYLIST_SIZE = 23;

    private final ObservableAudioLibrary audioRepository;
    private final ApplicationContext applicationContext;

    @FXML
    private SplitPane artistsViewSplitPane;
    @FXML
    private ListView<Artist> artistsListView;
    @FXML
    private ListView<ArtistAlbumListRow> albumsListView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label totalAlbumsLabel;
    @FXML
    private Label totalTracksLabel;
    @FXML
    private Button artistRandomButton;

    private ObservableMap<AlbumSet<ObservableAudioItem>, ArtistAlbumListRow> albumListRowMap;
    private ObjectProperty<Optional<Artist>> selectedArtistProperty;
    private FilteredList<Artist> filteredArtists;

    @Autowired
    public ArtistViewController(ObservableAudioLibrary audioLibrary, ApplicationContext applicationContext) {
        this.audioRepository = audioLibrary;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        totalAlbumsLabel.setText("0 albums");
        totalTracksLabel.setText("0 tracks");
        artistRandomButton.visibleProperty().bind(map(nameLabel.textProperty().isEmpty().not(), Function.identity()));
        artistRandomButton.setOnAction(_ -> playRandomArtistTracks());
        selectedArtistProperty = new SimpleObjectProperty<>(this, "selected artist", Optional.empty());
        nameLabel.textProperty().bind(Bindings.createStringBinding(() ->
                        selectedArtistProperty.get().isPresent() ? selectedArtistProperty.get().get().getName() : "",
                selectedArtistProperty));

        albumListRowMap = FXCollections.observableMap(new TreeMap<>());
        albumListRowMap.addListener(this::artistAlbumListRowMapChangeListener);

        // TODO replace listener from artistsListView to selectedArtistProperty ?
        artistsListView.getSelectionModel().selectedItemProperty().addListener(this::selectedArtistListener);
        artistsListView.setCellFactory(_ -> new ArtistCell());
        artistsListView.setOnMouseClicked(this::doubleClickOnArtistHandler);
        configureArtistsListViewBacking();
        audioRepository.getArtistCatalogPublisher().subscribe(this::artistCatalogChangeHandler);
        artistsListView.getSelectionModel().selectFirst();
    }

    /**
     * Configure the artists ListView to remain backed by the repository's artistsProperty.
     * <p>
     * JavaFX filtering/sorting APIs operate on ObservableList, but the source of truth
     * for artists in the repository is a ReadOnlySetProperty to guarantee uniqueness and
     * set semantics.
     */
    private void configureArtistsListViewBacking() {
        var artistsBackingList = FXCollections.observableArrayList(audioRepository.artistsProperty());

        audioRepository.artistsProperty().addListener((SetChangeListener<Artist>) change -> {
            if (change.wasAdded()) {
                Platform.runLater(() -> artistsBackingList.add(change.getElementAdded()));
            }
            if (change.wasRemoved()) {
                Platform.runLater(() -> artistsBackingList.remove(change.getElementRemoved()));
            }
        });

        filteredArtists = new FilteredList<>(artistsBackingList);
        filteredArtists.addListener(this::artistsListener);
        artistsListView.setItems(new SortedList<>(filteredArtists, Comparator.comparing(Artist::getName)));
    }

    private void artistsListener(ListChangeListener.Change<? extends Artist> change) {
        if (selectedArtistProperty.get().isEmpty() && !change.getList().isEmpty()) {
            artistsListView.getSelectionModel().select(0);
            artistsListView.getFocusModel().focus(0);
        }
    }

    @SuppressWarnings("java:S4968") // Type parameter AlbumSet is final in kotlin, but the compiler does not allow to remove the extends clause
    private void artistAlbumListRowMapChangeListener(MapChangeListener.Change<? extends AlbumSet<ObservableAudioItem>, ? extends ArtistAlbumListRow> change) {
        Platform.runLater(() -> {
            if (change.wasAdded()) {
                albumsListView.getItems().add(change.getValueAdded());
            } else if (change.wasRemoved()) {
                albumsListView.getItems().remove(change.getValueRemoved());
            }
            totalTracksLabel.setText(getTotalArtistTracksString());
            totalAlbumsLabel.setText(getAlbumString());
        });
    }

    private void selectedArtistListener(ObservableValue<? extends Artist> obs, Artist oldArtist, Artist newArtist) {
        if (newArtist != null) {
            // if the selected artistCatalog is not already selected
            if (! nameLabel.getText().equals(newArtist.getName())) {
                selectedArtistProperty.set(Optional.of(newArtist));
                audioRepository.getArtistCatalog(newArtist).ifPresent(artistCatalog -> {
                    albumListRowMap.clear();
                    artistCatalog.getAlbums().forEach(albumSet -> {
                        var audioItemsTableView = applicationContext.getBean(SimpleAudioItemTableView.class);
                        var artistListRow = applicationContext.getBean(ArtistAlbumListRow.class, newArtist, albumSet, audioItemsTableView);
                        albumListRowMap.put(albumSet, artistListRow);
                    });
                });
            }
        } else {
            if (artistsListView.getItems().isEmpty()) {
                selectedArtistProperty.set(Optional.empty());
                Platform.runLater(albumListRowMap::clear);
            }
        }
    }

    private void doubleClickOnArtistHandler(MouseEvent event) {
        selectedArtistProperty.get().ifPresent(_ -> {
            if (event.getClickCount() == 2) {
                playRandomArtistTracks();
            }
        });
    }

    private void playRandomArtistTracks() {
        var selectedArtist = selectedArtistProperty.get().get();
        var randomAudioItemsFromArtist = audioRepository.getRandomAudioItemsFromArtist(selectedArtist, DEFAULT_RANDOM_PLAYLIST_SIZE);
        applicationContext.publishEvent(new PlayItemEvent(randomAudioItemsFromArtist, this));
    }

    private String getTotalArtistTracksString() {
        int totalArtistTracks = albumListRowMap.values().stream()
            .mapToInt(trackSet -> trackSet.containedAudioItemsProperty().size())
            .sum();
        String appendix = totalArtistTracks == 1 ? " track" : " tracks";
        return totalArtistTracks + appendix;
    }

    private String getAlbumString() {
        int numberOfTrackSets = albumListRowMap.size();
        var appendix = numberOfTrackSets == 1 ? " album" : " albums";
        return numberOfTrackSets + appendix;
    }

    /**
     * The change handler on the artist catalog is used to update the artistCatalog view when
     * audio items are created, updated, or removed from the artist catalog.
     *
     * @param event The event that contains the artist catalog that changed.
     */
    private void artistCatalogChangeHandler(CrudEvent<Artist, ObservableArtistCatalog> event) {
        selectedArtistProperty.get().ifPresent(selectedArtist ->
            event.getEntities().forEach((artist, artistCatalog) -> {
                if (artist.equals(selectedArtist)) {
                    albumListRowMap.clear();
                    artistCatalog.getAlbums().forEach(albumSet -> {
                        var audioItemsTableView = applicationContext.getBean(SimpleAudioItemTableView.class);
                        var artistListRow = applicationContext.getBean(ArtistAlbumListRow.class, selectedArtist, albumSet, audioItemsTableView);
                        albumListRowMap.put(albumSet, artistListRow);
                    });
                }
            })
        );
    }

    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        return albumListRowMap.values().stream()
            .flatMap(entry -> entry.selectedAudioItemsProperty().stream())
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    public void findAudioItemInArtistViewAndSelect(ObservableAudioItem audioItem) {
        Set<Artist> artistsInvolved = audioItem.getArtistsInvolved();
        Optional<Artist> selectedArtist = selectedArtistProperty.getValue();
        if (selectedArtist.isPresent() && !artistsInvolved.contains(selectedArtist.get())) {
            var newArtist = artistsInvolved.stream().findFirst().get();
            selectedArtistProperty.setValue(Optional.of(newArtist));

            artistsListView.getSelectionModel().select(newArtist);  // This should work
            artistsListView.scrollTo(newArtist);
        } else {
            albumListRowMap.values().forEach(trackSet -> {
                if (trackSet.getAlbumSet().getAlbumName().equals(audioItem.getAlbum().getName())) {
                    trackSet.selectAudioItem(audioItem);
                    albumsListView.scrollTo(trackSet);
                } else {
                    trackSet.deselectAllAudioItems();
                }
            });
        }
    }

    public void selectAllTracks() {
        albumListRowMap.values().forEach(ArtistAlbumListRow::selectAllAudioItems);
    }

    public void deselectAllTracks() {
        albumListRowMap.values().forEach(ArtistAlbumListRow::deselectAllAudioItems);
    }

    @EventListener
    public void searchTextTypedEvent(SearchTextTypedEvent event) {
        filteredArtists.setPredicate(filterArtistsByQuery(event.searchText));
    }

    private Predicate<Artist> filterArtistsByQuery(String query) {
        return artist ->
                albumListRowMap.keySet().stream()
                        .flatMap(Collection::stream)
                        .anyMatch(audioItem -> artistMatchesQuery(audioItem, artist, query.toLowerCase()));
    }

    private boolean artistMatchesQuery(ObservableAudioItem audioItem, Artist artist, String query) {
        if (query == null || query.isEmpty())
            return true;

        var matchesName = artist.getName().toLowerCase().contains(query.toLowerCase());
        var matchesArtist = audioItem.getArtist().equals(artist);
        var matchesArtistName = audioItem.getArtist().getName().toLowerCase().contains(query);
        var matchesAlbumArtist = audioItem.getAlbum().getAlbumArtist().getName().toLowerCase().contains(query);
        var matchesComments = audioItem.getComments() != null && audioItem.getComments().toLowerCase().contains(query);
        var matchesAlbumName = audioItem.getAlbum().getName().toLowerCase().contains(query);

        var containsMatchedTrack =
                matchesArtist &&
                (matchesName || matchesArtistName || matchesAlbumArtist || matchesComments || matchesAlbumName);

        return matchesName || containsMatchedTrack;
    }

    public ReadOnlyObjectProperty<Optional<Artist>> selectedArtistProperty() {
        return selectedArtistProperty;
    }

    private static class ArtistCell extends ListCell<Artist> {

        @Override
        protected void updateItem(Artist item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null)
                Platform.runLater(() -> setGraphic(null));
            else
                Platform.runLater(() -> setGraphic(new Label(item.getName())));
        }
    }
}
