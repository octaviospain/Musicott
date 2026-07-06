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

import static java.util.stream.Collectors.*;
import static org.fxmisc.easybind.EasyBind.map;

/**
 * @author Octavio Calleya
 */
@FxmlView("/fxml/ArtistViewController.fxml")
@Controller
public class ArtistViewController {

    private final ObservableAudioLibrary audioRepository;
    private final ApplicationContext applicationContext;

    @FXML
    private SplitPane artistsViewSplitPane;
    @FXML
    private ListView<ObservableArtistCatalog> artistsListView;
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

    private ObservableMap<AlbumTrackGroup, ArtistAlbumListRow> albumListRowMap;
    private ObservableList<ArtistAlbumListRow> albumRowsBackingList;
    private FilteredList<ArtistAlbumListRow> filteredAlbumRows;
    private ObjectProperty<Optional<ObservableArtistCatalog>> selectedArtistProperty;
    private FilteredList<ObservableArtistCatalog> filteredArtists;
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
        artistRandomButton.setOnAction(_ -> applicationContext.publishEvent(new PlayRandomFromContextEvent(this)));
        selectedArtistProperty = new SimpleObjectProperty<>(this, "selected artist", Optional.empty());
        nameLabel.textProperty().bind(Bindings.createStringBinding(() ->
                        selectedArtistProperty.get().isPresent() ? selectedArtistProperty.get().get().getArtistName() : "",
                selectedArtistProperty));

        albumListRowMap = FXCollections.observableMap(new LinkedHashMap<>());
        albumRowsBackingList = FXCollections.observableArrayList();
        filteredAlbumRows = new FilteredList<>(albumRowsBackingList);
        albumsListView.setItems(filteredAlbumRows);

        artistsListView.getSelectionModel().selectedItemProperty().addListener(this::selectedArtistListener);
        artistsListView.setCellFactory(_ -> new ArtistCell());
        artistsListView.setOnMouseClicked(this::doubleClickOnArtistHandler);
        configureArtistsListViewBacking();
        audioRepository.getArtistCatalogPublisher().subscribeAsync(this::artistCatalogChangeHandler);
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
        var artistsBackingList = FXCollections.observableArrayList(audioRepository.getArtistCatalogsProperty());

        audioRepository.getArtistCatalogsProperty().addListener((SetChangeListener<ObservableArtistCatalog>) change -> {
            if (change.wasAdded()) {
                Platform.runLater(() -> artistsBackingList.add(change.getElementAdded()));
            }
            if (change.wasRemoved()) {
                Platform.runLater(() -> artistsBackingList.remove(change.getElementRemoved()));
            }
        });

