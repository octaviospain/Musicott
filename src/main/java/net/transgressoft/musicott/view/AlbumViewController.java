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
import net.transgressoft.commons.fx.music.audio.ObservableAlbum;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.OverlayTracksDrawer;
import net.transgressoft.musicott.view.custom.table.AlbumTrackGroup;
import net.transgressoft.musicott.view.custom.table.AudioItemQueryMatcher;
import net.transgressoft.musicott.events.SearchTextTypedEvent;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Controller for the Albums navigation view. Renders the audio library's album catalog as a grid of
 * cover-art cells backed directly by {@link ObservableAudioLibrary#getAlbumsProperty()}, the reactive
 * album projection provided by the music domain layer. The view does not re-group audio items by
 * album: the projection is the single source of truth and the grid reflects its set membership
 * reactively as tracks are imported, edited, or removed.
 *
 * <p>Single-clicking a cover cell slides in a right overlay drawer showing the album's tracks,
 * grouped into disc sections for multi-disc albums. Clicking the dimmed grid area or pressing Esc
 * closes the drawer. Track playback inherits the embedded {@link SimpleAudioItemTableView} behavior
 * (double-click to play, right-click context menu).
 *
 * <p>The currently selected album (whose drawer is open) is tracked via {@link #selectedAlbum} and
 * reflected on each {@link AlbumGridCell} through a {@link PseudoClass} so the CSS layer can render
 * a persistent selection indicator distinct from the hover glow.
 *
 * @author Octavio Calleya
 */
@FxmlView("/fxml/AlbumViewController.fxml")
@Controller
public class AlbumViewController {

    private static final double CELL_WIDTH = 170.0;
    private static final double CELL_HEIGHT = 210.0;
    private static final double COVER_SIZE = 150.0;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final ObservableAudioLibrary audioRepository;
    private final ApplicationContext applicationContext;

    @FXML
    private StackPane albumsRootPane;

    private GridView<ObservableAlbum> albumGridView;
    private FilteredList<ObservableAlbum> filteredAlbums;

    /** The shared overlay drawer showing the selected album's tracks. */
    private OverlayTracksDrawer drawer;

    /** Lower-cased active search query, or empty when no filter is applied. */
    private String currentSearchQuery = "";

    /** The album whose drawer is currently open; {@code null} when no drawer is shown. */
    ObservableAlbum selectedAlbum;

    @Autowired
    public AlbumViewController(ObservableAudioLibrary audioLibrary, ApplicationContext applicationContext) {
        this.audioRepository = audioLibrary;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        albumGridView = new GridView<>();
        albumGridView.setCellWidth(CELL_WIDTH);
        albumGridView.setCellHeight(CELL_HEIGHT);
        albumGridView.setHorizontalCellSpacing(12);
        albumGridView.setVerticalCellSpacing(12);
        albumGridView.setCellFactory(grid -> new AlbumGridCell());

        configureGridBacking();

        // GridView reports a content-sized preferred height (all rows); left unconstrained it balloons
        // ancestor preferred heights and pushes the bottom player layout off-screen. Bind its size to
        // the containing pane so the grid always fits the available area and scrolls internally.
        albumGridView.setMinSize(0, 0);
        albumGridView.prefWidthProperty().bind(albumsRootPane.widthProperty());
        albumGridView.prefHeightProperty().bind(albumsRootPane.heightProperty());
        albumGridView.maxWidthProperty().bind(albumsRootPane.widthProperty());
        albumGridView.maxHeightProperty().bind(albumsRootPane.heightProperty());
        albumsRootPane.getChildren().add(albumGridView);

        // The shared drawer owns the overlay lifecycle (dim, slide, Esc, dispose-on-detach) so the
        // Albums and Genres views do not each reimplement it.
        drawer = new OverlayTracksDrawer(albumsRootPane, applicationContext);
    }

    /**
     * Keeps the grid backed by the repository's {@code albumsProperty}, an ordered
     * {@code ReadOnlyListProperty} whose entries are already sorted by album title. Its contents are
     * mirrored into an {@link ObservableList} the grid displays, preserving the projection's order.
     * The projection dispatches its mutations on the JavaFX Application Thread; updates are still
     * routed through {@link Platform#runLater} defensively so the grid never mutates off-thread.
     */
    private void configureGridBacking() {
        ObservableList<ObservableAlbum> albumsBacking = FXCollections.observableArrayList(audioRepository.getAlbumsProperty());
        logger.info("Albums view initialised with {} albums", albumsBacking.size());

        // Mirror the ordered projection wholesale on every change so the grid preserves its
        // album-title ordering out of the box, rather than imposing a view-side sort.
        audioRepository.getAlbumsProperty().addListener((ListChangeListener<ObservableAlbum>) change ->
                Platform.runLater(() -> {
                    albumsBacking.setAll(audioRepository.getAlbumsProperty());
                    logger.trace("Albums projection changed — backing now {}", albumsBacking.size());
                }));

        // Filter layer (search) preserves the projection's order — no view-imposed sort. The
        // predicate keeps albums with at least one track matching the active query; empty shows all.
        filteredAlbums = new FilteredList<>(albumsBacking);
        albumGridView.setItems(filteredAlbums);
    }

    /**
     * Opens the shared overlay drawer for the given album, computing its disc sections via
     * {@link #buildAlbumSections(ObservableAlbum)}. The header is left empty so the Albums drawer
     * appearance is preserved (the album title is rendered by the first row's own header label).
     * Ignored when a drawer is already open.
     *
     * @param album the album whose tracks are shown in the drawer
     */
    void openDrawer(ObservableAlbum album) {
        if (drawer.isOpen()) {
            return;
        }
        ObservableAlbum previouslySelected = selectedAlbum;
        selectedAlbum = album;
        drawer.open(buildAlbumSections(album), "", this::onDrawerClosed);
        drawer.applyQuery(currentSearchQuery);
        refreshCellSelection(previouslySelected);
        refreshCellSelection(album);
    }

    /** Clears the selection highlight when the drawer closes. Invoked by the drawer on close/dispose. */
    private void onDrawerClosed() {
        ObservableAlbum deselected = selectedAlbum;
        selectedAlbum = null;
        refreshCellSelection(deselected);
    }

    /** Closes the open drawer, if any. Package-private entry point used by UI tests. */
    void closeDrawer() {
        drawer.close();
    }

    /**
     * Requests a cell update for the given album so its {@code selected} pseudo-class state is
     * refreshed to match the current {@link #selectedAlbum}. Iterates the grid's visible cells;
     * cells that are not yet realised will apply the correct state when they are recycled via
     * {@code updateItem}.
     */
    private void refreshCellSelection(ObservableAlbum album) {
        if (album == null) {
            return;
        }
        albumGridView.lookupAll(".grid-cell").forEach(node -> {
            if (node instanceof AlbumGridCell cell && album.equals(cell.getItem())) {
                cell.updateSelectedState();
            }
        });
    }

    /**
     * Filters the albums view to the current search query, mirroring the artist view. Only albums
     * with at least one track matching the query stay in the grid; an open drawer is narrowed to the
     * album's matching tracks and closed if the selected album has no match. An empty query restores
     * the full view.
     */
    @EventListener
    public void searchTextTypedEvent(SearchTextTypedEvent event) {
        currentSearchQuery = event.searchText == null ? "" : event.searchText.toLowerCase();
        if (filteredAlbums != null) {
            filteredAlbums.setPredicate(albumMatchesQuery(currentSearchQuery));
        }
        if (drawer.isOpen() && selectedAlbum != null) {
            if (albumMatchesQuery(currentSearchQuery).test(selectedAlbum)) {
                drawer.applyQuery(currentSearchQuery);
            } else {
                drawer.close();
            }
        }
    }

    private Predicate<ObservableAlbum> albumMatchesQuery(String query) {
        if (query == null || query.isEmpty()) {
            return album -> true;
        }
        return album -> {
            var tracks = album.getTracks();
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
     * Groups the album's tracks into disc sections for rendering in the overlay drawer. A single-disc
     * album produces one entry with disc key {@code 0} (no "Disc N" label rendered by
     * {@link ArtistAlbumListRow}). A multi-disc album produces one entry per distinct normalized disc
     * number, preserving the disc-then-track order already guaranteed by the domain layer.
     *
     * <p>Disc numbers that are {@code null} or non-positive are normalized to {@code 1}, mirroring
     * the same normalization in the Artists view.
     *
     * @param album the album to section
     * @return ordered list of {@code (AlbumTrackGroup, discNumber)} pairs
     */
    // Defensive null guard — a mock or partially-built album can return a null track list despite
    // the non-null domain contract; this must not NPE.
    @SuppressWarnings("java:S2589")
    List<Map.Entry<AlbumTrackGroup, Integer>> buildAlbumSections(ObservableAlbum album) {
        var tracks = album.getTracks();
        if (tracks == null || tracks.isEmpty()) {
            return List.of();
        }

        var discNumbers = tracks.stream()
                .map(t -> normalizeDisc(t.getDiscNumber()))
                .distinct()
                .toList();

        if (discNumbers.size() <= 1) {
            return List.of(Map.entry(new AlbumTrackGroup(album.getAlbumName(), tracks), 0));
        }

        var byDisc = new TreeMap<Integer, List<ObservableAudioItem>>();
        for (var track : tracks) {
            int disc = normalizeDisc(track.getDiscNumber());
            byDisc.computeIfAbsent(disc, _ -> new ArrayList<>()).add(track);
        }

        return byDisc.entrySet().stream()
                .map(e -> Map.entry(new AlbumTrackGroup(album.getAlbumName(), e.getValue()), e.getKey()))
                .toList();
    }

    static int normalizeDisc(Number discNumber) {
        if (discNumber == null) return 1;
        int v = discNumber.intValue();
        return v <= 0 ? 1 : v;
    }

    /**
     * Grid cell rendering a single album as a cover image above the album name and album artist.
     *
     * <p>Cover art loads lazily and only for cells the grid realises: observing {@code coverProperty}
     * (reading it and attaching the listener in {@code updateItem}) triggers the album to resolve its
     * cover off the JavaFX thread and publish the decoded image back on the JavaFX thread, where this
     * cell applies it. The previously bound album's listener is detached on every update so a recycled
     * cell never shows a stale cover.
     *
     * <p>Note: this manual listener approach (rather than a direct JavaFX binding on {@code coverProperty})
     * is intentional. Binding the cover property directly on the FX thread is a tracked upstream
     * follow-up once the domain layer makes it safe.
     *
     * <p>The cell content carries the {@code album-cell} style class to enable hover and selection
     * effects in CSS. A {@link PseudoClass} named {@code selected} is toggled via
     * {@link #updateSelectedState()} whenever the controller's selection changes.
     */
    private class AlbumGridCell extends GridCell<ObservableAlbum> {

        private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");

        private final ImageView coverView = new ImageView();
        private final Label nameLabel = new Label();
        private final Label artistLabel = new Label();
        private final VBox content = new VBox(6, coverView, nameLabel, artistLabel);

        private ObservableAlbum boundAlbum;
        private final ChangeListener<Optional<Image>> coverListener =
                (obs, oldCover, newCover) -> {
                    if (getItem() == boundAlbum) {
                        applyCover(newCover);
                    }
                };

        AlbumGridCell() {
            coverView.setFitWidth(COVER_SIZE);
            coverView.setFitHeight(COVER_SIZE);
            coverView.setPreserveRatio(true);
            coverView.getStyleClass().add("album-cover");
            // Single line each, ellipsised when too long, so the cell height stays uniform.
            nameLabel.setWrapText(false);
            nameLabel.setMaxWidth(COVER_SIZE);
            nameLabel.getStyleClass().add("album-cell-name");
            artistLabel.setWrapText(false);
            artistLabel.setMaxWidth(COVER_SIZE);
            artistLabel.getStyleClass().add("album-cell-artist");
            content.setAlignment(Pos.TOP_CENTER);
            content.setPadding(new Insets(4));
            content.getStyleClass().add("album-cell");

            setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && getItem() != null) {
                    AlbumViewController.this.openDrawer(getItem());
                }
            });
        }

        @Override
        protected void updateItem(ObservableAlbum album, boolean empty) {
            super.updateItem(album, empty);

            if (boundAlbum != null) {
                boundAlbum.getCoverProperty().removeListener(coverListener);
            }

            if (empty || album == null) {
                boundAlbum = null;
                setGraphic(null);
                return;
            }

            boundAlbum = album;
            nameLabel.setText(album.getAlbumName());
            artistLabel.setText(displayArtist(album));
            // Reading the value and attaching the listener triggers the off-thread cover load; the
            // resolved image arrives via the listener. Apply the current value so a recycled cell shows
            // the placeholder (or an already-resolved cover) immediately rather than a stale one.
            applyCover(album.getCoverProperty().get());
            album.getCoverProperty().addListener(coverListener);

            updateSelectedState();
            setGraphic(content);
        }

        /** Applies the {@code selected} pseudo-class based on whether this cell's album is the one open in the drawer. */
        void updateSelectedState() {
            content.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, getItem() != null && getItem().equals(selectedAlbum));
        }

        private void applyCover(Optional<Image> cover) {
            coverView.setImage(cover.orElse(ApplicationImage.DEFAULT_COVER.get()));
        }
    }

    /**
     * Resolves the artist line shown under an album title: the album artist when set, "Various
     * Artists" for compilations, otherwise the primary artist of the album's first track.
     */
    // Defensive null guards — a mock or partially-built album can return a null album artist, track
    // list, or per-track artist/name despite the non-null domain contract; this must not NPE.
    @SuppressWarnings("java:S2589")
    private static String displayArtist(ObservableAlbum album) {
        var albumArtist = album.getAlbumArtist();
        if (albumArtist != null && albumArtist.getName() != null && !albumArtist.getName().isBlank()) {
            return albumArtist.getName();
        }
        if (album.isCompilation()) {
            return "Various Artists";
        }
        var tracks = album.getTracks();
        if (tracks != null && !tracks.isEmpty()) {
            var artist = tracks.getFirst().getArtist();
            if (artist != null && artist.getName() != null) {
                return artist.getName();
            }
        }
        return "";
    }
}
