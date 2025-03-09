package net.transgressoft.musicott.view.custom.table;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.player.JavaFxPlayer;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.music.audio.Album;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.musicott.events.*;
import net.transgressoft.musicott.view.custom.ApplicationImage;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import org.apache.commons.io.FileUtils;
import org.fxmisc.easybind.EasyBind;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.fxmisc.easybind.EasyBind.subscribe;

/**
 * @author Octavio Calleya
 */
public abstract class AudioItemTableViewBase extends TableView<ObservableAudioItem> {

    public static final DataFormat TRACKS_DATA_FORMAT = new DataFormat("application/x-java-tracks-id");

    private static final String TRACK_TABLE_BASE_STYLE = "/css/tracktable.css";

    private static final Image DRAGBOARD_IMAGE = ApplicationImage.DRAGBOARD_ICON.get();

    protected static final String CENTER_RIGHT_ALIGN = "-fx-alignment: CENTER-RIGHT";
    protected static final String CENTER_LEFT_ALIGN = "-fx-alignment: CENTER-LEFT";
    protected static final String CENTER_ALIGN = "-fx-alignment: CENTER";

    private final ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty;
    private final ReadOnlySetProperty<ObservablePlaylist> playlistsProperty;
    private final ApplicationEventPublisher applicationEventPublisher;

    protected TableColumn<ObservableAudioItem, String> nameCol;
    protected TableColumn<ObservableAudioItem, Artist> artistCol;
    protected TableColumn<ObservableAudioItem, Album> albumCol;
    protected TableColumn<ObservableAudioItem, String> genreCol;
    protected TableColumn<ObservableAudioItem, String> commentsCol;
    protected TableColumn<ObservableAudioItem, Album> albumArtistCol;
    protected TableColumn<ObservableAudioItem, Album> labelCol;
    protected TableColumn<ObservableAudioItem, Number> sizeCol;
    protected TableColumn<ObservableAudioItem, Album> yearCol;
    protected TableColumn<ObservableAudioItem, Number> bitRateCol;
    protected TableColumn<ObservableAudioItem, Number> playCountCol;
    protected TableColumn<ObservableAudioItem, Number> discNumberCol;
    protected TableColumn<ObservableAudioItem, Number> bpmCol;
    protected TableColumn<ObservableAudioItem, Number> trackNumberCol;
    protected TableColumn<ObservableAudioItem, LocalDateTime> dateModifiedCol;
    protected TableColumn<ObservableAudioItem, LocalDateTime> dateAddedCol;
    protected TableColumn<ObservableAudioItem, Duration> totalTimeCol;

