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
import net.transgressoft.musicott.search.SearchCoordinator;
import net.transgressoft.musicott.search.Searchable;
import net.transgressoft.musicott.view.NavigationController.NavigationMode;
import net.transgressoft.musicott.view.custom.table.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.*;
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
public class ArtistViewController implements Searchable<String> {

    private final ObservableAudioLibrary audioRepository;
    private final ApplicationContext applicationContext;
    private final SearchCoordinator searchCoordinator;

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

    /**
     * Immutable snapshot of the artists backing list taken on the FX thread by {@link #prepareSnapshot()}
     * before the background scan begins. Read-only from {@link #computeMatchIds}.
     */
    private List<ObservableArtistCatalog> artistsSnapshot = List.of();

    /**
     * Immutable snapshot of the current album row list taken on the FX thread by {@link #prepareSnapshot()}
     * before the background scan begins. Read-only from {@link #computeMatchIds}.
     */
    private List<ArtistAlbumListRow> albumRowsSnapshot = List.of();

    /**
     * Precomputed result of the off-thread album-row scan: maps each row to the set of track IDs
     * that match the search query. Written by {@link #computeMatchIds} and read by
     * {@link #applyMatchIds} on the FX thread after the coroutine context switch.
     */
    private Map<ArtistAlbumListRow, Set<Integer>> matchingTrackIdsByRow = Map.of();

    /**
     * Immutable snapshot of each artist's tracks taken on the FX thread by {@link #prepareSnapshot()}
     * via {@link #audioItemsForArtist}. Consumed by {@link #computeMatchIds} for track-content
     * matching so the scan never iterates a live observable list off-thread.
     */
    private Map<Artist, List<ObservableAudioItem>> artistTracksSnapshot = Map.of();

    /**
     * Immutable snapshot of each album row's contained tracks taken on the FX thread by
     * {@link #prepareSnapshot()}. Consumed by {@link #computeMatchIds} so the per-row scan never
     * iterates a live {@code containedAudioItemsProperty} off-thread.
     */
    private Map<ArtistAlbumListRow, List<ObservableAudioItem>> rowTracksSnapshot = Map.of();

