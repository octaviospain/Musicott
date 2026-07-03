package net.transgressoft.musicott.view;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.fx.music.audio.ObservableGenreIndex;
import net.transgressoft.musicott.events.PlayItemEvent;
import net.transgressoft.musicott.search.SearchCoordinator;
import net.transgressoft.musicott.search.Searchable;
import net.transgressoft.musicott.view.NavigationController.NavigationMode;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.OverlayTracksDrawer;
import net.transgressoft.musicott.view.custom.table.AlbumTrackGroup;
import net.transgressoft.musicott.view.custom.table.AudioItemQueryMatcher;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Controller for the Genres navigation view. Renders the audio library's genre index as a grid of
 * cells backed directly by {@link ObservableAudioLibrary#getGenreIndexesProperty()}, the reactive
 * genre projection provided by the music domain layer. The view does not re-group audio items by
 * genre: the projection is the single source of truth and the grid reflects its set membership
 * reactively as tracks are imported, edited, or removed.
 *
 * <p>Unlike the album catalog, a genre index exposes no representative cover image, so each cell
 * draws from the genre's pool of distinct track covers and swaps to a different random one as the
 * pointer moves over the cell. Covers come from each track's {@code coverImageProperty}, which the
 * music domain layer now resolves off the JavaFX thread on first observation, so the UI thread never
 * blocks on disk I/O.
 *
 * <p>Single-clicking a cover cell slides in a right overlay drawer showing the selected genre's
 * tracks grouped by album. Only the genre-tagged tracks are shown under each album — partial albums
 * are rendered as-is without re-aggregation. Album-less genre tracks appear in a trailing
 * "Unknown Album" section. Clicking the dimmed grid area or pressing Esc closes the drawer.
 *
 * @author Octavio Calleya
 */
@FxmlView("/fxml/GenreViewController.fxml")
@Controller
public class GenreViewController implements Searchable<String> {

    private static final double CELL_WIDTH = 170.0;
    private static final double CELL_HEIGHT = 210.0;
    private static final double COVER_SIZE = 150.0;

    /** Label used for genre tracks that carry no album metadata. */
    static final String UNKNOWN_ALBUM = "Unknown Album";

    // Upper bound on tracks probed when building a genre's cover pool. Caps the one-time off-thread
    // loads for large genres while gathering enough distinct covers to cycle through on hover.
    private static final int POOL_PROBE_CAP = 40;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final ObservableAudioLibrary audioRepository;
    private final ApplicationContext applicationContext;
    private final SearchCoordinator searchCoordinator;

    @FXML
    private StackPane genresRootPane;

    private GridView<ObservableGenreIndex> genreGridView;
    private FilteredList<ObservableGenreIndex> filteredGenres;

    /** Ordered mirror of the repository's genre-index projection; the source of {@link #filteredGenres}. */
    private ObservableList<ObservableGenreIndex> genresBacking;

    /**
     * Immutable snapshot of {@link #genresBacking} taken on the FX thread by {@link #prepareSnapshot()}
     * before the background scan begins. Read-only from {@link #computeMatchIds}.
     */
    private List<ObservableGenreIndex> genresSnapshot = List.of();

    /** Lower-cased active search query, or empty when no filter is applied. */
    private String currentSearchQuery = "";

    /** The shared overlay drawer showing the selected genre's tracks grouped by album. */
    private OverlayTracksDrawer drawer;

    /** The genre whose drawer is currently open; {@code null} when no drawer is shown. */
    ObservableGenreIndex selectedGenre;

    /**
     * Listener attached to {@link ObservableGenreIndex#getEmptyProperty()} while a drawer is open.
     * Auto-closes the drawer when the genre bucket drops to empty. Detached on close.
     */
    private ChangeListener<Boolean> emptyListener;

    // Per-genre pool of distinct cover images, keyed by index identity, read/written only on the FX
    // thread. Resolved once off-thread on a genre's first display; cells then cycle through it on
    // hover with no further disk access.
    private final Map<ObservableGenreIndex, CoverPool> coverPools = new IdentityHashMap<>();

    /**
     * The distinct cover images of a genre bucket, accumulated on the JavaFX thread as the genre's
     * track covers resolve. Covers are deduplicated by image so cycling always shows genuinely
     * different art; the default placeholder is never a member. {@code started} guards one-time
     * resolution, and {@code lastShown} remembers the cover currently displayed so the last
     * randomized cover persists when the pointer leaves the cell (and across cell recycling).
     */
    private static final class CoverPool {
        private final List<Image> covers = new ArrayList<>();
        private boolean started;
        private Image lastShown;
    }

    @Autowired
    public GenreViewController(ObservableAudioLibrary audioLibrary, ApplicationContext applicationContext,
                               SearchCoordinator searchCoordinator) {
        this.audioRepository = audioLibrary;
        this.applicationContext = applicationContext;
        this.searchCoordinator = searchCoordinator;
    }

    @FXML
    public void initialize() {
        genreGridView = new GridView<>();
        genreGridView.setCellWidth(CELL_WIDTH);
        genreGridView.setCellHeight(CELL_HEIGHT);
        genreGridView.setHorizontalCellSpacing(12);
        genreGridView.setVerticalCellSpacing(12);
        genreGridView.setCellFactory(grid -> new GenreGridCell());

        configureGridBacking();

        // GridView reports a content-sized preferred height (all rows); left unconstrained it balloons
        // ancestor preferred heights and pushes the bottom player layout off-screen. Bind its size to
        // the containing pane so the grid always fits the available area and scrolls internally.
        genreGridView.setMinSize(0, 0);
        genreGridView.prefWidthProperty().bind(genresRootPane.widthProperty());
        genreGridView.prefHeightProperty().bind(genresRootPane.heightProperty());
        genreGridView.maxWidthProperty().bind(genresRootPane.widthProperty());
        genreGridView.maxHeightProperty().bind(genresRootPane.heightProperty());
        genresRootPane.getChildren().add(genreGridView);

        // The shared drawer owns the overlay lifecycle (dim, slide, Esc, dispose-on-detach) so the
        // Genres and Albums views do not each reimplement it.
        drawer = new OverlayTracksDrawer(genresRootPane, applicationContext);

        searchCoordinator.register(NavigationMode.GENRES, this);
    }

    /**
     * Keeps the grid backed by the repository's {@code genreIndexesProperty}, an ordered
     * {@code ReadOnlyListProperty} whose buckets are already sorted by the projection. Its contents
     * are mirrored into an {@link ObservableList} the grid displays, preserving the projection's
     * order. The projection dispatches its mutations on the JavaFX Application Thread; updates are
     * still routed through {@link Platform#runLater} defensively so the grid never mutates off-thread.
     */
    private void configureGridBacking() {
        genresBacking = FXCollections.observableArrayList(audioRepository.getGenreIndexesProperty());
        logger.info("Genres view initialised with {} genre buckets", genresBacking.size());

        // Mirror the ordered projection wholesale on every change so the grid preserves its bucket
        // ordering out of the box, rather than imposing a view-side sort.
        audioRepository.getGenreIndexesProperty().addListener((ListChangeListener<ObservableGenreIndex>) change ->
                Platform.runLater(() -> {
                    genresBacking.setAll(audioRepository.getGenreIndexesProperty());
                    // Drop cover pools for genre buckets that no longer exist so their decoded
                    // Image instances become eligible for GC over long editing sessions.
                    coverPools.keySet().retainAll(genresBacking);
                    // If the bucket whose drawer is open no longer exists, auto-close it.
                    if (selectedGenre != null && drawer.isOpen() && !genresBacking.contains(selectedGenre)) {
                        drawer.close();
                    }
                    logger.trace("Genres projection changed — backing now {}", genresBacking.size());
                }));

        // Filter layer (search) preserves the projection's order — no view-imposed sort. The
        // predicate keeps genres with at least one track matching the active query; empty shows all.
        filteredGenres = new FilteredList<>(genresBacking);
        genreGridView.setItems(filteredGenres);
    }

    /**
     * Opens the shared overlay drawer for the given genre, building its album sections via
     * {@link #buildGenreSections(ObservableGenreIndex)} and a textual header of the genre name and
     * track count. Ignored when a drawer is already open (reopen guard).
     *
     * <p>While the drawer is open a listener is attached to the genre's empty property so the drawer
     * auto-closes if the bucket drops to zero tracks. The listener is detached when the drawer closes.
     *
     * @param genre the genre whose tracks are shown in the drawer
     */
    void openDrawer(ObservableGenreIndex genre) {
        if (drawer.isOpen()) {
            return;
        }
        ObservableGenreIndex previouslySelected = selectedGenre;
        selectedGenre = genre;

        String headerText = genre.getGenreProperty().get().getName() + " · "
                + genre.getSizeProperty().get() + " tracks";
        drawer.open(buildGenreSections(genre), headerText, this::onDrawerClosed);
        drawer.applyQuery(currentSearchQuery);

        refreshCellSelection(previouslySelected);
        refreshCellSelection(genre);

        // Auto-close (signal b): a bucket dropping to zero tracks while the drawer is open closes it
        // (signal a in configureGridBacking handles outright bucket removal).
        emptyListener = (obs, was, isEmpty) -> {
            if (Boolean.TRUE.equals(isEmpty) && drawer.isOpen()) {
                drawer.close();
            }
        };
        selectedGenre.getEmptyProperty().addListener(emptyListener);
    }

    /**
     * Detaches the empty listener and clears the selection highlight when the drawer closes. Invoked
     * by the drawer on close or dispose.
     */
    private void onDrawerClosed() {
        if (emptyListener != null && selectedGenre != null) {
            selectedGenre.getEmptyProperty().removeListener(emptyListener);
            emptyListener = null;
        }
        ObservableGenreIndex deselected = selectedGenre;
        selectedGenre = null;
        refreshCellSelection(deselected);
    }

    /** Closes the open drawer, if any. Package-private entry point used by UI tests. */
    void closeDrawer() {
        drawer.close();
    }

    /**
     * Captures an immutable snapshot of {@link #genresBacking} on the FX thread. The snapshot is
     * consumed by {@link #computeMatchIds} running on the background dispatcher, preventing data
     * races with concurrent FX-thread mutations (genre bucket add/remove during import).
     */
    @Override
    public void prepareSnapshot() {
        genresSnapshot = List.copyOf(genresBacking);
    }

    /**
     * Scans the genre snapshot (captured on the FX thread by {@link #prepareSnapshot}) for genres
     * with at least one track matching the query. Must not touch any JavaFX observable or
     * {@link FilteredList} predicate.
     *
     * @param query the lower-cased search text
     * @return set of genre names that have at least one matching track
     */
    @Override
    public Set<String> computeMatchIds(String query) {
        return genresSnapshot.stream()
                .filter(genreMatchesQuery(query)::test)
                .map(g -> g.getGenreProperty().get().getName())
                .collect(toSet());
    }

    /**
     * Applies the pre-computed genre name set to the filtered list and updates the open drawer on the
     * JavaFX Application Thread. A blank {@code query} resets the view to show every genre; for a
     * non-blank query only genres whose name is in {@code ids} are shown, so an empty {@code ids}
     * hides all genres. An open drawer is narrowed to the genre's matching tracks and closed if the
     * selected genre has no match.
     *
     * @param query the lower-cased search text (forwarded to the open drawer)
     * @param ids   the set of matching genre names produced by {@link #computeMatchIds}
     */
    @Override
    public void applyMatchIds(String query, Set<String> ids) {
        currentSearchQuery = query == null ? "" : query;
        boolean reset = currentSearchQuery.isBlank();
        if (filteredGenres != null) {
            filteredGenres.setPredicate(reset ? null
                    : g -> ids.contains(g.getGenreProperty().get().getName()));
        }
        if (drawer.isOpen() && selectedGenre != null) {
            String selectedGenreName = selectedGenre.getGenreProperty().get().getName();
            if (reset || ids.contains(selectedGenreName)) {
                drawer.applyQuery(currentSearchQuery);
            } else {
                drawer.close();
            }
        }
    }

    private Predicate<ObservableGenreIndex> genreMatchesQuery(String query) {
        if (query == null || query.isEmpty()) {
            return genre -> true;
        }
        return genre -> {
            // getTracks() returns the immutable backing list, safe to read off the FX thread.
            var tracks = genre.getTracks();
            return tracks != null && tracks.stream().anyMatch(track -> AudioItemQueryMatcher.matches(track, query));
        };
    }

    /** Selects every track across all sections of the open drawer. No-op when no drawer is shown. */
    public void selectAllTracks() {
        drawer.selectAll();
    }

    /** Clears the track selection across all sections of the open drawer. No-op when no drawer is shown. */
    public void deselectAllTracks() {
        drawer.deselectAll();
    }

    /** The tracks selected across the open drawer's sections; empty when no drawer is shown. */
    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        return drawer.getSelectedTracks();
    }

    /**
     * Requests a cell update for the given genre so its {@code selected} pseudo-class state is
     * refreshed to match the current {@link #selectedGenre}.
     */
    private void refreshCellSelection(ObservableGenreIndex genre) {
        if (genre == null) {
            return;
        }
        genreGridView.lookupAll(".grid-cell").forEach(node -> {
            if (node instanceof GenreGridCell cell && genre.equals(cell.getItem())) {
                cell.updateSelectedState();
            }
        });
    }

    /**
     * Groups the genre's tracks into album sections for rendering in the overlay drawer. Only the
     * genre-tagged tracks contribute — partial albums (where only some tracks carry the genre) are
     * rendered as-is without re-aggregation against the full album tracklist.
     *
     * <p>Tracks with a non-blank album name are grouped by a composite {@code (albumName, disc)} key
     * using natural {@link String} order on the album name (matching the Artists-view convention) and
     * then by disc number. Multi-disc albums produce one entry per distinct normalized disc number;
     * single-disc albums produce one entry with disc key {@code 0} (which suppresses the "Disc N"
     * label in {@link ArtistAlbumListRow}).
     *
     * <p>Tracks whose album is {@code null} or whose album name is blank are collected into a single
     * trailing section labelled {@value #UNKNOWN_ALBUM} with disc number {@code 0}, so every genre
     * track remains reachable and playable.
     *
     * @param genre the genre index whose tracks are sectioned
     * @return ordered list of {@code (AlbumTrackGroup, discNumber)} pairs
     */
    @SuppressWarnings("java:S2589")
    List<Map.Entry<AlbumTrackGroup, Integer>> buildGenreSections(ObservableGenreIndex genre) {
        record AlbumDiscKey(String albumName, int disc) implements Comparable<AlbumDiscKey> {
            @Override
            public int compareTo(AlbumDiscKey other) {
                int cmp = albumName.compareTo(other.albumName);
                return cmp != 0 ? cmp : Integer.compare(disc, other.disc);
            }
        }

        var allTracks = genre.getTracksProperty();
        if (allTracks == null || allTracks.isEmpty()) {
            return List.of();
        }

        // Partition: tracks with a valid album name vs. album-less tracks.
        // Null/blank album fields are the album-less path; one malformed track must not NPE the rest.
        var albumLess = new ArrayList<ObservableAudioItem>();
        var albumTracks = new ArrayList<ObservableAudioItem>();
        for (var track : allTracks) {
            var albumDetails = track.getAlbumProperty().get();
            if (albumDetails != null && albumDetails.getName() != null && !albumDetails.getName().isBlank()) {
                albumTracks.add(track);
            } else {
                albumLess.add(track);
            }
        }

        // Detect which album names have more than one distinct normalized disc number.
        var multiDiscAlbums = albumTracks.stream()
                .collect(groupingBy(t -> t.getAlbumProperty().get().getName()))
                .entrySet().stream()
                .filter(e -> e.getValue().stream()
                        .map(t -> normalizeDisc(t.getDiscNumber()))
                        .distinct().count() > 1)
                .map(Map.Entry::getKey)
                .collect(toSet());

        // Group by (albumName, disc): multi-disc split per disc; single-disc use disc=0.
        var grouped = albumTracks.stream().collect(groupingBy(
                t -> new AlbumDiscKey(
                        t.getAlbumProperty().get().getName(),
                        multiDiscAlbums.contains(t.getAlbumProperty().get().getName())
                                ? normalizeDisc(t.getDiscNumber()) : 0),
                TreeMap::new,
                toList()));

        var result = new ArrayList<Map.Entry<AlbumTrackGroup, Integer>>();
        grouped.forEach((key, items) ->
                result.add(Map.entry(new AlbumTrackGroup(key.albumName(), items), key.disc())));

        // Album-less tracks → one trailing "Unknown Album" section with disc 0 (no disc label).
        if (!albumLess.isEmpty()) {
            result.add(Map.entry(new AlbumTrackGroup(UNKNOWN_ALBUM, albumLess), 0));
        }

        return result;
    }

    /**
     * Normalizes a disc number: {@code null} or non-positive values map to {@code 1}, mirroring
     * the same normalization used by the Artists view.
     */
    static int normalizeDisc(Number discNumber) {
        if (discNumber == null) return 1;
        int v = discNumber.intValue();
        return v <= 0 ? 1 : v;
    }

    /**
     * Resolves the distinct-cover pool for {@code genre}, once. Probes up to {@link #POOL_PROBE_CAP}
     * of the genre's tracks and observes each {@code coverImageProperty}: the music domain layer
     * resolves the cover off the JavaFX thread on first observation and publishes the decoded image
     * back on the JavaFX thread, where it joins the pool if not already present. Deduplicating by the
     * resolved image (rather than by album) guarantees the pool holds only genuinely distinct covers,
     * so a genre with real cover variety always has enough to cycle; tracks with no embedded art never
     * contribute. {@code onCoverArrived} runs on the JavaFX thread each time a cover joins the pool.
     */
    // Coordinates controller-scoped state (the shared cover-pool cache), not solely the genre
    // parameter, so it belongs in the controller rather than in a single grid cell.
    @SuppressWarnings("java:S3398")
    private void resolveCoverPool(ObservableGenreIndex genre, Runnable onCoverArrived) {
        CoverPool pool = coverPools.computeIfAbsent(genre, g -> new CoverPool());
        if (pool.started) {
            return;
        }
        pool.started = true;

        int probed = 0;
        for (ObservableAudioItem track : genre.getTracksProperty()) {
            if (probed >= POOL_PROBE_CAP) {
                break;
            }
            probed++;
            observeCover(track, pool, onCoverArrived);
        }
    }

    /**
     * Observes a track's cover, adding it to {@code pool} once resolved and only if the same image is
     * not already pooled. Reading the property triggers the off-thread load; if the image is already
     * present it is added immediately, otherwise a one-shot listener adds it when the load publishes
     * it on the JavaFX thread.
     */
    private void observeCover(ObservableAudioItem track, CoverPool pool, Runnable onCoverArrived) {
        var coverProperty = track.getCoverImageProperty();
        Optional<Image> current = coverProperty.get();
        if (current.isPresent()) {
            addDistinctCover(pool, current.get(), onCoverArrived);
            return;
        }
        ChangeListener<Optional<Image>>[] listener = new ChangeListener[1];
        listener[0] = (obs, oldCover, newCover) -> newCover.ifPresent(image -> {
            coverProperty.removeListener(listener[0]);
            addDistinctCover(pool, image, onCoverArrived);
        });
        coverProperty.addListener(listener[0]);
    }

    /** Adds {@code image} to the pool if not already present, then notifies {@code onCoverArrived}. */
    private static void addDistinctCover(CoverPool pool, Image image, Runnable onCoverArrived) {
        if (!pool.covers.contains(image)) {
            pool.covers.add(image);
        }
        onCoverArrived.run();
    }

    /**
     * Refreshes the resting cover of whichever grid cell currently shows {@code genre}. Invoked as
     * covers resolve so the default placeholder is replaced by a real cover on the visible cell,
     * regardless of which cell first triggered the pool resolution.
     */
    // Iterates every cell of the controller's grid view, not solely the genre parameter, so it
    // belongs in the controller rather than in a single grid cell.
    @SuppressWarnings("java:S3398")
    private void refreshGenreCover(ObservableGenreIndex genre) {
        genreGridView.lookupAll(".grid-cell").forEach(node -> {
            if (node instanceof GenreGridCell cell && genre.equals(cell.getItem())) {
                cell.applyRestingCover();
            }
        });
    }

    /** A random cover from the pool; the pool is guaranteed non-empty by the caller. */
    private static Image randomCover(CoverPool pool) {
        return pool.covers.get(ThreadLocalRandom.current().nextInt(pool.covers.size()));
    }

    /**
     * Grid cell rendering a single genre bucket as a cover image above the genre name and track count.
     * <p>
     * The genre index exposes no cover of its own, so the cell draws from the genre's pool of distinct
     * track covers (see {@link #resolveCoverPool}). At rest it shows a random pooled cover once art has
     * resolved (the default placeholder appears only until the first cover loads). While the pointer
     * moves over the cell, the cover cycles to a different random pooled cover on each movement —
     * instantly, since the pool is already resolved in memory. The last cover shown is remembered on
     * the pool, so it persists after the pointer leaves the cell and is restored if the cell is
     * recycled. A genre with fewer than two distinct covers does not cycle.
     */
    private class GenreGridCell extends GridCell<ObservableGenreIndex> {

        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final ImageView coverView = new ImageView();
        private final Label nameLabel = new Label();
        private final Label countLabel = new Label();
        private final VBox content = new VBox(6, coverView, nameLabel, countLabel);

        private ObservableGenreIndex boundGenre;

        GenreGridCell() {
            coverView.setFitWidth(COVER_SIZE);
            coverView.setFitHeight(COVER_SIZE);
            coverView.setPreserveRatio(true);
            coverView.getStyleClass().add("album-cover");
            nameLabel.setWrapText(false);
            nameLabel.setMaxWidth(COVER_SIZE);
            nameLabel.getStyleClass().add("genre-cell-name");
            countLabel.setWrapText(false);
            countLabel.setMaxWidth(COVER_SIZE);
            countLabel.getStyleClass().add("genre-cell-count");
            content.setAlignment(Pos.TOP_CENTER);
            content.setPadding(new Insets(4));
            content.getStyleClass().add("genre-cell");

            // Every pointer movement over the cell swaps to a different random pooled cover.
            setOnMouseMoved(event -> cycleCover());

            setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && getItem() != null) {
                    GenreViewController.this.openDrawer(getItem());
                }
                if (event.getClickCount() == 2 && getItem() != null) {
                    playGenre(getItem());
                }
            });
        }

        @Override
        protected void updateItem(ObservableGenreIndex genre, boolean empty) {
            super.updateItem(genre, empty);

            if (empty || genre == null) {
                boundGenre = null;
                setGraphic(null);
                return;
            }

            // controlsfx re-invokes updateItem frequently, so only react when the cell actually
            // rebinds to a different genre: refresh the labels and the resting cover once.
            if (genre != boundGenre) {
                boundGenre = genre;
                nameLabel.setText(genre.getGenreProperty().get().getName());
                countLabel.setText(genre.getSizeProperty().get() + " tracks");
                applyRestingCover();
                // Kick one-time off-thread resolution; each arriving cover refreshes whatever cell
                // currently shows this genre, so the default placeholder is replaced as soon as art exists.
                resolveCoverPool(genre, () -> refreshGenreCover(genre));
            }

            updateSelectedState();
            setGraphic(content);
        }

        /** Applies the {@code selected} pseudo-class based on whether this cell's genre is the one open in the drawer. */
        void updateSelectedState() {
            content.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, getItem() != null && getItem().equals(selectedGenre));
        }

        /**
         * Shows this cell's resting cover: the cover the user last cycled to (persisted on the pool),
         * or — if none has been chosen — a random cover from the genre's resolved pool. Falls back to
         * the default placeholder only while the pool holds no resolved cover yet; once at least one
         * track's art has resolved, a real cover is always shown.
         */
        void applyRestingCover() {
            ObservableGenreIndex genre = getItem();
            if (genre == null) {
                return;
            }
            CoverPool pool = coverPools.get(genre);
            if (pool != null && !pool.covers.isEmpty()) {
                if (pool.lastShown == null || !pool.covers.contains(pool.lastShown)) {
                    pool.lastShown = randomCover(pool);
                }
                coverView.setImage(pool.lastShown);
            } else {
                coverView.setImage(ApplicationImage.DEFAULT_COVER.get());
            }
        }

        /**
         * On pointer movement, swaps to a different random cover from the genre's pool and records it
         * as the pool's last-shown cover so it persists after the pointer leaves. No-op until at least
         * two distinct covers have resolved (nothing to cycle through).
         */
        private void cycleCover() {
            ObservableGenreIndex genre = getItem();
            if (genre == null) {
                return;
            }
            CoverPool pool = coverPools.get(genre);
            if (pool == null || pool.covers.size() < 2) {
                return;
            }
            int currentIndex = pool.lastShown == null ? -1 : pool.covers.indexOf(pool.lastShown);
            int next = ThreadLocalRandom.current().nextInt(pool.covers.size());
            if (next == currentIndex) {
                next = (next + 1) % pool.covers.size();
            }
            pool.lastShown = pool.covers.get(next);
            coverView.setImage(pool.lastShown);
        }
    }

    // Publishes through the controller's application event bus, not solely the genre parameter, so
    // it belongs in the controller rather than in a single grid cell. The null guard is defensive:
    // a mock or partial genre index can return a null track list despite the non-null contract.
    @SuppressWarnings({"java:S3398", "java:S2589"})
    private void playGenre(ObservableGenreIndex genre) {
        var tracks = genre.getTracksProperty();
        if (tracks != null && !tracks.isEmpty()) {
            applicationContext.publishEvent(new PlayItemEvent(List.copyOf(tracks), this));
        }
    }
}
