package net.transgressoft.musicott.view.custom.table;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.*;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fxmisc.easybind.Subscription;

import static org.fxmisc.easybind.EasyBind.map;
import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * A row in the artist albums list view that displays a single album (or a single disc of a
 * multi-disc album) together with its embedded track table. When {@code discNumber > 0} the row
 * renders a "Disc N" header label above the album title to distinguish disc sections within the
 * same album.
 *
 * @author Octavio Calleya
 */
@Component
@Scope("prototype")
public class ArtistAlbumListRow extends HBox {

    private static final String BASE_STYLE = "/css/base.css";
    private static final String SECONDARY_INFO_STYLE_CLASS = "album-info-secondary";
    private static final double COVER_SIZE = 130.0;

    private final Artist artist;
    private final AlbumTrackGroup albumSet;
    private final int discNumber;
    private final ListProperty<ObservableAudioItem> selectedAudioItemsProperty;
    private final ObservableList<ObservableAudioItem> containedAudioItems;
    private final FilteredList<ObservableAudioItem> filteredAudioItems;
    private final ListProperty<ObservableAudioItem> containedAudioItemsProperty;
    private final SimpleAudioItemTableView audioItemsTableView;

    private final List<Subscription> subscriptions = new ArrayList<>();

    private ImageView coverImageView;
    private VBox albumInfoVBox;
    private Label genresLabel;
    private Label albumLabelLabel;
    private Label yearLabel;
    private Label relatedArtistsLabel;