    @Autowired
    public ArtistViewController(ObservableAudioLibrary audioLibrary, ApplicationContext applicationContext,
                                SearchCoordinator searchCoordinator) {
        this.audioRepository = audioLibrary;
        this.applicationContext = applicationContext;
        this.searchCoordinator = searchCoordinator;
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

        searchCoordinator.register(NavigationMode.ARTISTS, this);

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
            albumListRowMap.put(albumSet, artistListRow);
            albumRowsBackingList.add(artistListRow);
        });

        // When a search query is active, re-apply per-row filtering for the newly loaded rows.
        // This is a bounded per-artist scan (acceptable on the FX thread) that mirrors the same
        // name-match-shows-all rule used in computeMatchIds: if the artist name matches the query,
        // every track in each row is shown; otherwise only tracks matching the query content are shown.
        if (!currentSearchQuery.isBlank()) {
            String q = currentSearchQuery;
            boolean artistNameMatches = artist.getName() != null
                    && artist.getName().toLowerCase().contains(q);
            if (artistNameMatches) {
                // Artist matched by name: show all tracks in all rows, keep rows visible
                albumRowsBackingList.forEach(row -> row.filterTracks(null));
                filteredAlbumRows.setPredicate(null);
            } else {
                // Artist matched only via track content: filter each row to matching tracks only
                albumRowsBackingList.forEach(row ->
                        row.filterTracks(item -> audioItemMatchesQuery(item, q)));
                filteredAlbumRows.setPredicate(row -> row.hasTracksMatching(item -> audioItemMatchesQuery(item, q)));
            }
        }

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

    /**
     * Captures immutable snapshots of the artists and album-row backing lists, the repository
     * audio-item pool, and each row's contained tracks on the FX thread. All snapshots are consumed
     * by {@link #computeMatchIds} on the background dispatcher, preventing data races with concurrent
     * FX-thread structural mutations of any of these live observable collections.
     */
    @Override
    public void prepareSnapshot() {
        artistsSnapshot = List.copyOf(filteredArtists.getSource());
        albumRowsSnapshot = List.copyOf(albumRowsBackingList);
        // Resolve each artist's tracks on the FX thread via audioItemsForArtist, which handles the
        // null-property fallback to per-artist catalogs; the off-thread scan then reads these copies.
        Map<Artist, List<ObservableAudioItem>> artistTracks = new HashMap<>();
        for (var catalog : artistsSnapshot) {
            var artist = catalog.getArtist();
            artistTracks.computeIfAbsent(artist, a -> audioItemsForArtist(a).toList());
        }
        artistTracksSnapshot = Map.copyOf(artistTracks);
        Map<ArtistAlbumListRow, List<ObservableAudioItem>> rowTracks = new HashMap<>();
        for (var row : albumRowsSnapshot) {
            rowTracks.put(row, List.copyOf(row.containedAudioItemsProperty()));
        }
        rowTracksSnapshot = Map.copyOf(rowTracks);
    }

    /**
     * Scans the artist and album-row snapshots (captured on the FX thread by {@link #prepareSnapshot})
     * for matching entries. Both the artist-level scan (name and track metadata) and the per-row
     * track scan run entirely off the FX thread. Precomputed results are stored in
     * {@link #matchingTrackIdsByRow} for cheap application in {@link #applyMatchIds}.
     *
     * <p>For each row the precomputed track-ID set depends on how the artist matched: if the row's
     * artist name matches the query directly (name-only match), every track in the row is included so
     * the user sees the full album when they select that artist; if the artist matched only via track
     * content, only the query-matching track IDs are included.
     *
     * @param query the lower-cased search text
     * @return set of artist names whose catalog has at least one match
     */
    @Override
    public Set<String> computeMatchIds(String query) {
        // First, build the set of artist names that match the query by name alone. This is used below
        // to decide whether a row should show all its tracks (name match) or only the matching subset.
        Set<String> artistNameMatches = artistsSnapshot.stream()
                .filter(catalog -> catalog.getArtistName() != null
                        && catalog.getArtistName().toLowerCase().contains(query))
                .map(ObservableArtistCatalog::getArtistName)
                .collect(toSet());

        // Precompute which track IDs match per album row so applyMatchIds can apply
        // cheap membership predicates without any substring scanning on the FX thread.
        // If the row belongs to a name-matched artist, include ALL track IDs so selecting that
        // artist reveals its full track list rather than an empty one.
        Map<ArtistAlbumListRow, Set<Integer>> rowMatchIds = new HashMap<>();
        for (var row : albumRowsSnapshot) {
            boolean artistNameMatched = row.getArtist() != null
                    && artistNameMatches.contains(row.getArtist().getName());
            List<ObservableAudioItem> rowTracks = rowTracksSnapshot.getOrDefault(row, List.of());
            Set<Integer> ids;
            if (artistNameMatched) {
                ids = rowTracks.stream()
                        .map(ObservableAudioItem::getId)
                        .collect(toSet());
            } else {
                ids = rowTracks.stream()
                        .filter(item -> audioItemMatchesQuery(item, query))
                        .map(ObservableAudioItem::getId)
                        .collect(toSet());
            }
            rowMatchIds.put(row, ids);
        }
        matchingTrackIdsByRow = Map.copyOf(rowMatchIds);

        return artistsSnapshot.stream()
                .filter(filterArtistsByQuery(query)::test)
                .map(ObservableArtistCatalog::getArtistName)
                .collect(toSet());
    }

    /**
     * Applies the pre-computed results to the artist view on the JavaFX Application Thread.
     * All three collections are updated using the precomputed ID sets from {@link #computeMatchIds}:
     * the artist predicate uses a name-membership check, the per-row track tables apply precomputed
     * track-ID predicates, and the album-row predicate shows only rows with at least one visible track.
     * No substring scanning is performed in this method.
     *
     * <p>A blank {@code query} signals a reset: all predicates are cleared and every artist and row
     * becomes visible again. For a non-blank query, rows whose artist matched by name show all their
     * tracks (the precomputed set for that row contains every track ID); rows that matched only by
     * track content show only the matching subset; rows with no match are hidden.
     *
     * @param query the lower-cased search text; blank signals a reset (show all)
     * @param ids   the set of matching artist names produced by {@link #computeMatchIds}
     */
    @Override
    public void applyMatchIds(String query, Set<String> ids) {
        currentSearchQuery = query == null ? "" : query;
        boolean reset = currentSearchQuery.isBlank();
        filteredArtists.setPredicate(reset ? null : catalog -> ids.contains(catalog.getArtistName()));
        if (reset) {
            // Reset: clear all row-level filters
            albumRowsBackingList.forEach(row -> row.filterTracks(null));
            filteredAlbumRows.setPredicate(null);
        } else {
            // Apply precomputed per-row track-ID sets — no substring scanning.
            // matchingTrackIdsByRow contains ALL track IDs for name-matched artist rows, and only the
            // query-matching subset for track-content-only matches. Rows with no matching tracks are hidden.
            albumRowsBackingList.forEach(row -> {
                Set<Integer> trackIds = matchingTrackIdsByRow.getOrDefault(row, Set.of());
                row.filterTracks(item -> trackIds.contains(item.getId()));
            });
            filteredAlbumRows.setPredicate(row -> {
                Set<Integer> trackIds = matchingTrackIdsByRow.getOrDefault(row, Set.of());
                return !trackIds.isEmpty();
            });
        }
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
            // Scan the FX-thread per-artist track snapshot rather than the live repository list:
            // this predicate runs off-thread inside computeMatchIds.
            return artistTracksSnapshot.getOrDefault(artistCatalog.getArtist(), List.of()).stream()
                    .anyMatch(audioItem -> audioItemMatchesQuery(audioItem, q));
        };
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
        var artistsInvolved = audioItem.getArtistsInvolved();
        if (artistsInvolved == null || !artistsInvolved.contains(artist)) {
            return false;
        }
        // A track surfaces under every artist it involves — including its album artist — so editing
        // the artist or album artist moves the track between artist rows. The one exception is a
        // compilation's album artist (e.g. "Various Artists"): individual compilation tracks must not
        // collapse under that grouping, only under their own performing artist.
        var album = audioItem.getAlbum();
        var albumArtist = album == null ? null : album.getAlbumArtist();
        boolean isCompilation = album != null && album.isCompilation();
        return !isCompilation || !artist.equals(albumArtist);
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
