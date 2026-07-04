package net.transgressoft.musicott.view;

import javafx.animation.TranslateTransition;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.rgielen.fxweaver.core.FxmlView;
import net.transgressoft.commons.fx.music.audio.ObservableAlbum;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import net.transgressoft.musicott.view.custom.table.AlbumTrackGroup;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.events.SearchTextTypedEvent;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
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
import java.util.Objects;
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
    /** Minimum drawer width so it stays usable on small windows. */
    private static final double DRAWER_MIN_WIDTH = 280.0;
    /** Drawer width as a fraction of the albums area, leaving the remainder for the grid. */
    private static final double DRAWER_WIDTH_FRACTION = 0.8;

    private final Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final ObservableAudioLibrary audioRepository;
    private final ApplicationContext applicationContext;

    @FXML
    private StackPane albumsRootPane;

    private GridView<ObservableAlbum> albumGridView;
    private ObservableList<ObservableAlbum> albumsBacking;
    private FilteredList<ObservableAlbum> filteredAlbums;

    private Pane dimPane;
    private VBox drawerPane;

    /** The {@link ArtistAlbumListRow} disc sections currently rendered inside the open drawer. */
    private final List<ArtistAlbumListRow> drawerRows = new ArrayList<>();

    /** Lower-cased active search query, or empty when no filter is applied. */
    private String currentSearchQuery = "";

    /** The album whose drawer is currently open; {@code null} when no drawer is shown. */
    ObservableAlbum selectedAlbum;

    /**
     * Closes the overlay drawer when Esc is pressed. Consumes the event so it does not propagate to
     * other components. Attached to the scene as a KEY_PRESSED filter — not a handler — so it fires
     * regardless of which node has focus.
     */
    private final javafx.event.EventHandler<KeyEvent> escHandler = event -> {
        if (event.getCode() == KeyCode.ESCAPE && drawerPane != null) {
            closeDrawer();
            event.consume();
        }
    };

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

        // Attach or detach the Esc key filter as this view enters or leaves a scene, preventing the
        // filter from leaking into other navigation modes when the Albums view is not visible.
        albumsRootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, escHandler);
            }
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, escHandler);
                // A navigation-mode switch removes this view from the scene graph while a drawer may
                // still be open. The dim and drawer are children of albumsRootPane, so without an
                // explicit teardown they would persist as a stuck, non-dismissable overlay on return,
                // and the open guard would then permanently block reopening. Dispose immediately —
                // an animated close is neither visible nor able to complete off-scene.
                disposeDrawer();
            }
        });
    }

    /**
     * Keeps the grid backed by the repository's {@code albumsProperty}, an ordered
     * {@code ReadOnlyListProperty} whose entries are already sorted by album title. Its contents are
     * mirrored into an {@link ObservableList} the grid displays, preserving the projection's order.
     * The projection dispatches its mutations on the JavaFX Application Thread; updates are still
     * routed through {@link Platform#runLater} defensively so the grid never mutates off-thread.
     */
    private void configureGridBacking() {
        albumsBacking = FXCollections.observableArrayList(audioRepository.getAlbumsProperty());
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
     * Opens the right overlay drawer for the given album. If the drawer is already open (e.g., the
     * user clicked a second cell before the first drawer closed), the call is ignored.
     *
     * <p>A full-size dim {@link Pane} is layered above the grid and intercepts mouse clicks to close
     * the drawer. The drawer itself is a responsive-width {@link VBox} aligned to the right edge of
     * the root pane: its width is bound to 40% of the root pane width (with a sensible minimum) so
     * it never occupies the full grid even on narrow windows. It slides in from the right via a
     * {@link TranslateTransition}.
     *
     * <p>Album tracks are rendered by one {@link ArtistAlbumListRow} per disc section, obtained as
     * prototype Spring beans. Rows are added to the drawer before the drawer is attached to the root
     * pane so that the row's scene-attachment subscription wiring fires correctly.
     *
     * <p>{@code artists.css} is scoped to the drawer node so that the {@link ArtistAlbumListRow}
     * labels ({@code #albumTitleLabel}, {@code .album-info-secondary}, etc.) render with the same
     * visual treatment they have in the artist view, without leaking those rules into the grid.
     *
     * @param album the album whose tracks are shown in the drawer
     */
    void openDrawer(ObservableAlbum album) {
        if (drawerPane != null) {
            return;
        }

        dimPane = new Pane();
        dimPane.getStyleClass().add("album-drawer-dim");
        dimPane.prefWidthProperty().bind(albumsRootPane.widthProperty());
        dimPane.prefHeightProperty().bind(albumsRootPane.heightProperty());
        dimPane.setOnMouseClicked(_ -> closeDrawer());

        var sections = buildAlbumSections(album);
        var sectionsVBox = new VBox(8);
        drawerRows.clear();
        for (var entry : sections) {
            var tableView = applicationContext.getBean(SimpleAudioItemTableView.class);
            var row = applicationContext.getBean(ArtistAlbumListRow.class,
                    Artist.UNKNOWN, entry.getKey(), tableView, entry.getValue());
            drawerRows.add(row);
            sectionsVBox.getChildren().add(row);
        }
        // Honor an active search query: narrow each section to matching tracks and hide sections
        // (discs) that contain none, so the drawer mirrors the same filter as the grid.
        applyDrawerQuery();

        var scrollPane = new ScrollPane(sectionsVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        // Prevent the inner scroll content from forcing the drawer wider than its bound width.
        scrollPane.maxWidthProperty().bind(albumsRootPane.widthProperty().multiply(DRAWER_WIDTH_FRACTION)
                .map(w -> Math.max(w.doubleValue(), DRAWER_MIN_WIDTH)));

        // The drawer and its rows must be constructed before adding to the root pane so that the
        // ArtistAlbumListRow scene-attachment subscription fires with a non-null scene, wiring up
        // the EasyBind subscriptions that keep the row's content reactive.
        drawerPane = new VBox(scrollPane);
        drawerPane.getStyleClass().add("album-drawer");
        // Scope artists.css to the drawer subtree so ArtistAlbumListRow labels (album title,
        // genres, related-artists) match the visual treatment from the artist view.
        drawerPane.getStylesheets().add(getClass().getResource("/css/artists.css").toExternalForm());
        // Cap the drawer at 40% of the available area with a minimum floor so it is usable at
        // any window size; the grid remains visible in the remaining 60%.
        drawerPane.prefWidthProperty().bind(albumsRootPane.widthProperty().multiply(DRAWER_WIDTH_FRACTION)
                .map(w -> Math.max(w.doubleValue(), DRAWER_MIN_WIDTH)));
        drawerPane.maxWidthProperty().bind(drawerPane.prefWidthProperty());
        drawerPane.setMinWidth(DRAWER_MIN_WIDTH);
        StackPane.setAlignment(drawerPane, Pos.CENTER_RIGHT);

        // Start off-screen to the right by the drawer's current resolved width before the slide-in.
        double initialOffset = Math.max(albumsRootPane.getWidth() * DRAWER_WIDTH_FRACTION, DRAWER_MIN_WIDTH);
        drawerPane.setTranslateX(initialOffset);

        albumsRootPane.getChildren().addAll(dimPane, drawerPane);

        // Update the selected album and refresh grid cells so the previously selected cell deselects
        // and this one gains the selected pseudo-class.
        ObservableAlbum previouslySelected = selectedAlbum;
        selectedAlbum = album;
        refreshCellSelection(previouslySelected);
        refreshCellSelection(album);

        var slideIn = new TranslateTransition(Duration.millis(220), drawerPane);
        slideIn.setToX(0);
        slideIn.play();
    }

    /**
     * Closes the overlay drawer with a slide-out animation. The dim pane and drawer pane are removed
     * from the root pane when the animation finishes. The fields are nulled immediately so the
     * close guard and the Esc handler see the closed state without waiting for the animation to end.
     */
    void closeDrawer() {
        if (drawerPane == null) {
            return;
        }
        // Null the fields immediately so re-entrant calls (e.g. Esc fired twice) see closed state.
        var localDim = dimPane;
        var localDrawer = drawerPane;
        dimPane = null;
        drawerPane = null;
        drawerRows.clear();

        // Slide out by the current resolved drawer width so the animation exactly reverses the slide-in.
        double slideOutOffset = Math.max(albumsRootPane.getWidth() * DRAWER_WIDTH_FRACTION, DRAWER_MIN_WIDTH);
        var slideOut = new TranslateTransition(Duration.millis(180), localDrawer);
        slideOut.setToX(slideOutOffset);
        slideOut.setOnFinished(_ -> albumsRootPane.getChildren().removeAll(localDim, localDrawer));
        slideOut.play();

        ObservableAlbum deselected = selectedAlbum;
        selectedAlbum = null;
        refreshCellSelection(deselected);
    }

    /**
     * Tears the drawer down synchronously, clearing its state and detaching the overlay nodes without
     * a slide-out animation. Used when the view leaves the scene graph, where an animated close cannot
     * complete and leaving the overlay attached would block reopening on return.
     */
    private void disposeDrawer() {
        if (drawerPane == null) {
            return;
        }
        var localDim = dimPane;
        var localDrawer = drawerPane;
        dimPane = null;
        drawerPane = null;
        selectedAlbum = null;
        drawerRows.clear();
        albumsRootPane.getChildren().removeAll(localDim, localDrawer);
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
        if (drawerPane != null && selectedAlbum != null) {
            if (albumMatchesQuery(currentSearchQuery).test(selectedAlbum)) {
                applyDrawerQuery();
            } else {
                closeDrawer();
            }
        }
    }

    /** Narrows each open drawer section to tracks matching the active query and hides empty sections. */
    private void applyDrawerQuery() {
        for (var row : drawerRows) {
            row.filterTracksByQuery(currentSearchQuery);
            boolean hasMatch = row.hasTracksMatching(currentSearchQuery);
            row.setVisible(hasMatch);
            row.setManaged(hasMatch);
        }
    }

    private Predicate<ObservableAlbum> albumMatchesQuery(String query) {
        if (query == null || query.isEmpty()) {
            return album -> true;
        }
        return album -> {
            var tracks = album.getTracks();
            return tracks != null && tracks.stream().anyMatch(track -> audioItemMatchesQuery(track, query));
        };
    }

    /** Selects every track across all sections of the open drawer. No-op when no drawer is shown. */
    public void selectAllTracks() {
        drawerRows.forEach(ArtistAlbumListRow::selectAllAudioItems);
    }

    /** Clears the track selection across all sections of the open drawer. No-op when no drawer is shown. */
    public void deselectAllTracks() {
        drawerRows.forEach(ArtistAlbumListRow::deselectAllAudioItems);
    }

    /** The tracks selected across the open drawer's sections; empty when no drawer is shown. */
    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        var selected = FXCollections.<ObservableAudioItem>observableArrayList();
        drawerRows.forEach(row -> selected.addAll(row.selectedAudioItemsProperty()));
        return selected;
    }

    // Defensive null guards — imported tracks from partial catalogs can carry null artist/album/
    // album-artist or null name fields; one malformed track must not NPE the whole album filter.
    // Mirrors the artist-view matching so search behaves identically across navigation modes.
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

    private static int normalizeDisc(Number discNumber) {
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
