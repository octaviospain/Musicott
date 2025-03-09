package net.transgressoft.musicott.view.custom.table;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.music.audio.AlbumView;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.musicott.view.custom.ApplicationImage;

import com.google.common.base.Joiner;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.fxmisc.easybind.EasyBind.map;
import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * @author Octavio Calleya
 */
public class ArtistListRow extends HBox {

    private static final String BASE_STYLE = "/css/base.css";
    private static final double COVER_SIZE = 130.0;

    private final Artist artist;
    private final AlbumView<ObservableAudioItem> albumView;
    private final ListProperty<ObservableAudioItem> selectedAudioItemsProperty;
    private final ObservableList<ObservableAudioItem> containedAudioItems;
    private final ListProperty<ObservableAudioItem> containedAudioItemsProperty;
    private final Comparator<ObservableAudioItem> trackComparator;
    private final SimpleAudioItemTableView audioItemsTableView;

    private ImageView coverImageView;
    private VBox albumInfoVBox;
    private Label genresLabel;
    private Label albumLabelLabel;
    private Label yearLabel;
    private Label relatedArtistsLabel;

    public ArtistListRow(Artist artist,
                         AlbumView<ObservableAudioItem> albumView,
                         ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty,
                         ReadOnlySetProperty<ObservablePlaylist> playlistsProperty,
                         StringProperty searchTextProperty,
                         ApplicationEventPublisher applicationEventPublisher) {
        super();
        this.artist = artist;
        this.albumView = albumView;
        trackComparator = audioItemComparator();
        containedAudioItems = FXCollections.observableArrayList(albumView.getAudioItems());
        containedAudioItems.sort(trackComparator);
        containedAudioItemsProperty = new SimpleListProperty<>(this, "contained tracks");
        containedAudioItemsProperty.bind(new SimpleObjectProperty<>(containedAudioItems));

        placeLeftVBox();
        placeRightVBox();
        setPrefWidth(USE_COMPUTED_SIZE);
        setPrefHeight(USE_COMPUTED_SIZE);
        getStylesheets().add(getClass().getResource(BASE_STYLE).toExternalForm());
        audioItemsTableView = new SimpleAudioItemTableView(selectedPlaylistProperty, playlistsProperty, searchTextProperty, applicationEventPublisher);
        audioItemsTableView.setItems(containedAudioItems);
        audioItemsTableView.sort();
        updateRelatedArtistsLabel();
        updateAlbumLabelLabel();
        checkArtistColumn();
        selectedAudioItemsProperty = new SimpleListProperty<>(this, "selected artist tracks");
        selectedAudioItemsProperty.bind(new SimpleObjectProperty<>(audioItemsTableView.getSelectionModel().getSelectedItems()));
    }

    private void placeLeftVBox() {
        coverImageView = new ImageView();
        coverImageView.setFitWidth(COVER_SIZE);
        coverImageView.setFitHeight(COVER_SIZE);
        updateTrackSetImage();
        var sizeLabel = new Label();
        sizeLabel.setId("sizeLabel");
        sizeLabel.textProperty().bind(map(containedAudioItemsProperty.sizeProperty(), this::getAlbumSizeString));
        yearLabel = new Label(getYearsString());
        yearLabel.setId("yearLabel");

        var underImageGridPane = new GridPane();
        var cc1 = new ColumnConstraints(COVER_SIZE / 2);
        var cc2 = new ColumnConstraints(COVER_SIZE / 2);
        underImageGridPane.getColumnConstraints().addAll(cc1, cc2);
        underImageGridPane.add(sizeLabel, 0, 0);
        underImageGridPane.add(yearLabel, 1, 0);
        GridPane.setHalignment(sizeLabel, HPos.LEFT);
        GridPane.setHalignment(yearLabel, HPos.RIGHT);

        var coverBorderPane = new BorderPane();
        coverBorderPane.setTop(coverImageView);
        coverBorderPane.setCenter(underImageGridPane);
        BorderPane.setMargin(underImageGridPane, new Insets(10, 0, 0, 0));
        VBox.setMargin(coverImageView, new Insets(0, 0, 10, 0));
        HBox.setMargin(coverBorderPane, new Insets(20, 20, 20, 20));
        getChildren().add(coverBorderPane);
    }