    protected AudioItemTableViewBase(ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty, StringProperty searchTextProperty,
                                     ReadOnlySetProperty<ObservablePlaylist> playlistsProperty, ApplicationEventPublisher applicationEventPublisher) {
        super();
        this.selectedPlaylistProperty = selectedPlaylistProperty;
        this.playlistsProperty = playlistsProperty;
        this.applicationEventPublisher = applicationEventPublisher;
        initColumns();

        setContextMenu(new AudioItemTableViewContextMenu());
        addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedEventHandler());
        addEventHandler(KeyEvent.KEY_PRESSED, keyPressedEventHandler());
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        getStylesheets().add(getClass().getResource(TRACK_TABLE_BASE_STYLE).toExternalForm());
        setRowFactory(tableview -> new AudioItemTableRow());
        setItems(getAudioItemListBoundToSearchField(searchTextProperty));
    }

    private void initColumns() {
        nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cellData -> cellData.getValue().getTitleProperty());
        nameCol.setPrefWidth(170);

        artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(cellData -> cellData.getValue().getArtistProperty());
        artistCol.setCellFactory(ArtistNameTableCell::new);
        artistCol.setPrefWidth(170);

        albumCol = new TableColumn<>("Album");
        albumCol.setCellValueFactory(cellData -> cellData.getValue().getAlbumProperty());
        albumCol.setCellFactory(AlbumNameTableCell::new);
        albumCol.setPrefWidth(170);

        genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(cellData -> cellData.getValue().getGenreNameProperty());
        genreCol.setPrefWidth(120);

        commentsCol = new TableColumn<>("Comments");
        commentsCol.setCellValueFactory(cellData -> cellData.getValue().getCommentsProperty());
        commentsCol.setPrefWidth(150);

        albumArtistCol = new TableColumn<>("Album Artist");
        albumArtistCol.setCellValueFactory(cellData -> cellData.getValue().getAlbumProperty());
        albumArtistCol.setCellFactory(AlbumArtistTableCell::new);
        albumArtistCol.setPrefWidth(100);

        labelCol = new TableColumn<>("Label");
        labelCol.setCellValueFactory(cellData -> cellData.getValue().getAlbumProperty());
        labelCol.setCellFactory(AlbumLabelNameTableCell::new);
        labelCol.setPrefWidth(120);

        dateModifiedCol = new TableColumn<>("Modified");
        dateModifiedCol.setCellValueFactory(cellData -> cellData.getValue().getLastDateModifiedProperty());
        dateModifiedCol.setCellFactory(DateTimeTableCell::new);
        dateModifiedCol.setPrefWidth(110);
        dateModifiedCol.setStyle(CENTER_RIGHT_ALIGN);

        dateAddedCol = new TableColumn<>("Added");
        dateAddedCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDateOfCreation()));
        dateAddedCol.setCellFactory(DateTimeTableCell::new);
        dateAddedCol.setPrefWidth(110);
        dateAddedCol.setStyle(CENTER_RIGHT_ALIGN);
        dateAddedCol.setSortType(TableColumn.SortType.DESCENDING);

        sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(cellData -> new SimpleLongProperty(cellData.getValue().getLength()));
        sizeCol.setCellFactory(ByteSizeTableCell::new);
        sizeCol.setPrefWidth(64);
        sizeCol.setStyle(CENTER_RIGHT_ALIGN);

        totalTimeCol = new TableColumn<>("Duration");
        totalTimeCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getDuration()));
        totalTimeCol.setCellFactory(DurationTableCell::new);
        totalTimeCol.setPrefWidth(60);
        totalTimeCol.setMinWidth(60);   // TODO needed ?
        totalTimeCol.setMaxWidth(60);
        totalTimeCol.setStyle(CENTER_RIGHT_ALIGN);

        yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(cellData -> cellData.getValue().getAlbumProperty());
        yearCol.setCellFactory(AlbumYearTableCell::new);
        yearCol.setPrefWidth(60);
        yearCol.setStyle(CENTER_RIGHT_ALIGN);

        playCountCol = new TableColumn<>("Plays");
        playCountCol.setPrefWidth(50);
        playCountCol.setCellValueFactory(cellData -> cellData.getValue().getPlayCountProperty());
        playCountCol.setStyle(CENTER_RIGHT_ALIGN);

        discNumberCol = new TableColumn<>("Disc Num");
        discNumberCol.setCellValueFactory(cellData -> cellData.getValue().getDiscNumberProperty());
        discNumberCol.setCellFactory(NumericTableCell::new);
        discNumberCol.setPrefWidth(45);
        discNumberCol.setStyle(CENTER_RIGHT_ALIGN);

        trackNumberCol = new TableColumn<>("Track Num");
        trackNumberCol.setCellValueFactory(cellData -> cellData.getValue().getTrackNumberProperty());
        trackNumberCol.setCellFactory(NumericTableCell::new);
        trackNumberCol.setPrefWidth(45);
        trackNumberCol.setStyle(CENTER_RIGHT_ALIGN);

        bitRateCol = new TableColumn<>("BitRate");
        bitRateCol.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getBitRate()));
        bitRateCol.setCellFactory(BitRateTableCell::new);
        bitRateCol.setPrefWidth(60);
        bitRateCol.setStyle(CENTER_RIGHT_ALIGN);

        bpmCol = new TableColumn<>("BPM");
        bpmCol.setCellValueFactory(cellData -> cellData.getValue().getBpmProperty());
        bpmCol.setCellFactory(NumericTableCell::new);
        bpmCol.setStyle(CENTER_RIGHT_ALIGN);
        bpmCol.setPrefWidth(60);
    }

    private EventHandler<MouseEvent> mouseClickedEventHandler() {
        return event -> {
            if (event.getButton() == MouseButton.SECONDARY)
                getContextMenu().show(this, event.getScreenX(), event.getScreenY());
            else if (event.getButton() == MouseButton.PRIMARY && getContextMenu().isShowing())
                getContextMenu().hide();
        };
    }

    private EventHandler<KeyEvent> keyPressedEventHandler() {
        return event -> {
            if (event.getCode() == KeyCode.ENTER) {
                ObservableList<ObservableAudioItem> selection = getSelectionModel().getSelectedItems();
                if (selection != null && ! selection.isEmpty())
                    applicationEventPublisher.publishEvent(new PlayItemEvent(selection, event.getSource()));
            } else if (event.getCode() == KeyCode.SPACE)
                applicationEventPublisher.publishEvent(new PauseEvent(event.getSource()));
        };
    }

    /**
     * Binds the text typed on the search text field to a filtered subset of items shown on the table
     */
    private SortedList<ObservableAudioItem> getAudioItemListBoundToSearchField(StringProperty searchTextProperty) {
        ObservableList<ObservableAudioItem> audioItems = FXCollections.observableArrayList();
        var filteredAudioItems = new FilteredList<>(audioItems, t -> true);

        subscribe(searchTextProperty, query -> filteredAudioItems.setPredicate(filterAudioItemPredicate(query)));

        var sortedAudioItems = new SortedList<>(filteredAudioItems);
        sortedAudioItems.comparatorProperty().bind(comparatorProperty());
        return sortedAudioItems;
    }

    /**
     * Returns a {@link Predicate} that evaluates the match of a given {@code String} to a given {@link ObservableAudioItem}
     *
     * @param query The {@code String} to match against the audio item
     *
     * @return The {@code Predicate}
     */
    private Predicate<ObservableAudioItem> filterAudioItemPredicate(String query) {
        return audioItem -> {
            var result = query == null || query.isEmpty();
            if (! result)
                result = audioItemContainsQuery(audioItem, query.toLowerCase());
            return result;
        };
    }

    private boolean audioItemContainsQuery(ObservableAudioItem audioItem, String query) {
        return audioItem.getArtist().getName().toLowerCase().contains(query) ||
            audioItem.getAlbum().getName().toLowerCase().contains(query) ||
            audioItem.getAlbum().getAlbumArtist().getName().toLowerCase().contains(query) ||
            audioItem.getTitle().toLowerCase().contains(query);
    }

    public void selectFocusAndScroll(ObservableAudioItem audioItem) {
        getSelectionModel().clearSelection();
        getSelectionModel().select(audioItem);
        scrollTo(audioItem);
        var entryPos = getSelectionModel().getSelectedIndex();
        getSelectionModel().focus(entryPos);
    }

    private class AudioItemTableRow extends TableRow<ObservableAudioItem> {

        private final PseudoClass notInDiskRow = PseudoClass.getPseudoClass("notInDisk");
        private final PseudoClass unplayableRow = PseudoClass.getPseudoClass("unplayable");

        public AudioItemTableRow() {
            setOnMouseClicked(this::playAudioItemOnMouseClickedHandler);
            setOnDragDetected(this::onDragDetectedMovingAudioItemHandler);
            EasyBind.subscribe(hoverProperty(), onAudioItemHoveredHandler());
            EasyBind.subscribe(itemProperty(), onAudioItemChangedHandler());
        }

        private void playAudioItemOnMouseClickedHandler(MouseEvent event) {
            if (! isEmpty() && getItem() != null && event.getClickCount() == 2)
                applicationEventPublisher.publishEvent(new PlayItemEvent(Collections.singletonList(getItem()), event.getSource()));
        }

        private void onDragDetectedMovingAudioItemHandler(MouseEvent event) {
            if (! isEmpty()) {
                var dragBoard = startDragAndDrop(TransferMode.COPY);
                dragBoard.setDragView(DRAGBOARD_IMAGE);

                var selection = getSelectionModel().getSelectedItems();
                var audioItemSelectionIds = selection.stream().map(ObservableAudioItem::getId).collect(Collectors.toList());
                var clipboardContent = new ClipboardContent();
                clipboardContent.put(TRACKS_DATA_FORMAT, audioItemSelectionIds);
                dragBoard.setContent(clipboardContent);
                event.consume();
            }
        }

        private Consumer<Boolean> onAudioItemHoveredHandler() {
            return newHovered -> {
                if (Boolean.TRUE.equals(newHovered) && getItem() != null)
                    applicationEventPublisher.publishEvent(new AudioItemHoveredEvent(getItem(), this));
            };
        }

        private Consumer<ObservableAudioItem> onAudioItemChangedHandler() {
            return item -> {
                if (item != null) {
                    pseudoClassStateChanged(unplayableRow, ! JavaFxPlayer.Companion.isPlayable(getItem()));
                    pseudoClassStateChanged(notInDiskRow, ! item.getPath().toFile().exists());
                } else {
                    pseudoClassStateChanged(notInDiskRow, false);
                    pseudoClassStateChanged(unplayableRow, false);
                }
            };
        }
    }

    private class AudioItemTableViewContextMenu extends ContextMenu {

        private final Menu addToPlaylistMenu;
        private final MenuItem deleteFromPlaylistMenuItem;
        private final List<MenuItem> playlistsInMenu = new ArrayList<>();

        public AudioItemTableViewContextMenu() {
            super();
            addToPlaylistMenu = new Menu("Add to playlist");

            MenuItem playMenuItem = new MenuItem("Play");
            playMenuItem.setOnAction(event -> {
                ObservableList<ObservableAudioItem> selectedAudioItems = getSelectionModel().getSelectedItems();
                if (selectedAudioItems != null && !selectedAudioItems.isEmpty()) {
                    applicationEventPublisher.publishEvent(new PlayItemEvent(selectedAudioItems, this));
                }
            });

            MenuItem editMenuItem = new MenuItem("Edit");
            editMenuItem.setOnAction(event -> {
                ObservableList<ObservableAudioItem> selectedAudioItems = getSelectionModel().getSelectedItems();
                if (! selectedAudioItems.isEmpty()) {
                    applicationEventPublisher.publishEvent(new OpenAudioItemEditorView(new HashSet<>(selectedAudioItems), this));
                }
            });

            MenuItem deleteMenuItem = new MenuItem("Delete");
            deleteMenuItem.setOnAction(event -> {
                ObservableList<ObservableAudioItem> selectedAudioItems = getSelectionModel().getSelectedItems();
                if (selectedAudioItems != null && !selectedAudioItems.isEmpty())
                    applicationEventPublisher.publishEvent(new DeleteAudioItemsEvent(new HashSet<>(selectedAudioItems), this));
            });

            MenuItem addToQueueMenuItem = new MenuItem("Add to play queue");
            addToQueueMenuItem.setOnAction(event -> {
                ObservableList<ObservableAudioItem> selectedAudioItems = getSelectionModel().getSelectedItems();
                if (selectedAudioItems != null && !selectedAudioItems.isEmpty())
                    applicationEventPublisher.publishEvent(new AddToPlayQueueEvent(selectedAudioItems, this));
            });

            deleteFromPlaylistMenuItem = new MenuItem("Delete from playlist");
            deleteFromPlaylistMenuItem.setId("deleteFromPlaylistMenuItem");
            deleteFromPlaylistMenuItem.setOnAction(event ->
                    selectedPlaylistProperty.get().ifPresent(audioPlaylist -> {
                    ObservableList<ObservableAudioItem> selectedAudioItems = getSelectionModel().getSelectedItems();
                    audioPlaylist.getAudioItemsProperty().removeAll(selectedAudioItems);
                }));

            getItems().addAll(playMenuItem, editMenuItem, deleteMenuItem, addToQueueMenuItem, new SeparatorMenuItem());
            getItems().addAll(deleteFromPlaylistMenuItem, addToPlaylistMenu);
        }

        @Override
        public void show(Node anchor, double screenX, double screenY) {
            playlistsInMenu.clear();
            Optional<ObservablePlaylist> selectedPlaylist = selectedPlaylistProperty.get();
            if (selectedPlaylist.isPresent() && ! selectedPlaylist.get().isDirectory())  {
                playlistsProperty.stream()
                        .filter(p -> ! p.isDirectory())
                        .forEach(this::addPlaylistToMenuList);

                addToPlaylistMenu.getItems().clear();
                addToPlaylistMenu.getItems().addAll(playlistsInMenu);
                deleteFromPlaylistMenuItem.setVisible(true);
            }
            else
                deleteFromPlaylistMenuItem.setVisible(false);
            super.show(anchor, screenX, screenY);
        }

        private void addPlaylistToMenuList(ObservablePlaylist playlist) {
            Optional<ObservablePlaylist> selectedPlaylist = selectedPlaylistProperty.get();
            if (! (selectedPlaylist.isPresent() && selectedPlaylist.get().equals(playlist))) {
                MenuItem playlistMenuItem = new MenuItem(playlist.getName());
                playlistMenuItem.setOnAction(event -> playlist.getAudioItemsProperty().addAll(getSelectionModel().getSelectedItems()));
                playlistsInMenu.add(playlistMenuItem);
            }
        }
    }
}

