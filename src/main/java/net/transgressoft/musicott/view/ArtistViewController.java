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
        albumRowsBackingList = FXCollections.observableArrayList();
        filteredAlbumRows = new FilteredList<>(albumRowsBackingList);
        albumsListView.setItems(filteredAlbumRows);

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

    // JavaFX ListChangeListener's SAM requires the `? extends Artist` parametrization for the
    // method reference to bind; narrowing to the final `Artist` type breaks compilation.
    @SuppressWarnings("java:S4968")
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

    // JavaFX ChangeListener's SAM requires the `? extends Artist` parametrization for the
    // method reference to bind; narrowing to the final `Artist` type breaks compilation.
    @SuppressWarnings("java:S4968")
    private void selectedArtistListener(ObservableValue<? extends Artist> obs, Artist oldArtist, Artist newArtist) {
        if (newArtist != null) {
            // if the selected artistCatalog is not already selected
            if (! nameLabel.getText().equals(newArtist.getName())) {
                selectedArtistProperty.set(Optional.of(newArtist));
                refreshAlbumRowsForArtist(newArtist);
            }
        } else {
            if (artistsListView.getItems().isEmpty()) {
                selectedArtistProperty.set(Optional.empty());
                Platform.runLater(albumListRowMap::clear);
            }
        }
    }

    private void refreshAlbumRowsForArtist(Artist artist) {
        var albumSetsWithDisc = albumSetsForArtist(artist);
        Runnable refresh = () -> replaceAlbumRowsForArtist(artist, albumSetsWithDisc);
        if (Platform.isFxApplicationThread()) {
            refresh.run();
        } else {
            Platform.runLater(refresh);
        }
    }

    private void replaceAlbumRowsForArtist(Artist artist, Map<AlbumSet<ObservableAudioItem>, Integer> albumSetsWithDisc) {
        albumListRowMap.clear();
        albumRowsBackingList.clear();
        albumSetsWithDisc.forEach((albumSet, discNum) -> {
            var audioItemsTableView = applicationContext.getBean(SimpleAudioItemTableView.class);
            var artistListRow = applicationContext.getBean(ArtistAlbumListRow.class, artist, albumSet, audioItemsTableView, discNum);
            artistListRow.filterTracksByQuery(currentSearchQuery);
            albumListRowMap.put(albumSet, artistListRow);
            albumRowsBackingList.add(artistListRow);
        });
        totalTracksLabel.setText(getTotalArtistTracksString());
        totalAlbumsLabel.setText(getAlbumString());
    }

    /**
     * Builds an ordered map of album sets to disc numbers for the given artist. Multi-disc albums
     * produce one entry per distinct disc number; single-disc albums produce one entry with disc
     * number {@code 0} (no disc label rendered).
     */
    Map<AlbumSet<ObservableAudioItem>, Integer> albumSetsForArtist(Artist artist) {
        record AlbumDiscKey(String albumName, int disc) implements Comparable<AlbumDiscKey> {
            @Override
            public int compareTo(AlbumDiscKey other) {
                int cmp = albumName.compareTo(other.albumName);
                return cmp != 0 ? cmp : Integer.compare(disc, other.disc);
            }
        }

        var tracks = audioItemsForArtist(artist)
                .filter(audioItem -> audioItem.getAlbum() != null && audioItem.getAlbum().getName() != null)
                .collect(Collectors.toList());
        if (tracks.isEmpty()) {
            return Map.of();
        }

        // Detect which album names have more than one distinct normalized disc number
        var multiDiscAlbums = tracks.stream()
                .collect(Collectors.groupingBy(t -> t.getAlbum().getName()))
                .entrySet().stream()
                .filter(e -> e.getValue().stream()
                        .map(t -> normalizeDisc(t.getDiscNumber()))
                        .distinct().count() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // Group by composite (albumName, disc): multi-disc albums split per disc, single-disc use disc=0
        var grouped = tracks.stream().collect(Collectors.groupingBy(
                t -> new AlbumDiscKey(
                        t.getAlbum().getName(),
                        multiDiscAlbums.contains(t.getAlbum().getName()) ? normalizeDisc(t.getDiscNumber()) : 0),
                TreeMap::new,
                Collectors.toList()));

        // Build result map preserving TreeMap order: AlbumSet → disc number (0 = no label)
        var result = new LinkedHashMap<AlbumSet<ObservableAudioItem>, Integer>();
        grouped.forEach((key, items) ->
                result.put(new AlbumView<>(key.albumName(), items), key.disc()));
        return result;
    }

    private static int normalizeDisc(Number discNumber) {
        if (discNumber == null) return 1;
        int v = discNumber.intValue();
        return v <= 0 ? 1 : v;
    }

    // getAudioItemsProperty() is nominally non-null, but partial or mock-backed repositories can
    // return null; the fallback to the per-artist catalog keeps filtering working in those cases.
    @SuppressWarnings("java:S2589")
    private Stream<ObservableAudioItem> audioItemsForArtist(Artist artist) {
        var audioItems = audioRepository.getAudioItemsProperty();
        if (audioItems != null) {
            return audioItems.stream().filter(audioItem -> audioItemBelongsToArtist(audioItem, artist));
        }

        var catalog = audioRepository.getArtistCatalog(artist);
        if (catalog.isEmpty()) {
            return Stream.empty();
        }
        return catalog.stream()
                .flatMap(artistCatalog -> artistCatalog.getAlbums().stream())
                .flatMap(Collection::stream);
    }

    private void doubleClickOnArtistHandler(MouseEvent event) {
        selectedArtistProperty.get().ifPresent(_ -> {
            if (event.getClickCount() == 2) {
                playRandomArtistTracks();
            }
        });
    }

    private void playRandomArtistTracks() {
        // All call sites (doubleClickOnArtistHandler via ifPresent, and artistRandomButton whose
        // visibility is bound to nameLabel being non-empty) guarantee the artist is present.
        @SuppressWarnings("java:S3655")
        var selectedArtist = selectedArtistProperty.get().get();
        var randomAudioItemsFromArtist = audioRepository.getRandomAudioItemsFromArtist(selectedArtist, DEFAULT_RANDOM_PLAYLIST_SIZE);
        applicationContext.publishEvent(new PlayItemEvent(randomAudioItemsFromArtist, this));
    }

    private String getTotalArtistTracksString() {
        int totalArtistTracks = albumRowsBackingList.stream()
            .mapToInt(trackSet -> trackSet.containedAudioItemsProperty().size())
            .sum();
        String appendix = totalArtistTracks == 1 ? " track" : " tracks";
        return totalArtistTracks + appendix;
    }

    private String getAlbumString() {
        long numberOfAlbums = albumRowsBackingList.stream()
                .map(row -> row.getAlbumSet().getAlbumName())
                .distinct()
                .count();
        var appendix = numberOfAlbums == 1 ? " album" : " albums";
        return numberOfAlbums + appendix;
    }

    /**
     * The change handler on the artist catalog is used to update the artistCatalog view when
     * audio items are created, updated, or removed from the artist catalog.
     *
     * @param event The event that contains the artist catalog that changed.
     */
    private void artistCatalogChangeHandler(CrudEvent<Artist, ObservableArtistCatalog> event) {
        selectedArtistProperty.get().ifPresent(selectedArtist -> {
            var selectedCatalogChanged = event.getEntities().containsKey(selectedArtist);
            var involvedTrackChanged = event.getEntities().values().stream()
                    .flatMap(artistCatalog -> artistCatalog.getAlbums().stream())
                    .flatMap(Collection::stream)
                    .anyMatch(audioItem -> audioItemBelongsToArtist(audioItem, selectedArtist));
            if (selectedCatalogChanged || involvedTrackChanged) {
                refreshAlbumRowsForArtist(selectedArtist);
            }
        });
    }

    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        return albumRowsBackingList.stream()
            .flatMap(entry -> entry.selectedAudioItemsProperty().stream())
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    public void findAudioItemInArtistViewAndSelect(ObservableAudioItem audioItem) {
        Set<Artist> artistsInvolved = audioItem.getArtistsInvolved();
        Optional<Artist> selectedArtist = selectedArtistProperty.getValue();
        if (selectedArtist.isPresent() && !artistsInvolved.contains(selectedArtist.get())) {
            var newArtist = artistsInvolved.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("AudioItem has no artists"));
            selectedArtistProperty.setValue(Optional.of(newArtist));

            artistsListView.getSelectionModel().select(newArtist);  // This should work
            artistsListView.scrollTo(newArtist);
        } else {
            albumRowsBackingList.forEach(trackSet -> {
                boolean sameAlbum = trackSet.getAlbumSet().getAlbumName().equals(audioItem.getAlbum().getName());
                boolean containsItem = trackSet.getAlbumSet().stream().anyMatch(item -> item.equals(audioItem));
                if (sameAlbum && containsItem) {
                    trackSet.selectAudioItem(audioItem);
                    albumsListView.scrollTo(trackSet);
                } else {
                    trackSet.deselectAllAudioItems();
                }
            });
        }
    }

    public void selectAllTracks() {
        albumRowsBackingList.forEach(ArtistAlbumListRow::selectAllAudioItems);
    }

    public void deselectAllTracks() {
        albumRowsBackingList.forEach(ArtistAlbumListRow::deselectAllAudioItems);
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
        return artist -> {
            if (artist.getName() != null && artist.getName().toLowerCase().contains(q)) {
                return true;
            }
            return audioItemsForArtist(artist)
                    .anyMatch(audioItem -> audioItemMatchesQuery(audioItem, q));
        };
    }

    private Predicate<ArtistAlbumListRow> albumRowMatchesQuery(String query) {
        if (query == null || query.isEmpty()) {
            return row -> true;
        }
        return row -> row.hasTracksMatching(query);
    }

    // Defensive null guards — partial catalogs can ship tracks with null artist/album/album-artist.
    // Sonar's flow analysis trusts the music-commons API's nominal non-null types and flags these
    // guards as gratuitous; in practice imported tracks sometimes carry nulls.
    @SuppressWarnings({"java:S2589", "java:S2583"})
    private static boolean audioItemBelongsToArtist(ObservableAudioItem audioItem, Artist artist) {
        var itemArtist = audioItem.getArtist();
        if (itemArtist != null && itemArtist.equals(artist)) {
            return true;
        }
        var album = audioItem.getAlbum();
        var albumArtist = album == null ? null : album.getAlbumArtist();
        return audioItem.getArtistsInvolved() != null
                && audioItem.getArtistsInvolved().contains(artist)
                && !artist.equals(albumArtist);
    }

    // Guard each metadata access — partial catalogs can have null artist/album/album-artist
    // or null name fields; one malformed track shouldn't NPE the entire artist filter.
    // Sonar's flow analysis trusts the music-commons API's nominal non-null types and flags
    // each guard as gratuitous; in practice imported tracks sometimes ship with nulls.
    @SuppressWarnings("java:S2589")
    private static boolean audioItemMatchesQuery(ObservableAudioItem audioItem, String query) {
        var matchesTitle = audioItem.getTitle() != null && audioItem.getTitle().toLowerCase().contains(query);
        if (matchesTitle) {
            return true;
        }

        var itemArtist = audioItem.getArtist();
        var matchesArtistName = itemArtist != null && itemArtist.getName() != null
                && itemArtist.getName().toLowerCase().contains(query);
        if (matchesArtistName) {
            return true;
        }

        var matchesArtistInvolved = audioItem.getArtistsInvolved() != null && audioItem.getArtistsInvolved().stream()
                .map(Artist::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> name.toLowerCase().contains(query));
        if (matchesArtistInvolved) {
            return true;
        }

        var album = audioItem.getAlbum();
        var matchesAlbumArtist = album != null && album.getAlbumArtist() != null
                && album.getAlbumArtist().getName() != null
                && album.getAlbumArtist().getName().toLowerCase().contains(query);
        if (matchesAlbumArtist) {
            return true;
        }

        var matchesComments = audioItem.getComments() != null && audioItem.getComments().toLowerCase().contains(query);
        if (matchesComments) {
            return true;
        }

        var matchesAlbumName = album != null && album.getName() != null
                && album.getName().toLowerCase().contains(query);
        if (matchesAlbumName) {
            return true;
        }

        return album != null && album.getLabel() != null
                && album.getLabel().getName() != null
                && album.getLabel().getName().toLowerCase().contains(query);
    }

    public ReadOnlyObjectProperty<Optional<Artist>> selectedArtistProperty() {
        return selectedArtistProperty;
    }

    private static class ArtistCell extends ListCell<Artist> {

        @Override
        protected void updateItem(Artist item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null)
                setGraphic(null);
            else
                setGraphic(new Label(item.getName()));
        }
    }
}