    private void placeRightVBox() {
        var albumTitleLabel = new Label(albumView.getAlbumName());
        albumTitleLabel.setId("albumTitleLabel");
        albumTitleLabel.setMaxWidth(480);
        relatedArtistsLabel = new Label();
        relatedArtistsLabel.setId("relatedArtistsLabel");
        relatedArtistsLabel.setWrapText(true);
        relatedArtistsLabel.setMaxWidth(480);
        relatedArtistsLabel.setPrefWidth(USE_COMPUTED_SIZE);
        genresLabel = new Label(getGenresString());
        genresLabel.setId("genresLabel");
        genresLabel.setWrapText(true);
        genresLabel.setMaxWidth(480);
        albumLabelLabel = new Label();
        albumLabelLabel.setId("albumLabelLabel");
        buildSimpleTableView();

        albumInfoVBox = new VBox(albumTitleLabel, genresLabel, audioItemsTableView);
        VBox.setVgrow(audioItemsTableView, Priority.ALWAYS);
        HBox.setHgrow(albumInfoVBox, Priority.SOMETIMES);
        HBox.setMargin(albumInfoVBox, new Insets(20, 20, 5, 0));
        getChildren().add(albumInfoVBox);
    }

    private String getGenresString() {
        var genres = containedAudioItems.stream().filter(entry -> ! entry.getGenre().name().isEmpty())
                .map(entry -> entry.getGenre().name())
                .collect(Collectors.toSet());
        return Joiner.on(", ").join(genres);
    }

    private String getYearsString() {
        var differentYears = containedAudioItems.stream().filter(track -> track.getAlbum().getYear() != 0)
                .map(track -> String.valueOf(track.getAlbum().getYear()))
                .collect(Collectors.toSet());
        return Joiner.on(", ").join(differentYears);
    }

    private String getAlbumSizeString(Number numberOfTracks) {
        var appendix = numberOfTracks.intValue() == 1 ? " track" : " tracks";
        return numberOfTracks + appendix;
    }

    private void updateTrackSetImage() {
        containedAudioItems.stream()
                .filter(track -> track.getCoverImageProperty().get().isPresent())
                .findAny()
                .ifPresentOrElse(track -> coverImageView.setImage(track.getCoverImageProperty().get().get()), () -> {
                    Image defaultCoverImage = ApplicationImage.DEFAULT_COVER.get();
                    coverImageView.setImage(defaultCoverImage);
                });
    }

    private void updateAlbumLabelLabel() {
        var differentLabels = containedAudioItems.stream().map(entry -> entry.getAlbum().getLabel().getName())
                .collect(Collectors.toSet());
        differentLabels.remove("");
        var labelString = Joiner.on(", ").join(differentLabels);
        albumLabelLabel.setText(labelString);
        if (labelString.isEmpty())
            albumInfoVBox.getChildren().remove(albumLabelLabel);
        else if (! albumInfoVBox.getChildren().contains(albumLabelLabel))
            albumInfoVBox.getChildren().add(albumInfoVBox.getChildren().size() - 2, albumLabelLabel);
    }

    private void updateRelatedArtistsLabel() {
        var relatedArtists = containedAudioItems.stream()
                .flatMap(t -> t.getArtistsInvolved().stream())
                .collect(Collectors.toCollection(HashSet::new));

        relatedArtists.remove(artist.getName());
        var relatedArtistsString = "with " + Joiner.on(", ").join(relatedArtists);
        relatedArtistsLabel.setText(relatedArtistsString);
        if (relatedArtists.isEmpty())
            albumInfoVBox.getChildren().remove(relatedArtistsLabel);
        else if (! albumInfoVBox.getChildren().contains(relatedArtistsLabel))
            albumInfoVBox.getChildren().add(1, relatedArtistsLabel);
    }