        filteredArtists = new FilteredList<>(artistsBackingList);
        filteredArtists.addListener(this::artistsListener);
        artistsListView.setItems(new SortedList<>(filteredArtists, Comparator.comparing(ObservableArtistCatalog::getArtistName)));
    }

    // JavaFX ListChangeListener's SAM requires the `? extends Artist` parametrization for the
    // method reference to bind; narrowing to the final `Artist` type breaks compilation.
    @SuppressWarnings("java:S4968")
    private void artistsListener(ListChangeListener.Change<? extends ObservableArtistCatalog> change) {
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
    private void selectedArtistListener(ObservableValue<? extends ObservableArtistCatalog> obs,
                                        ObservableArtistCatalog oldArtistCatalog,
                                        ObservableArtistCatalog newArtistCatalog) {
        if (newArtistCatalog != null) {
            // if the selected artistCatalog is not already selected
            if (! nameLabel.getText().equals(newArtistCatalog.getArtistName())) {
                selectedArtistProperty.set(Optional.of(newArtistCatalog));
                refreshAlbumRowsForArtist(newArtistCatalog.getArtist());
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

    private void replaceAlbumRowsForArtist(Artist artist, Map<AlbumTrackGroup, Integer> albumSetsWithDisc) {
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
    Map<AlbumTrackGroup, Integer> albumSetsForArtist(Artist artist) {
        record AlbumDiscKey(String albumName, int disc) implements Comparable<AlbumDiscKey> {
            @Override
            public int compareTo(AlbumDiscKey other) {
                int cmp = albumName.compareTo(other.albumName);
                return cmp != 0 ? cmp : Integer.compare(disc, other.disc);
            }
        }

        var tracks = audioItemsForArtist(artist)
                .filter(audioItem -> audioItem.getAlbum() != null && audioItem.getAlbum().getName() != null)
                .toList();
        if (tracks.isEmpty()) {
            return Map.of();
        }

        // Detect which album names have more than one distinct normalized disc number
        var multiDiscAlbums = tracks.stream()
                .collect(groupingBy(t -> t.getAlbum().getName()))
                .entrySet().stream()
                .filter(e -> e.getValue().stream()
                        .map(t -> normalizeDisc(t.getDiscNumber()))
                        .distinct().count() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());

        // Group by composite (albumName, disc): multi-disc albums split per disc, single-disc use disc=0
        var grouped = tracks.stream().collect(groupingBy(
                t -> new AlbumDiscKey(
                        t.getAlbum().getName(),
                        multiDiscAlbums.contains(t.getAlbum().getName()) ? normalizeDisc(t.getDiscNumber()) : 0),
                TreeMap::new,
                toList()));

        // Build result map preserving TreeMap order: album group → disc number (0 = no label)
        var result = new LinkedHashMap<AlbumTrackGroup, Integer>();
        grouped.forEach((key, items) ->
                result.put(new AlbumTrackGroup(key.albumName(), items), key.disc()));
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
        return catalog.stream().flatMap(ArtistViewController::catalogAudioItems);
    }

    /**
     * Streams every audio item in an artist catalog. The catalog exposes its albums as a set of
     * {@code AlbumDetails} and resolves each album's tracks by name; tracks are de-duplicated to
     * guard against distinct album identities that happen to share a name.
     */
    private static Stream<ObservableAudioItem> catalogAudioItems(ObservableArtistCatalog catalog) {
        return catalog.getAlbumsProperty().stream()
                .flatMap(album -> catalog.albumAudioItemsProperty(album.getName()).stream())
                .distinct();
    }

    private void doubleClickOnArtistHandler(MouseEvent event) {
        selectedArtistProperty.get().ifPresent(_ -> {
            if (event.getClickCount() == 2) {
                applicationContext.publishEvent(new PlayRandomFromContextEvent(this));
            }
        });
    }

    /**
     * Returns every audio item involving the currently selected artist, or an empty list when no
     * artist is selected. The pool mirrors what the artist view displays — it includes tracks where
     * the artist appears in {@code artistsInvolved} (featured or album collaborations), not only
     * tracks whose primary artist matches. {@link MainController} consumes this to resolve the
     * random-playback pool for the {@code ARTISTS} navigation context.
     */
    public List<ObservableAudioItem> getSelectedArtistAudioItems() {
        return selectedArtistProperty.getValue()
                .map(artistCatalog -> audioItemsForArtist(artistCatalog.getArtist()).toList())
                .orElseGet(List::of);
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
        selectedArtistProperty.get().ifPresent(selectedArtistCatalog -> {
            var selectedCatalogChanged = event.getEntities().containsKey(selectedArtistCatalog.getArtist());
            var involvedTrackChanged = event.getEntities().values().stream()
                    .flatMap(ArtistViewController::catalogAudioItems)
                    .anyMatch(audioItem -> audioItemBelongsToArtist(audioItem, selectedArtistCatalog.getArtist()));
            if (selectedCatalogChanged || involvedTrackChanged) {
                refreshAlbumRowsForArtist(selectedArtistCatalog.getArtist());
            }
        });
    }

    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        return albumRowsBackingList.stream()
            .flatMap(entry -> entry.selectedAudioItemsProperty().stream())
            .collect(toCollection(FXCollections::observableArrayList));
    }

    public void findAudioItemInArtistViewAndSelect(ObservableAudioItem audioItem) {
        Set<Artist> artistsInvolved = audioItem.getArtistsInvolved();
        Optional<ObservableArtistCatalog> selectedArtistCatalog = selectedArtistProperty.getValue();
        if (selectedArtistCatalog.isPresent() && !artistsInvolved.contains(selectedArtistCatalog.get().getArtist())) {
            var newArtist = artistsInvolved.stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("AudioItem has no artists"));
            Optional<ObservableArtistCatalog> artistCatalog = (Optional<ObservableArtistCatalog>) audioRepository.getArtistCatalog(newArtist);
            selectedArtistProperty.setValue(artistCatalog);
            artistCatalog.ifPresent(catalog -> {
                artistsListView.getSelectionModel().select(catalog);
                artistsListView.scrollTo(catalog);
            });
        } else {
            albumRowsBackingList.forEach(trackSet -> {
                var album = audioItem.getAlbum();
                boolean sameAlbum = album != null
                        && trackSet.getAlbumSet().getAlbumName().equals(album.getName());
                boolean containsItem = trackSet.getAlbumSet().tracks().stream().anyMatch(item -> item.equals(audioItem));
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

    private Predicate<ObservableArtistCatalog> filterArtistsByQuery(String query) {
        if (query == null || query.isEmpty()) {
            return artist -> true;
        }
        String q = query.toLowerCase();
        return artistCatalog -> {
            if (artistCatalog.getArtistName() != null && artistCatalog.getArtistName().toLowerCase().contains(q)) {
                return true;
            }
            return audioItemsForArtist(artistCatalog.getArtist())
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

    public ReadOnlyObjectProperty<Optional<ObservableArtistCatalog>> selectedArtistProperty() {
        return selectedArtistProperty;
    }

    private static class ArtistCell extends ListCell<ObservableArtistCatalog> {

        @Override
        protected void updateItem(ObservableArtistCatalog item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null)
                setGraphic(null);
            else
                setGraphic(new Label(item.getArtistName()));
        }
    }
}
