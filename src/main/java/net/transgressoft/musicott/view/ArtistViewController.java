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
import net.transgressoft.commons.event.*;
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

import static org.fxmisc.easybind.EasyBind.map;

/**
 * @author Octavio Calleya
 */
@FxmlView("/fxml/ArtistViewController.fxml")
@Controller
public class ArtistViewController {

    private static final short DEFAULT_RANDOM_PLAYLIST_SIZE = 23;

    private final ObservableAudioLibrary audioRepository;
    private final ApplicationContext applicationContext;

    @FXML
    private SplitPane artistsViewSplitPane;
    @FXML
    private ListView<Artist> artistsListView;
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

    private ObservableMap<AlbumView<ObservableAudioItem>, ArtistAlbumListRow> artistAlbumListRowMap;
    private ObjectProperty<Optional<Artist>> selectedArtistProperty;
    private FilteredList<Artist> filteredArtists;

    @Autowired
    public ArtistViewController(ObservableAudioLibrary audioLibrary, ApplicationContext applicationContext) {
        this.audioRepository = audioLibrary;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        artistAlbumListRowMap = FXCollections.observableMap(new TreeMap<>());
        artistAlbumListRowMap.addListener(this::artistAlbumListRowMapChangeListener);

        // TODO replace listener from artistsListView to selectedArtistProperty ?
        artistsListView.getSelectionModel().selectedItemProperty().addListener(this::selectedArtistListener);

        artistsListView.setCellFactory(_ -> new ArtistCell());
        artistsListView.setOnMouseClicked(this::doubleClickOnArtistHandler);
        configureArtistsListViewBacking();

        totalAlbumsLabel.setText("0 albums");
        totalTracksLabel.setText("0 tracks");
        artistRandomButton.visibleProperty().bind(map(nameLabel.textProperty().isEmpty().not(), Function.identity()));
        artistRandomButton.setOnAction(e -> playRandomArtistTracks());
        selectedArtistProperty = new SimpleObjectProperty<>(this, "selected artist", Optional.empty());
        nameLabel.textProperty().bind(Bindings.createStringBinding(() ->
                selectedArtistProperty.get().isPresent() ? selectedArtistProperty.get().get().getName() : "",
                selectedArtistProperty));

//        artistsListView.getSelectionModel().select(0);
        audioRepository.subscribe(this::audioItemsChangeSubscription);
    }

    /**
     * Configure the artists ListView to remain backed by the repository's artistsProperty.
     * <p>
     * JavaFX filtering/sorting APIs operate on ObservableList, but the source of truth
     * for artists in the repository is a ReadOnlySetProperty to guarantee uniqueness and
     * set semantics.
     */
    private void configureArtistsListViewBacking() {
        var artistsBackingList = FXCollections.observableArrayList(audioRepository.artistsProperty());

        audioRepository.artistsProperty().addListener((SetChangeListener<Artist>) change -> {
            if (change.wasAdded()) {
                Platform.runLater(() -> artistsBackingList.add(change.getElementAdded()));
            }
            if (change.wasRemoved()) {
                Platform.runLater(() -> artistsBackingList.remove(change.getElementRemoved()));
            }
        });

        filteredArtists = new FilteredList<>(artistsBackingList);
        filteredArtists.addListener(this::artistsListener);
        artistsListView.setItems(new SortedList<>(filteredArtists, Comparator.comparing(Artist::getName)));
    }

    private void artistsListener(ListChangeListener.Change<? extends Artist> change) {
        if (selectedArtistProperty.get().isEmpty() && !change.getList().isEmpty()) {
            artistsListView.getSelectionModel().select(0);
            artistsListView.getFocusModel().focus(0);
        }
    }

    @SuppressWarnings("java:S4968") // Type parameter AlbumView is final in kotlin, but the compiler does not allow to remove the extends clause
    private void artistAlbumListRowMapChangeListener(MapChangeListener.Change<? extends AlbumView<ObservableAudioItem>, ? extends ArtistAlbumListRow> change) {
        Platform.runLater(() -> {
            if (change.wasAdded()) {
                albumsListView.getItems().add(change.getValueAdded());
            } else if (change.wasRemoved()) {
                albumsListView.getItems().remove(change.getValueRemoved());
            }
            totalTracksLabel.setText(getTotalArtistTracksString());
            totalAlbumsLabel.setText(getAlbumString());
        });
//        checkForNullSelectedArtist();
    }

    private void selectedArtistListener(ObservableValue<? extends Artist> obs, Artist oldArtist, Artist newArtist) {
        if (newArtist != null) {
            // if the selected artist is not already selected
            if (! nameLabel.getText().equals(newArtist.getName())) {
                selectedArtistProperty.set(Optional.of(newArtist));
                audioRepository.getArtistCatalog(newArtist).ifPresent(this::setArtistView);
            }
        } else {
            if (artistsListView.getItems().isEmpty()) {
                selectedArtistProperty.set(Optional.empty());
                Platform.runLater(artistAlbumListRowMap::clear);
            } else {
//                checkForNullSelectedArtist();
            }
        }
    }