class ArtistNameTableCell extends TableCell<ObservableAudioItem, Artist> {

    public ArtistNameTableCell(TableColumn<ObservableAudioItem, Artist> column) {
        super();
    }

    @Override
    protected void updateItem(Artist artist, boolean empty) {
        super.updateItem(artist, empty);
        if (empty || artist == null)
            setText("");
        else
            setText(artist.getName());
    }
}

class AlbumNameTableCell extends TableCell<ObservableAudioItem, Album> {

    public AlbumNameTableCell(TableColumn<ObservableAudioItem, Album> column) {
        super();
    }

    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);
        if (empty || album == null)
            setText("");
        else
            setText(album.getName());
    }
}

class AlbumArtistTableCell extends TableCell<ObservableAudioItem, Album> {

    public AlbumArtistTableCell(TableColumn<ObservableAudioItem, Album> column) {
        super();
    }

    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);
        if (empty || album == null)
            setText("");
        else
            setText(album.getAlbumArtist().getName());
    }
}

class AlbumLabelNameTableCell extends TableCell<ObservableAudioItem, Album> {

    public AlbumLabelNameTableCell(TableColumn<ObservableAudioItem, Album> column) {
        super();
    }

    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);
        if (empty || album == null)
            setText("");
        else
            setText(album.getLabel().getName());
    }
}

