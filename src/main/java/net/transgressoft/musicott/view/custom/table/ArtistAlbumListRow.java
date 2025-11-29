package net.transgressoft.musicott.view.custom.table;

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
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.*;
import net.transgressoft.musicott.view.custom.ApplicationImage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static org.fxmisc.easybind.EasyBind.map;
import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * @author Octavio Calleya
 */
@Component
@Scope("prototype")
public class ArtistAlbumListRow extends HBox {

    private static final String BASE_STYLE = "/css/base.css";
    private static final double COVER_SIZE = 130.0;

    private final Artist artist;
    private final AlbumSet<ObservableAudioItem> albumSet;
    private final ListProperty<ObservableAudioItem> selectedAudioItemsProperty;
    private final ObservableList<ObservableAudioItem> containedAudioItems;
    private final ListProperty<ObservableAudioItem> containedAudioItemsProperty;
    private final SimpleAudioItemTableView audioItemsTableView;

    private ImageView coverImageView;
    private VBox albumInfoVBox;
    private Label genresLabel;
    private Label albumLabelLabel;
    private Label yearLabel;
    private Label relatedArtistsLabel;

    public ArtistAlbumListRow(Artist artist, AlbumSet<ObservableAudioItem> albumSet, SimpleAudioItemTableView audioItemsTableView) {
        super();
        this.artist = artist;
        this.albumSet = albumSet;
        this.audioItemsTableView = audioItemsTableView;
        containedAudioItems = FXCollections.observableArrayList(albumSet);
        containedAudioItems.sort(this::audioItemComparator);
        containedAudioItemsProperty = new SimpleListProperty<>(this, "contained tracks");
        containedAudioItemsProperty.bind(new SimpleObjectProperty<>(containedAudioItems));

        placeLeftVBox();
        placeRightVBox();
        setPrefWidth(USE_COMPUTED_SIZE);
        setPrefHeight(USE_COMPUTED_SIZE);
        getStylesheets().add(getClass().getResource(BASE_STYLE).toExternalForm());
        audioItemsTableView.setItems(containedAudioItems);
        audioItemsTableView.sort();
        setRelatedArtistsLabel();
        updateAlbumLabelLabel();
        setArtistColumn();
        selectedAudioItemsProperty = new SimpleListProperty<>(this, "selected artist tracks");
        selectedAudioItemsProperty.bind(new SimpleObjectProperty<>(audioItemsTableView.getSelectionModel().getSelectedItems()));
    }

    private void placeLeftVBox() {
        coverImageView = new ImageView();
        coverImageView.setFitWidth(COVER_SIZE);
        coverImageView.setFitHeight(COVER_SIZE);
        updateAudioItemsImage();
        var sizeLabel = new Label();
        sizeLabel.setId("sizeLabel");
        sizeLabel.textProperty().bind(map(containedAudioItemsProperty.sizeProperty(), this::getAlbumSizeString));
        yearLabel = new Label(buildYearsString());
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
        var albumTitleLabel = new Label(albumSet.getAlbumName());
        albumTitleLabel.setId("albumTitleLabel");
        albumTitleLabel.setMaxWidth(480);
        relatedArtistsLabel = new Label();
        relatedArtistsLabel.setId("relatedArtistsLabel");
        relatedArtistsLabel.setWrapText(true);
        relatedArtistsLabel.setMaxWidth(480);
        relatedArtistsLabel.setPrefWidth(USE_COMPUTED_SIZE);
        genresLabel = new Label(buildGenresString());
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

    private String buildGenresString() {
        return containedAudioItems.stream()
                .filter(audioItem -> ! Genre.UNDEFINED.equals(audioItem.getGenre()))
                .map(observableAudioItem -> observableAudioItem.getGenre().capitalize())
                .collect(Collectors.joining(", "));
    }

    private String buildYearsString() {
        return containedAudioItems.stream()
                .filter(track -> track.getAlbum().getYear() != null)
                .map(track -> String.valueOf(track.getAlbum().getYear()))
                .sorted()
                .collect(Collectors.joining(", "));
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

    private void updateAlbumLabelLabel() {
        var labelString = containedAudioItems.stream()
                .filter(entry -> ! entry.getAlbum().getLabel().getName().isEmpty())
                .map(entry -> entry.getAlbum().getLabel().getName())
                .collect(Collectors.joining(", "));

        albumLabelLabel.setText(labelString);
        if (labelString.isEmpty())
            albumInfoVBox.getChildren().remove(albumLabelLabel);
        else if (! albumInfoVBox.getChildren().contains(albumLabelLabel))
            albumInfoVBox.getChildren().add(albumInfoVBox.getChildren().size() - 2, albumLabelLabel);
    }

    private void setRelatedArtistsLabel() {
        var relatedArtistsString = containedAudioItems.stream()
                .flatMap(t -> t.getArtistsInvolved().stream().map(Artist::getName))
                .filter(artistName -> ! artistName.equalsIgnoreCase(artist.getName()))
                .collect(Collectors.joining(", ", "with ", ""));

        relatedArtistsLabel.setText(relatedArtistsString);
        if (relatedArtistsString.isEmpty())
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
     * Extracts some properties of the audioItem in each row and listen for the changes to update the view
     *
     * @param audioItem The {@link ObservableAudioItem} in the row
     *
     * @return The {@link IntegerProperty} of the audioItem number
     */
    private ReadOnlyIntegerProperty listenAudioItemChangesAndSort(ObservableAudioItem audioItem) {
        subscribe(audioItem.getTrackNumberProperty(), tn -> containedAudioItems.sort(this::audioItemComparator));
        subscribe(audioItem.getDiscNumberProperty(), dn -> containedAudioItems.sort(this::audioItemComparator));
        subscribe(audioItem.getGenreProperty(), g -> genresLabel.setText(buildGenresString()));
        subscribe(audioItem.getAlbumProperty(), y -> {
            yearLabel.setText(buildYearsString());
            updateAlbumLabelLabel();
        });
        subscribe(audioItem.getArtistsInvolvedProperty(), ai ->
                Platform.runLater(() -> {
                    setRelatedArtistsLabel();
                    setArtistColumn();
                }));
        // TODO subscribe to changes on the audioItem cover property
        //        subscribe(observableAudioItem.hasCoverProperty(), c -> updateTrackSetImage());
        return audioItem.getTrackNumberProperty();
    }

    private Integer audioItemComparator(ObservableAudioItem audioItem1, ObservableAudioItem audioItem2) {
        // TODO Improve for the case when an album has more than 1 disc, showing them in different tables
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

    public AlbumSet<ObservableAudioItem> getAlbumSet() {
        return albumSet;
    }

    public ListProperty<ObservableAudioItem> selectedAudioItemsProperty() {
        return selectedAudioItemsProperty;
    }

    public ListProperty<ObservableAudioItem> containedAudioItemsProperty() {
        return containedAudioItemsProperty;
    }
}