    private void doubleClickOnArtistHandler(MouseEvent event) {
        selectedArtistProperty.get().ifPresent(_ -> {
            if (event.getClickCount() == 2) {
                playRandomArtistTracks();
            }
        });
    }

    private void playRandomArtistTracks() {
        var selectedArtist = selectedArtistProperty.get().get();
        List<ObservableAudioItem> randomAudioItemsFromArtist = audioRepository.getRandomAudioItemsFromArtist(selectedArtist, DEFAULT_RANDOM_PLAYLIST_SIZE);
        applicationContext.publishEvent(new PlayItemEvent(randomAudioItemsFromArtist, this));
    }

    private void setArtistView(ReactiveArtistCatalog<ObservableAudioItem> artist) {
        artistAlbumListRowMap.clear();
        artist.getAlbums().forEach(this::addAlbumView);
//        if (albumViews.isEmpty() && !artistsListView.getItems().isEmpty()) {
//            checkForNullSelectedArtist();
//        }
    }

    private String getTotalArtistTracksString() {
        int totalArtistTracks = artistAlbumListRowMap.values().stream()
            .mapToInt(trackSet -> trackSet.containedAudioItemsProperty().size())
            .sum();
        String appendix = totalArtistTracks == 1 ? " track" : " tracks";
        return totalArtistTracks + appendix;
    }

    private String getAlbumString() {
        int numberOfTrackSets = artistAlbumListRowMap.size();
        var appendix = numberOfTrackSets == 1 ? " album" : " albums";
        return numberOfTrackSets + appendix;
    }

    /**
     * The subscription on the audio items is used to update the artist view when
     * audio items are created, updated, or removed.
     *
     * @param event The event that contains the audio items that were changed.
     */
    private void audioItemsChangeSubscription(CrudEvent<Integer, ObservableAudioItem> event) {
        selectedArtistProperty.get().ifPresent(selectedArtist -> {
            var artistAudioItems = event.getEntities().values()
                    .stream()
                    .filter(audioItem -> audioItem.getArtistsInvolved().contains(selectedArtist))
                    .toList();

            if (! artistAudioItems.isEmpty()) {
                var artistView = audioRepository.getArtistCatalog(selectedArtist);
                if (artistView.isEmpty()) {
                    throw new IllegalStateException("Artist " + selectedArtist + " should be present at this point");
                }
                modifyArtistView(event.getType(), artistAudioItems);
            }
        });
    }

    private void modifyArtistView(CrudEvent.Type eventType, List<ObservableAudioItem> modifiedAudioItems) {
        switch (eventType) {
            case CREATE -> {
                var a = artistAlbumListRowMap.keySet().stream()
                        .filter(albumView -> modifiedAudioItems.stream().anyMatch(i -> i.getAlbum().getName().equals(albumView.getAlbumName())))
                        .toList();

            }
            case UPDATE -> {
            }
            case DELETE -> {
            }
            case READ -> {} // Nothing to do
        }

        var currentAlbums = artistAlbumListRowMap.keySet();
        if (updatedAlbums.isEmpty()) {
            Platform.runLater(artistAlbumListRowMap::clear);
            return;
        }

        updatedAlbums.forEach(albumView -> {
            currentAlbums.removeIf(a -> a.getAlbumName().equals(albumView.getAlbumName()));
            Platform.runLater(() -> {
                var audioItemsTableView = applicationContext.getBean(SimpleAudioItemTableView.class);
                var showingArtist = selectedArtistProperty.getValue().get();
                var artistListRow = applicationContext.getBean(ArtistAlbumListRow.class, showingArtist, albumView, audioItemsTableView);
                artistAlbumListRowMap.put(albumView, artistListRow);
            });
        });

        Platform.runLater(() -> {
            totalTracksLabel.setText(getTotalArtistTracksString());
            totalAlbumsLabel.setText(getAlbumString());
        });
//
//        // Added albums = updated - current
//        var albumsToAdd = new HashSet<>(updatedAlbums);
//        albumsToAdd.removeAll(currentAlbums);
//
//        // Removed albums = current - updated
//        var albumsToRemove = new HashSet<>(currentAlbums);
//        albumsToRemove.removeAll(updatedAlbums);
//
//        // Kept albums = intersection of current and updated
//        var albumsToUpdate = new HashSet<>(currentAlbums);
//        albumsToUpdate.retainAll(updatedAlbums);
//
//        updateKeptAlbums(albumsToUpdate);
//        albumsToAdd.forEach(albumView -> Platform.runLater(() -> addAlbumView(albumView)));
//        albumsToRemove.forEach(albumView -> Platform.runLater(() -> artistAlbumListRowMap.remove(albumView)));
//
//        totalTracksLabel.setText(getTotalArtistTracksString());
//        totalAlbumsLabel.setText(getAlbumString());
    }