class AlbumYearTableCell extends TableCell<ObservableAudioItem, Album> {

    public AlbumYearTableCell(TableColumn<ObservableAudioItem, Album> column) {
        super();
    }

    @Override
    protected void updateItem(Album album, boolean empty) {
        super.updateItem(album, empty);
        if (empty || album == null)
            setText("");
        else
            setText(String.valueOf(album.getYear()));
    }
}

class NumericTableCell extends TableCell<ObservableAudioItem, Number> {

    public NumericTableCell(TableColumn<ObservableAudioItem, Number> column) {
        super();
    }

    @Override
    protected void updateItem(Number item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || ((int) item) < 1)
            setText("");
        else
            setText("" + item);
    }
}

class DateTimeTableCell extends TableCell<ObservableAudioItem, LocalDateTime> {

    public DateTimeTableCell(TableColumn<ObservableAudioItem, LocalDateTime> column) {
        super();
    }

    @Override
    protected void updateItem(LocalDateTime item, boolean empty) {
        super.updateItem(item, empty);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
        if (item == null)
            setText("");
        else
            setText(item.format(dateFormatter));
    }
}

class ByteSizeTableCell extends TableCell<ObservableAudioItem, Number> {

    public ByteSizeTableCell(TableColumn<ObservableAudioItem, Number> column) {
        super();
    }

