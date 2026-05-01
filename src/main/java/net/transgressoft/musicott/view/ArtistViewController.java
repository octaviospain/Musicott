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
    private ObservableList<ArtistAlbumListRow> albumRowsBackingList;
    private FilteredList<ArtistAlbumListRow> filteredAlbumRows;
    private ObjectProperty<Optional<Artist>> selectedArtistProperty;
    private FilteredList<Artist> filteredArtists;
    private String currentSearchQuery = "";

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
        // Backing list mirrors albumListRowMap insertions/removals; albumsListView shows a filtered
        // view so the active search query can hide non-matching album rows entirely.
        albumRowsBackingList = FXCollections.observableArrayList();
        filteredAlbumRows = new FilteredList<>(albumRowsBackingList);
        albumsListView.setItems(filteredAlbumRows);

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
        var artistsBackingList = FXCollections.observableArrayList(audioRepository.getArtistsProperty());

        audioRepository.getArtistsProperty().addListener((SetChangeListener<Artist>) change -> {
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
        if (!change.getList().isEmpty()) {
            // Defer selection until the SortedList (registered after this listener) has processed
            // the same change. Only auto-select when nothing is selected yet — preserve the user's
            // current selection across catalog updates that mutate the artist list.
            Platform.runLater(() -> {
                if (artistsListView.getSelectionModel().getSelectedItem() == null) {
                    artistsListView.getSelectionModel().select(0);
                    artistsListView.getFocusModel().focus(0);
                }
            });
        }
    }

    @SuppressWarnings("java:S4968") // Type parameter AlbumSet is final in kotlin, but the compiler does not allow to remove the extends clause
    private void artistAlbumListRowMapChangeListener(MapChangeListener.Change<? extends AlbumSet<ObservableAudioItem>, ? extends ArtistAlbumListRow> change) {
        Platform.runLater(() -> {
            if (change.wasAdded()) {
                ArtistAlbumListRow added = change.getValueAdded();
                // Newly-loaded rows must inherit the active query so reselecting an artist while a
                // search is active doesn't surface unfiltered album content for a moment.
                added.filterTracksByQuery(currentSearchQuery);
                albumRowsBackingList.add(added);
            } else if (change.wasRemoved()) {
                albumRowsBackingList.remove(change.getValueRemoved());
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
        currentSearchQuery = event.searchText == null ? "" : event.searchText;
        filteredArtists.setPredicate(filterArtistsByQuery(currentSearchQuery));
        // Propagate to currently-loaded album rows: hide rows with no matching tracks and let each
        // visible row narrow its embedded SimpleAudioItemTableView to the matching tracks.
        albumRowsBackingList.forEach(row -> row.filterTracksByQuery(currentSearchQuery));
        filteredAlbumRows.setPredicate(albumRowMatchesQuery(currentSearchQuery));
    }

    private Predicate<Artist> filterArtistsByQuery(String query) {
        if (query == null || query.isEmpty()) {
            return artist -> true;
        }
        String q = query.toLowerCase();
        // Walk the artist's own catalog instead of albumListRowMap (which only holds the currently
        // selected artist's albums). Without this, queries by title/album/comments/label only ever
        // match against the selected artist, so other artists with track-level matches stay hidden.
        return artist -> {
            if (artist.getName().toLowerCase().contains(q)) {
                return true;
            }
            return audioRepository.getArtistCatalog(artist).stream()
                    .flatMap(catalog -> catalog.getAlbums().stream())
                    .flatMap(Collection::stream)
                    .anyMatch(audioItem -> artistMatchesQuery(audioItem, artist, q));
        };
    }

    private Predicate<ArtistAlbumListRow> albumRowMatchesQuery(String query) {
        if (query == null || query.isEmpty()) {
            return row -> true;
        }
        return row -> row.hasTracksMatching(query);
    }

    private boolean artistMatchesQuery(ObservableAudioItem audioItem, Artist artist, String query) {
        if (query == null || query.isEmpty())
            return true;

        // Guard each metadata access — partial catalogs can have null artist/album/album-artist
        // or null name fields; one malformed track shouldn't NPE the entire artist filter.
        var matchesName = artist.getName() != null && artist.getName().toLowerCase().contains(query);
        var itemArtist = audioItem.getArtist();
        var matchesArtist = itemArtist != null && itemArtist.equals(artist);
        var matchesTitle = audioItem.getTitle() != null && audioItem.getTitle().toLowerCase().contains(query);
        var matchesArtistName = itemArtist != null && itemArtist.getName() != null
                && itemArtist.getName().toLowerCase().contains(query);
        var album = audioItem.getAlbum();
        var matchesAlbumArtist = album != null && album.getAlbumArtist() != null
                && album.getAlbumArtist().getName() != null
                && album.getAlbumArtist().getName().toLowerCase().contains(query);
        var matchesComments = audioItem.getComments() != null && audioItem.getComments().toLowerCase().contains(query);
        var matchesAlbumName = album != null && album.getName() != null
                && album.getName().toLowerCase().contains(query);
        var matchesLabel = album != null && album.getLabel() != null
                && album.getLabel().getName() != null
                && album.getLabel().getName().toLowerCase().contains(query);

        var containsMatchedTrack =
                matchesArtist &&
                (matchesName || matchesTitle || matchesArtistName || matchesAlbumArtist
                        || matchesComments || matchesAlbumName || matchesLabel);

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
