package net.transgressoft.musicott.view.custom;

import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.musicott.view.custom.table.AlbumTrackGroup;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.AudioItemQueryMatcher;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Right-side overlay drawer shared by the Albums and Genres navigation modes. Given a root
 * {@link StackPane} it owns the full overlay lifecycle: a dimmed backdrop, a resizable panel that
 * slides in from the right, an optional textual header, a scrollable stack of
 * {@link ArtistAlbumListRow} sections, and teardown via click-away, Esc, or scene detachment.
 *
 * <p>The drawer is content-agnostic: callers supply a pre-built list of {@code (AlbumTrackGroup,
 * discNumber)} sections and an optional header string, so each navigation mode keeps its own grid,
 * cell rendering, selection state, and section-building logic while reusing one drawer
 * implementation. Disc {@code 0} suppresses the "Disc N" label; a non-empty header renders a
 * {@code drawer-header}-styled label above the sections.
 *
 * <p>The drawer opens at its maximum width (80% of the root pane, floored on small windows) and can
 * be resized by dragging the handle on its left edge, down to a minimum that still shows each row's
 * cover art. The chosen width is remembered across re-opens and re-clamped when the window resizes.
 *
 * <p>The Esc key filter and dispose-on-detach behavior are registered on the root pane's scene, so
 * the drawer never leaks its key handler into other navigation modes and never leaves a stuck
 * overlay when the view is swapped out. {@code artists.css} is scoped to the drawer subtree so the
 * embedded rows render with the artist-view visual treatment.
 *
 * @author Octavio Calleya
 */
public class OverlayTracksDrawer {

    /** Default/maximum drawer width as a fraction of the root pane, leaving the rest for the grid. */
    private static final double DRAWER_WIDTH_FRACTION = 0.8;
    /** Floor for the computed maximum width so the drawer stays usable on small windows. */
    private static final double MAX_WIDTH_FLOOR = 280.0;
    // Narrowest the user can drag the drawer. The album/disc rows render a 130px cover with 20px
    // margins each side (~170px column); this floor keeps that column — plus the resize handle and
    // the vertical scrollbar — visible so the cover art stays fully in view at the smallest size.
    private static final double RESIZE_MIN_WIDTH = 210.0;
    /** Width of the left-edge drag handle. */
    private static final double RESIZE_HANDLE_WIDTH = 6.0;
    private static final Duration SLIDE_IN = Duration.millis(220);
    private static final Duration SLIDE_OUT = Duration.millis(180);

    private final StackPane rootPane;
    private final ApplicationContext applicationContext;

    private Pane dimPane;
    private HBox drawerPane;
    private final List<ArtistAlbumListRow> drawerRows = new ArrayList<>();
    private Runnable onClosed;

    // Nodes and animation of a drawer still sliding out after close(); purged if a reopen or a
    // dispose happens before the slide-out finishes, so a fast reopen never stacks two overlays.
    private Pane closingDimPane;
    private HBox closingDrawerPane;
    private TranslateTransition closingTransition;

    /** Lower-cased active search query narrowing the drawer's rows; empty applies no filter. */
    private String currentQuery = "";

    /** Live width of the open drawer; also the slide-out offset. */
    private double currentWidth;
    /** The user's last chosen width, remembered across re-opens; {@code < 0} means use the maximum. */
    private double preferredWidth = -1;
    private double dragStartSceneX;
    private double dragStartWidth;

    /**
     * Closes the drawer when Esc is pressed. Consumes the event so it does not propagate. Attached to
     * the scene as a KEY_PRESSED filter — not a handler — so it fires regardless of focus.
     */
    private final EventHandler<KeyEvent> escHandler = event -> {
        if (event.getCode() == KeyCode.ESCAPE && drawerPane != null) {
            close();
            event.consume();
        }
    };