    /**
     * Creates a new album list row.
     *
     * @param artist             the artist owning this album row
     * @param albumSet           the group of audio items belonging to this album (or disc)
     * @param audioItemsTableView the embedded track table view
     * @param discNumber         the 1-based disc number for multi-disc albums; {@code 0} means
     *                           single-disc (no disc label rendered)
     */
    public ArtistAlbumListRow(Artist artist, AlbumTrackGroup albumSet, SimpleAudioItemTableView audioItemsTableView, int discNumber) {
        super();
        this.artist = artist;
        this.albumSet = albumSet;
        this.audioItemsTableView = audioItemsTableView;
        this.discNumber = discNumber;
        containedAudioItems = FXCollections.observableArrayList(albumSet.tracks());
        containedAudioItems.sort(this::audioItemComparator);
        filteredAudioItems = new FilteredList<>(containedAudioItems);
        containedAudioItemsProperty = new SimpleListProperty<>(this, "contained tracks");
        containedAudioItemsProperty.bind(new SimpleObjectProperty<>(containedAudioItems));

        placeLeftVBox();
        placeRightVBox();
        setPrefWidth(0);
        setMinWidth(0);
        setPrefHeight(USE_COMPUTED_SIZE);
        getStylesheets().add(getClass().getResource(BASE_STYLE).toExternalForm());
        audioItemsTableView.setItems(filteredAudioItems);
        audioItemsTableView.sort();
        setRelatedArtistsLabel();
        updateAlbumLabelLabel();
        setArtistColumn();
        selectedAudioItemsProperty = new SimpleListProperty<>(this, "selected artist tracks");
        selectedAudioItemsProperty.bind(new SimpleObjectProperty<>(audioItemsTableView.getSelectionModel().getSelectedItems()));
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) subscriptions.forEach(Subscription::unsubscribe);
        });
    }

    private void placeLeftVBox() {
        coverImageView = new ImageView();
        coverImageView.setId("coverImageView");
        coverImageView.setFitWidth(COVER_SIZE);
        coverImageView.setFitHeight(COVER_SIZE);
        updateAudioItemsImage();
        var sizeLabel = new Label();
        sizeLabel.setId("sizeLabel");
        // Share the secondary style with the year label so both carry the same font size and
        // padding; otherwise the two sit on different baselines under the cover (misaligned).
        sizeLabel.getStyleClass().add(SECONDARY_INFO_STYLE_CLASS);
        sizeLabel.textProperty().bind(map(containedAudioItemsProperty.sizeProperty(), this::getAlbumSizeString));
        yearLabel = new Label(buildYearsString());
        yearLabel.setId("yearLabel");
        yearLabel.getStyleClass().add(SECONDARY_INFO_STYLE_CLASS);

        var underImageGridPane = new GridPane();
        var cc1 = new ColumnConstraints(COVER_SIZE / 2);
        var cc2 = new ColumnConstraints(COVER_SIZE / 2);
        underImageGridPane.getColumnConstraints().addAll(cc1, cc2);
        underImageGridPane.add(sizeLabel, 0, 0);
        underImageGridPane.add(yearLabel, 1, 0);
        GridPane.setHalignment(sizeLabel, HPos.LEFT);
        GridPane.setHalignment(yearLabel, HPos.RIGHT);
        // Pin both to the same vertical line so the track count and year read as one aligned row.
        GridPane.setValignment(sizeLabel, VPos.CENTER);
        GridPane.setValignment(yearLabel, VPos.CENTER);

        var coverBorderPane = new BorderPane();
        coverBorderPane.setTop(coverImageView);
        coverBorderPane.setCenter(underImageGridPane);
        BorderPane.setMargin(underImageGridPane, new Insets(10, 0, 0, 0));
        VBox.setMargin(coverImageView, new Insets(0, 0, 10, 0));
        HBox.setMargin(coverBorderPane, new Insets(20, 20, 20, 20));
        getChildren().add(coverBorderPane);
    }

    private void placeRightVBox() {
        var albumTitleLabel = new Label(albumSet.getAlbumName());
        albumTitleLabel.setId("albumTitleLabel");
        // No fixed width cap: the title takes as much horizontal space as it needs, up to the row
        // width (the drawer can be far wider than the artist view, where a 480px cap truncated it).
        relatedArtistsLabel = new Label();
        relatedArtistsLabel.setId("relatedArtistsLabel");
        relatedArtistsLabel.getStyleClass().add(SECONDARY_INFO_STYLE_CLASS);
        // Single line that truncates with an ellipsis rather than wrapping — squeezing the drawer
        // must not grow the row vertically. No fixed width cap so it uses the full available width.
        relatedArtistsLabel.setWrapText(false);
        genresLabel = new Label(buildGenresString());
        genresLabel.setId("genresLabel");
        genresLabel.setWrapText(true);
        genresLabel.setMaxWidth(480);
        albumLabelLabel = new Label();
        albumLabelLabel.setId("albumLabelLabel");
        albumLabelLabel.getStyleClass().add(SECONDARY_INFO_STYLE_CLASS);
        buildSimpleTableView();

        albumInfoVBox = new VBox(albumTitleLabel, genresLabel, audioItemsTableView);
        if (discNumber > 0) {
            var discLabel = new Label("Disc " + discNumber);
            discLabel.setId("discLabel");
            discLabel.getStyleClass().add(SECONDARY_INFO_STYLE_CLASS);
            albumInfoVBox.getChildren().add(1, discLabel);
        }
        VBox.setVgrow(audioItemsTableView, Priority.ALWAYS);
        HBox.setHgrow(albumInfoVBox, Priority.SOMETIMES);
        HBox.setMargin(albumInfoVBox, new Insets(20, 20, 5, 0));
        getChildren().add(albumInfoVBox);
    }

    private String buildGenresString() {
        return GenreExtensionsKt.joinGenres(containedAudioItems.stream()
                .flatMap(observableAudioItem -> observableAudioItem.getGenres().stream())
                .collect(Collectors.toSet()));
    }

    @SuppressWarnings("java:S2589")
    private String buildYearsString() {
        return containedAudioItems.stream()
                .filter(track -> track.getAlbum() != null && track.getAlbum().getYear() != null)
                .map(track -> track.getAlbum().getYear().intValue())
                .min(Integer::compareTo)
                .map(String::valueOf)
                .orElse("");
    }

    private String getAlbumSizeString(Number numberOfTracks) {
        var appendix = numberOfTracks.intValue() == 1 ? " track" : " tracks";
        return numberOfTracks + appendix;
    }

    private void updateAudioItemsImage() {
        containedAudioItems.stream()
                .filter(track -> track.getCoverImageProperty().get().isPresent())
                .findAny()
                .ifPresentOrElse(track -> coverImageView.setImage(track.getCoverImageProperty().get().get()), () -> {
                    Image defaultCoverImage = ApplicationImage.DEFAULT_COVER.get();
                    coverImageView.setImage(defaultCoverImage);
                });
    }

    // Defensive null guards — tracks imported without a label tag carry null Album.getLabel()
    // or null Label.getName(); one untagged track must not NPE the entire row render.
    @SuppressWarnings("java:S2589")
    private void updateAlbumLabelLabel() {
        var labelString = containedAudioItems.stream()
                .filter(entry -> entry.getAlbum() != null
                        && entry.getAlbum().getLabel() != null
                        && entry.getAlbum().getLabel().getName() != null
                        && !entry.getAlbum().getLabel().getName().isEmpty())
                .map(entry -> entry.getAlbum().getLabel().getName())
                .distinct()
                .findFirst()
                .orElse("");

        albumLabelLabel.setText(labelString);
        if (labelString.isEmpty())
            albumInfoVBox.getChildren().remove(albumLabelLabel);
        else if (! albumInfoVBox.getChildren().contains(albumLabelLabel))
            albumInfoVBox.getChildren().add(albumInfoVBox.getChildren().size() - 2, albumLabelLabel);
    }

    private void setRelatedArtistsLabel() {
        var relatedArtistNames = containedAudioItems.stream()
                .flatMap(t -> t.getArtistsInvolved().stream().map(Artist::getName))
                .filter(artistName -> ! artistName.equalsIgnoreCase(artist.getName()))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        var relatedArtistsString = relatedArtistNames.isEmpty() ? "" : "with " + relatedArtistNames;

        relatedArtistsLabel.setText(relatedArtistsString);
        if (relatedArtistsString.isEmpty())
            albumInfoVBox.getChildren().remove(relatedArtistsLabel);
        else if (! albumInfoVBox.getChildren().contains(relatedArtistsLabel))
            albumInfoVBox.getChildren().add(1, relatedArtistsLabel);
    }

    private void buildSimpleTableView() {
        audioItemsTableView.trackNumberCol.setCellValueFactory(cellData -> listenAudioItemChangesAndSort(cellData.getValue()));

        // Bind the table height to the FILTERED row count so the row collapses (or expands)
        // as the search query narrows the visible tracks instead of leaving empty space behind.
        var rowCount = Bindings.size(filteredAudioItems);
        audioItemsTableView.prefHeightProperty().bind(
                audioItemsTableView.fixedCellSizeProperty().multiply(Bindings.createDoubleBinding(
                        () -> rowCount.intValue() * 1.06,
                        rowCount)));
        audioItemsTableView.minHeightProperty().bind(audioItemsTableView.prefHeightProperty());
        audioItemsTableView.maxHeightProperty().bind(audioItemsTableView.prefHeightProperty());
    }

    /**
     * Applies the global search query to the embedded track table. An empty or null query clears
     * the filter; otherwise only tracks whose title, artist, album, album-artist, or comments
     * contain the query (case-insensitive) remain visible.
     */
    public void filterTracksByQuery(String query) {
        if (query == null || query.isEmpty()) {
            filteredAudioItems.setPredicate(null);
        } else {
            String q = query.toLowerCase();
            filteredAudioItems.setPredicate(item -> trackMatchesQuery(item, q));
        }
    }

    /** True when at least one track in this album row matches the query (or the query is empty). */
    public boolean hasTracksMatching(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String q = query.toLowerCase();
        return containedAudioItems.stream().anyMatch(item -> trackMatchesQuery(item, q));
    }

    /**
     * Narrows the embedded track table to tracks matching {@code predicate}. A {@code null} predicate
     * clears the filter. Lets a caller supply its own match rule (e.g. the cover-grid views' shared
     * matcher) instead of this row's default query matching.
     */
    public void filterTracks(Predicate<ObservableAudioItem> predicate) {
        filteredAudioItems.setPredicate(predicate == null ? null : predicate::test);
    }

    /** True when at least one track matches {@code predicate} (or the predicate is {@code null}). */
    public boolean hasTracksMatching(Predicate<ObservableAudioItem> predicate) {
        return predicate == null || containedAudioItems.stream().anyMatch(predicate);
    }

    // Defensive null guards on title / artist / album metadata: imported tracks can ship with
    // null fields (partial catalogs), and one malformed item must not NPE the whole query filter.
    // Sonar's flow analysis trusts the music-commons API's nominal non-null types and flags
    // each guard as gratuitous; in practice we hit them for real-world libraries.
    @SuppressWarnings("java:S2589")
    private static boolean trackMatchesQuery(ObservableAudioItem item, String lowerCaseQuery) {
        if (item.getTitle() != null && item.getTitle().toLowerCase().contains(lowerCaseQuery)) {
            return true;
        }
        var artist = item.getArtist();
        if (artist != null && artist.getName() != null
                && artist.getName().toLowerCase().contains(lowerCaseQuery)) {
            return true;
        }
        if (item.getArtistsInvolved() != null && item.getArtistsInvolved().stream()
                .map(Artist::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> name.toLowerCase().contains(lowerCaseQuery))) {
            return true;
        }
        if (AudioItemTableViewBase.albumContainsQuery(item.getAlbum(), lowerCaseQuery)) {
            return true;
        }
        return item.getComments() != null && item.getComments().toLowerCase().contains(lowerCaseQuery);
    }

    /**
     * Extracts some properties of the audioItem in each row and listen for the changes to update the view
     *
     * @param audioItem The {@link ObservableAudioItem} in the row
     *
     * @return The {@link IntegerProperty} of the audioItem number
     */
    private ReadOnlyIntegerProperty listenAudioItemChangesAndSort(ObservableAudioItem audioItem) {
        subscriptions.add(subscribe(audioItem.getTrackNumberProperty(), _ -> containedAudioItems.sort(this::audioItemComparator)));
        subscriptions.add(subscribe(audioItem.getDiscNumberProperty(), _ -> containedAudioItems.sort(this::audioItemComparator)));
        subscriptions.add(subscribe(audioItem.getGenresProperty(), _ -> genresLabel.setText(buildGenresString())));
        subscriptions.add(subscribe(audioItem.getAlbumProperty(), _ -> {
            yearLabel.setText(buildYearsString());
            updateAlbumLabelLabel();
        }));
        subscriptions.add(subscribe(audioItem.getArtistsInvolvedProperty(), _ ->
                Platform.runLater(() -> {
                    setRelatedArtistsLabel();
                    setArtistColumn();
                })));
        subscriptions.add(subscribe(audioItem.getCoverImageProperty(), _ -> Platform.runLater(this::updateAudioItemsImage)));
        return audioItem.getTrackNumberProperty();
    }

    private Integer audioItemComparator(ObservableAudioItem audioItem1, ObservableAudioItem audioItem2) {
        int ai1DiscNum = audioItem1.getDiscNumber() == null ? 0 : audioItem1.getDiscNumber().intValue();
        int ai2DiscNum = audioItem2.getDiscNumber() == null ? 0 : audioItem2.getDiscNumber().intValue();
        int result = ai1DiscNum - ai2DiscNum;
        int ai1TrackNum = audioItem1.getTrackNumber() == null ? 0 : audioItem1.getTrackNumber().intValue();
        int ai2TrackNum = audioItem2.getTrackNumber() == null ? 0 : audioItem2.getTrackNumber().intValue();
        return result == 0 ? ai1TrackNum - ai2TrackNum : result;
    }

    /**
     * Places the artist column only if there are more than 1 different artist on the columns,
     * or removes it from the table if there aren't or the only common artist is the same of this
     * showing {@code ArtistAlbumListRow}
     */
    private void setArtistColumn() {
        var commonColumnArtists = containedAudioItems.stream().map(ObservableAudioItem::getArtist).collect(Collectors.toSet());
        if (commonColumnArtists.isEmpty() || (commonColumnArtists.size() == 1 && commonColumnArtists.contains(artist)))
            audioItemsTableView.removeArtistColumn();
        else
            audioItemsTableView.placeArtistColumn();
    }

    public void selectAudioItem(ObservableAudioItem audioItem) {
        audioItemsTableView.getSelectionModel().clearSelection();
        audioItemsTableView.getSelectionModel().select(audioItem);
        var index = audioItemsTableView.getSelectionModel().getSelectedIndex();
        audioItemsTableView.getSelectionModel().focus(index);
    }

    public void selectAllAudioItems() {
        audioItemsTableView.getSelectionModel().selectAll();
    }

    public void deselectAllAudioItems() {
        audioItemsTableView.getSelectionModel().clearSelection();
    }

    public AlbumTrackGroup getAlbumSet() {
        return albumSet;
    }

    public Artist getArtist() {
        return artist;
    }

    public ListProperty<ObservableAudioItem> selectedAudioItemsProperty() {
        return selectedAudioItemsProperty;
    }

    public ListProperty<ObservableAudioItem> containedAudioItemsProperty() {
        return containedAudioItemsProperty;
    }
}