    private void buildSimpleTableView() {
        audioItemsTableView.trackNumberCol.setCellValueFactory(cellData -> listenAudioItemChangesAndSort(cellData.getValue()));

        var rows = map(containedAudioItemsProperty.sizeProperty(), s -> s.intValue() * 1.06);
        audioItemsTableView.prefHeightProperty().bind(audioItemsTableView.fixedCellSizeProperty().multiply(rows.getValue()));
        audioItemsTableView.minHeightProperty().bind(audioItemsTableView.prefHeightProperty());
        audioItemsTableView.maxHeightProperty().bind(audioItemsTableView.prefHeightProperty());
    }

    /**
     * Extracts some properties of the track in each row and listen for the changes to update the view
     *
     * @param audioItem The {@link ObservableAudioItem} in the row
     *
     * @return The {@link IntegerProperty} of the track number
     */
    private ReadOnlyIntegerProperty listenAudioItemChangesAndSort(ObservableAudioItem audioItem) {
        subscribe(audioItem.getTrackNumberProperty(), tn -> containedAudioItems.sort(trackComparator));
        subscribe(audioItem.getDiscNumberProperty(), dn -> containedAudioItems.sort(trackComparator));
        subscribe(audioItem.getGenreNameProperty(), g -> genresLabel.setText(getGenresString()));
        subscribe(audioItem.getAlbumProperty(), y -> {
            yearLabel.setText(getYearsString());
            updateAlbumLabelLabel();
        });
        subscribe(audioItem.getArtistsInvolvedProperty(), ai ->
                Platform.runLater(() -> {
                    updateRelatedArtistsLabel();
                    checkArtistColumn();
                }));
        //        subscribe(observableAudioItem.hasCoverProperty(), c -> updateTrackSetImage()); //TODO
        return audioItem.getTrackNumberProperty();
    }

    private Comparator<ObservableAudioItem> audioItemComparator() {
        return (te1, te2) -> {
            // TODO change this when implementing other tables for other discs in the same album
            int te1DiscNum = te1.getDiscNumber();
            int te2DiscNum = te2.getDiscNumber();
            int result = te1DiscNum - te2DiscNum;
            int te1TrackNum = te1.getTrackNumber();
            int te2TrackNum = te2.getTrackNumber();
            return result == 0 ? te1TrackNum - te2TrackNum : result;
        };
    }

    /**
     * Places the artist column only if there are more than 1 different artist on the columns,
     * or removes it from the table if there aren't or the only common artist is the same of this
     * showing {@code TrackSetAreaRow}
     */
    private void checkArtistColumn() {
        var commonColumnArtists = containedAudioItems.stream().map(ObservableAudioItem::getArtist).collect(Collectors.toSet());
        if (commonColumnArtists.isEmpty() || (commonColumnArtists.size() == 1 && commonColumnArtists.contains(artist)))
            audioItemsTableView.removeArtistColumn();
        else
            audioItemsTableView.placeArtistColumn();
    }

    public void selectAudioItem(ObservableAudioItem track) {
        audioItemsTableView.getSelectionModel().clearSelection();
        audioItemsTableView.getSelectionModel().select(track);
        var entryPos = audioItemsTableView.getSelectionModel().getSelectedIndex();
        audioItemsTableView.getSelectionModel().focus(entryPos);
    }

    public void selectAllAudioItems() {
        audioItemsTableView.getSelectionModel().selectAll();
    }

    public void deselectAllAudioItems() {
        audioItemsTableView.getSelectionModel().clearSelection();
    }

    public AlbumView<ObservableAudioItem> getAlbumView() {
        return albumView;
    }

    public ListProperty<ObservableAudioItem> selectedgetAudioItemsProperty() {
        return selectedAudioItemsProperty;
    }

    public ListProperty<ObservableAudioItem> containedgetAudioItemsProperty() {
        return containedAudioItemsProperty;
    }
}