    @Override
    protected void updateItem(Number item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null)
            setText("");
        else {
            setText(FileUtils.byteCountToDisplaySize(item.longValue()));
        }
    }
}

class DurationTableCell extends TableCell<ObservableAudioItem, Duration> {

    public DurationTableCell(TableColumn<ObservableAudioItem, Duration> column) {
        super();
    }

    @Override
    protected void updateItem(Duration item, boolean empty) {
        super.updateItem(item, empty);
        if (item == null)
            setText("");
        else {
            var hours = (int) item.toHours();
            var minutes = (int) item.minusHours(hours).toMinutes();
            var seconds = (int) item.minusHours(hours).minusMinutes(minutes).getSeconds();
            var stringBuilder = new StringBuilder();
            if (hours > 0)
                stringBuilder.append(hours).append(":");

            if (minutes < 10)
                stringBuilder.append(0).append(minutes).append(":");
            else
                stringBuilder.append(minutes).append(":");

            if (seconds < 10)
                stringBuilder.append(0).append(seconds);
            else
                stringBuilder.append(seconds);
            setText(stringBuilder.toString());
        }
    }
}

class BitRateTableCell extends TableCell<ObservableAudioItem, Number> {

    public BitRateTableCell(TableColumn<ObservableAudioItem, Number> column) {
        super();
    }

    @Override
    protected void updateItem(Number item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null || ((int) item) == 0)
            setText("");
        else
            setText("" + item);
    }
}