    /**
     * @param rootPane           the pane the overlay is layered onto (the navigation view's root)
     * @param applicationContext used to resolve the per-section table view and row beans
     */
    public OverlayTracksDrawer(StackPane rootPane, ApplicationContext applicationContext) {
        this.rootPane = rootPane;
        this.applicationContext = applicationContext;
        // Attach or detach the Esc filter as the view enters or leaves a scene. On detachment,
        // dispose synchronously: an animated close cannot complete off-scene and a leftover overlay
        // would block reopening on return.
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, escHandler);
            }
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, escHandler);
                dispose();
            }
        });
        // Keep the open drawer within the resize bounds as the window changes size.
        rootPane.widthProperty().addListener((obs, ov, nv) -> {
            if (drawerPane != null) {
                applyWidth(resolveWidth());
            }
        });
    }

    /** @return {@code true} while a drawer is shown. */
    public boolean isOpen() {
        return drawerPane != null;
    }

    /**
     * Opens the drawer with the given sections and optional header. Ignored if a drawer is already
     * open (reopen guard). The {@code onClosed} callback runs exactly once when this drawer is later
     * closed or disposed, letting the caller clear its own selection state.
     *
     * @param sections   ordered {@code (AlbumTrackGroup, discNumber)} pairs to render as rows
     * @param headerText header label text; pass {@code null} or empty to render no header
     * @param onClosed   invoked once when the drawer closes or is disposed; may be {@code null}
     */
    public void open(List<Map.Entry<AlbumTrackGroup, Integer>> sections, String headerText, Runnable onClosed) {
        if (drawerPane != null) {
            return;
        }
        // A previous close may still be sliding out; remove it now so this new drawer does not
        // stack on top of it.
        purgePendingClose();
        this.onClosed = onClosed;

        dimPane = new Pane();
        dimPane.getStyleClass().add("album-drawer-dim");
        dimPane.prefWidthProperty().bind(rootPane.widthProperty());
        dimPane.prefHeightProperty().bind(rootPane.heightProperty());
        dimPane.setOnMouseClicked(_ -> close());

        var sectionsVBox = new VBox(8);
        drawerRows.clear();
        for (var entry : sections) {
            var tableView = applicationContext.getBean(SimpleAudioItemTableView.class);
            var row = applicationContext.getBean(ArtistAlbumListRow.class,
                    Artist.UNKNOWN, entry.getKey(), tableView, entry.getValue());
            drawerRows.add(row);
            sectionsVBox.getChildren().add(row);
        }
        applyQueryToRows();

        var scrollPane = new ScrollPane(sectionsVBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // The content column: optional header above the scrollable sections. It grows to fill the
        // drawer width minus the resize handle.
        VBox contentBox;
        if (headerText != null && !headerText.isEmpty()) {
            var headerLabel = new Label(headerText);
            headerLabel.getStyleClass().add("drawer-header");
            contentBox = new VBox(headerLabel, scrollPane);
        } else {
            contentBox = new VBox(scrollPane);
        }
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        var resizeHandle = new Region();
        resizeHandle.getStyleClass().add("drawer-resize-handle");
        resizeHandle.setCursor(Cursor.H_RESIZE);
        resizeHandle.setMinWidth(RESIZE_HANDLE_WIDTH);
        resizeHandle.setPrefWidth(RESIZE_HANDLE_WIDTH);
        resizeHandle.setMaxWidth(RESIZE_HANDLE_WIDTH);
        resizeHandle.setMaxHeight(Double.MAX_VALUE);
        resizeHandle.setOnMousePressed(this::onResizeStart);
        resizeHandle.setOnMouseDragged(this::onResizeDrag);

        // The drawer and its rows must be built before adding to the root pane so the row's
        // scene-attachment subscription fires with a non-null scene and wires its reactive bindings.
        drawerPane = new HBox(resizeHandle, contentBox);
        drawerPane.getStyleClass().add("album-drawer");
        // Scope artists.css to the drawer subtree so the embedded row labels match the artist view.
        drawerPane.getStylesheets().add(getClass().getResource("/css/artists.css").toExternalForm());
        applyWidth(resolveWidth());
        StackPane.setAlignment(drawerPane, Pos.CENTER_RIGHT);

        // Start off-screen to the right by the drawer's current width before the slide-in.
        drawerPane.setTranslateX(currentWidth);

        rootPane.getChildren().addAll(dimPane, drawerPane);

        var slideIn = new TranslateTransition(SLIDE_IN, drawerPane);
        slideIn.setToX(0);
        slideIn.play();
    }

    /**
     * Closes the drawer with a slide-out animation and removes the overlay nodes when it finishes.
     * Fields are nulled immediately so re-entrant calls and the Esc handler see the closed state, and
     * the {@code onClosed} callback runs once.
     */
    public void close() {
        if (drawerPane == null) {
            return;
        }
        var localDim = dimPane;
        var localDrawer = drawerPane;
        var callback = onClosed;
        double width = currentWidth;
        dimPane = null;
        drawerPane = null;
        onClosed = null;
        drawerRows.clear();

        localDim.prefWidthProperty().unbind();
        localDim.prefHeightProperty().unbind();

        var slideOut = new TranslateTransition(SLIDE_OUT, localDrawer);
        slideOut.setToX(width);
        slideOut.setOnFinished(_ -> {
            rootPane.getChildren().removeAll(localDim, localDrawer);
            if (closingDrawerPane == localDrawer) {
                closingDimPane = null;
                closingDrawerPane = null;
                closingTransition = null;
            }
        });
        closingDimPane = localDim;
        closingDrawerPane = localDrawer;
        closingTransition = slideOut;
        slideOut.play();

        if (callback != null) {
            callback.run();
        }
    }

    /**
     * Tears the drawer down synchronously without a slide-out animation, running the {@code onClosed}
     * callback once. Used when the view leaves the scene graph.
     */
    public void dispose() {
        // Remove any drawer still sliding out from an earlier close before checking the open state.
        purgePendingClose();
        if (drawerPane == null) {
            return;
        }
        var localDim = dimPane;
        var localDrawer = drawerPane;
        var callback = onClosed;
        dimPane = null;
        drawerPane = null;
        onClosed = null;
        drawerRows.clear();
        localDim.prefWidthProperty().unbind();
        localDim.prefHeightProperty().unbind();
        rootPane.getChildren().removeAll(localDim, localDrawer);

        if (callback != null) {
            callback.run();
        }
    }

    /**
     * Narrows the open drawer to tracks matching {@code query}, hiding sections with no match, and
     * remembers the query so subsequently opened drawers apply the same filter. An empty or
     * {@code null} query clears the filter.
     */
    public void applyQuery(String query) {
        // Lower-case here so the invariant holds for every caller, since AudioItemQueryMatcher
        // expects an already-lower-cased query.
        currentQuery = query == null ? "" : query.toLowerCase();
        applyQueryToRows();
    }

    /**
     * Removes a drawer still sliding out from a previous {@link #close()} and stops its animation, so
     * a fast reopen does not stack a second overlay on top of the one still animating away.
     */
    private void purgePendingClose() {
        if (closingTransition != null) {
            closingTransition.stop();
            closingTransition = null;
        }
        if (closingDimPane != null || closingDrawerPane != null) {
            rootPane.getChildren().removeAll(closingDimPane, closingDrawerPane);
            closingDimPane = null;
            closingDrawerPane = null;
        }
    }

    private void applyQueryToRows() {
        // Narrow with the same field set the cover grids filter by, so a section (album) is shown
        // only when it has a matching track, and shows only the matching tracks.
        Predicate<ObservableAudioItem> predicate = currentQuery.isEmpty()
                ? null
                : item -> AudioItemQueryMatcher.matches(item, currentQuery);
        for (var row : drawerRows) {
            row.filterTracks(predicate);
            boolean hasMatch = row.hasTracksMatching(predicate);
            row.setVisible(hasMatch);
            row.setManaged(hasMatch);
        }
    }

    /** Selects every track across all sections of the open drawer. No-op when no drawer is shown. */
    public void selectAll() {
        drawerRows.forEach(ArtistAlbumListRow::selectAllAudioItems);
    }

    /** Clears the track selection across all sections of the open drawer. No-op when no drawer is shown. */
    public void deselectAll() {
        drawerRows.forEach(ArtistAlbumListRow::deselectAllAudioItems);
    }

    /** @return the tracks selected across the open drawer's sections; empty when no drawer is shown. */
    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        var selected = FXCollections.<ObservableAudioItem>observableArrayList();
        drawerRows.forEach(row -> selected.addAll(row.selectedAudioItemsProperty()));
        return selected;
    }

    private void onResizeStart(MouseEvent event) {
        dragStartSceneX = event.getSceneX();
        dragStartWidth = currentWidth;
        event.consume();
    }

    /**
     * Widens the drawer as the handle is dragged left and narrows it when dragged right, clamped to
     * the resize bounds. The chosen width is remembered so it survives close/reopen.
     */
    private void onResizeDrag(MouseEvent event) {
        double max = maxWidth();
        double target = clamp(dragStartWidth + (dragStartSceneX - event.getSceneX()),
                Math.min(RESIZE_MIN_WIDTH, max), max);
        preferredWidth = target;
        applyWidth(target);
        event.consume();
    }

    /** Fixes the drawer to {@code width} by pinning its min/pref/max, and records it for the slide-out. */
    private void applyWidth(double width) {
        currentWidth = width;
        if (drawerPane != null) {
            drawerPane.setMinWidth(width);
            drawerPane.setPrefWidth(width);
            drawerPane.setMaxWidth(width);
        }
    }

    /** The maximum drawer width: 80% of the root pane, floored so it stays usable on small windows. */
    private double maxWidth() {
        return Math.max(rootPane.getWidth() * DRAWER_WIDTH_FRACTION, MAX_WIDTH_FLOOR);
    }

    /** The width to open (or re-clamp) at: the remembered width if any, clamped to the resize bounds. */
    private double resolveWidth() {
        double max = maxWidth();
        double base = preferredWidth < 0 ? max : preferredWidth;
        return clamp(base, Math.min(RESIZE_MIN_WIDTH, max), max);
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }
}