    private void updateKeptAlbums(Collection<AlbumView<ObservableAudioItem>> albumViewsToUpdate) {
        albumViewsToUpdate.forEach(albumView -> {
            var albumTrackSet = artistAlbumListRowMap.get(albumView);
            var oldTracks = new HashSet<>(albumTrackSet.containedAudioItemsProperty());
            var newTracks = new HashSet<>(albumView.getAudioItems());
            newTracks.removeAll(oldTracks);
            oldTracks.removeAll(newTracks);

            Platform.runLater(() -> {
                artistAlbumListRowMap.get(albumView).containedAudioItemsProperty().addAll(newTracks);
                artistAlbumListRowMap.get(albumView).containedAudioItemsProperty().removeAll(oldTracks);
            });
        });
    }

    private void addAlbumView(AlbumView<ObservableAudioItem> albumView) {
        selectedArtistProperty.getValue().ifPresent(showingArtist -> {
            var audioItemsTableView = applicationContext.getBean(SimpleAudioItemTableView.class);
            var artistListRow = applicationContext.getBean(ArtistAlbumListRow.class, showingArtist, albumView, audioItemsTableView);
//            subscribe(artistListRow.selectedAudioItemsProperty(), selection -> checkForNullSelectedArtist());
            artistAlbumListRowMap.put(albumView, artistListRow);
        });
    }

    // TODO is this needed?
//    void checkForNullSelectedArtist() {
//        if (artistsListView.getSelectionModel().getSelectedItem() == null || !artistsListView.getItems().isEmpty()) {
//            Platform.runLater(() -> {
//                artistsListView.getSelectionModel().select(0);
//                artistsListView.getFocusModel().focus(0);
//                artistsListView.scrollTo(0);
//            });
//        }
//    }

    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        return artistAlbumListRowMap.values().stream()
            .flatMap(entry -> entry.selectedAudioItemsProperty().stream())
            .collect(Collectors.toCollection(FXCollections::observableArrayList));
    }

    public void findAudioItemInArtistViewAndSelect(ObservableAudioItem audioItem) {
        Set<Artist> artistsInvolved = audioItem.getArtistsInvolved();
        Optional<Artist> selectedArtist = selectedArtistProperty.getValue();
        if (selectedArtist.isPresent() && !artistsInvolved.contains(selectedArtist.get())) {
            Artist newArtist = artistsInvolved.stream().findFirst().get();
            selectedArtistProperty.setValue(Optional.of(newArtist));

            artistsListView.getSelectionModel().select(newArtist);  // This should work
            artistsListView.scrollTo(newArtist);
        } else {
            artistAlbumListRowMap.values().forEach(trackSet -> {
                if (trackSet.getAlbumView().getAlbumName().equals(audioItem.getAlbum().getName())) {
                    trackSet.selectAudioItem(audioItem);
                    albumsListView.scrollTo(trackSet);
                } else {
                    trackSet.deselectAllAudioItems();
                }
            });
        }
    }

    public void selectAllTracks() {
        artistAlbumListRowMap.values().forEach(ArtistAlbumListRow::selectAllAudioItems);
    }

    public void deselectAllTracks() {
        artistAlbumListRowMap.values().forEach(ArtistAlbumListRow::deselectAllAudioItems);
    }

    @EventListener
    public void searchTextTypedEvent(SearchTextTypedEvent event) {
        filteredArtists.setPredicate(filterArtistsByQuery(event.searchText));
    }

    private Predicate<Artist> filterArtistsByQuery(String query) {
        return artist -> {
            boolean result = query == null || query.isEmpty();
            if (!result) {
                result = artistMatchesQuery(artist, query.toLowerCase());
            }
            return result;
        };
    }

    private boolean artistMatchesQuery(Artist artist, String query) {
        boolean matchesName = artist.getName().toLowerCase().contains(query.toLowerCase());

        // TODO test if logic can be replaced by looking into albumViews
        boolean containsMatchedTrack = audioRepository.contains(audioItem -> audioItem.getArtist().equals(artist) &&
                (audioItem.getArtist().getName().toLowerCase().contains(query) ||
                        audioItem.getAlbum().getAlbumArtist().getName().toLowerCase().contains(query) ||
                        audioItem.getComments().toLowerCase().contains(query) ||
                        audioItem.getAlbum().getName().toLowerCase().contains(query)));
        return matchesName || containsMatchedTrack;
    }

    public ReadOnlyObjectProperty<Optional<Artist>> selectedArtistProperty() {
        return selectedArtistProperty;
    }

    private static class ArtistCell extends ListCell<Artist> {

        @Override
        protected void updateItem(Artist item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null)
                Platform.runLater(() -> setGraphic(null));
            else
                Platform.runLater(() -> setGraphic(new Label(item.getName())));
        }
    }
}
