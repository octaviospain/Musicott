package net.transgressoft.musicott.view;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import net.rgielen.fxweaver.core.FxmlView;
import net.transgressoft.commons.event.CrudEvent;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioLibrary;
import net.transgressoft.commons.music.audio.AlbumView;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.ArtistView;
import net.transgressoft.musicott.events.PlayItemEvent;
import net.transgressoft.musicott.events.SearchTextTypedEvent;
import net.transgressoft.musicott.view.custom.table.ArtistAlbumListRow;
import net.transgressoft.musicott.view.custom.table.SimpleAudioItemTableView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.fxmisc.easybind.EasyBind.map;
import static org.fxmisc.easybind.EasyBind.subscribe;

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

    private ObservableMap<AlbumView<ObservableAudioItem>, ArtistAlbumListRow> albumViews;
    private ObjectProperty<Optional<Artist>> selectedArtistProperty;
    private FilteredList<Artist> filteredArtists;

    @Autowired
    public ArtistViewController(ObservableAudioLibrary audioRepository, ApplicationContext applicationContext) {
        this.audioRepository = audioRepository;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        albumViews = FXCollections.observableMap(new TreeMap<>());
        albumViews.addListener(this::albumViewsChangeListener);

        // TODO replace listener from artistsListView to selectedArtistProperty ?
        artistsListView.getSelectionModel().selectedItemProperty().addListener(this::selectedArtistListener);

        artistsListView.setCellFactory(listview -> new ArtistCell());
        artistsListView.setOnMouseClicked(this::doubleClickOnArtistHandler);
        totalAlbumsLabel.setText("0 albums");
        totalTracksLabel.setText("0 tracks");
        artistRandomButton.visibleProperty().bind(map(nameLabel.textProperty().isEmpty().not(), Function.identity()));
        artistRandomButton.setOnAction(e -> playRandomArtistTracks());
        selectedArtistProperty = new SimpleObjectProperty<>(this, "selected artist", Optional.empty());
        nameLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            if (selectedArtistProperty.get().isPresent()) {
                return selectedArtistProperty.get().get().getName();
            } else {
                return "";
            }
        }, selectedArtistProperty));

        audioRepository.subscribe(this::audioItemsChangeSubscription);

        filteredArtists = new FilteredList<>(audioRepository.artistsProperty(), artist -> true);
        filteredArtists.addListener((ListChangeListener<? super Artist>) this::artistsListener);
        var sortedAudioItems = new SortedList<>(filteredArtists, Comparator.comparing(Artist::getName));
        artistsListView.setItems(sortedAudioItems);
        artistsListView.getSelectionModel().select(0);
    }

    private void artistsListener(ListChangeListener.Change<? extends Artist> change) {
        if (selectedArtistProperty.get().isEmpty() && !change.getList().isEmpty()) {
            artistsListView.getSelectionModel().select(0);
            artistsListView.getFocusModel().focus(0);
        }
    }

    @SuppressWarnings("java:S4968") // Type parameter AlbumView is final in kotlin but the compiler does not allow to remove the extends clause
    private void albumViewsChangeListener(MapChangeListener.Change<? extends AlbumView<ObservableAudioItem>, ? extends ArtistAlbumListRow> change) {
        if (change.wasAdded()) {
            albumsListView.getItems().add(change.getValueAdded());
        } else if (change.wasRemoved()) {
            albumsListView.getItems().remove(change.getValueRemoved());
        }
        totalTracksLabel.setText(getTotalArtistTracksString());
        totalAlbumsLabel.setText(getAlbumString());
        checkForNullSelectedArtist();
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
                albumViews.clear();
            } else {
                checkForNullSelectedArtist();
            }
        }
    }

    private void doubleClickOnArtistHandler(MouseEvent event) {
        selectedArtistProperty.get().ifPresent(selectedArtist -> {
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

    private void setArtistView(ArtistView<ObservableAudioItem> artist) {
        albumViews.clear();
        artist.getAlbums().forEach(this::addAlbumView);
        if (albumViews.isEmpty() && !artistsListView.getItems().isEmpty()) {
            checkForNullSelectedArtist();
        }
    }

    private String getTotalArtistTracksString() {
        int totalArtistTracks = albumViews.values().stream()
            .mapToInt(trackSet -> trackSet.containedAudioItemsProperty().size())
            .sum();
        String appendix = totalArtistTracks == 1 ? " track" : " tracks";
        return totalArtistTracks + appendix;
    }

    private String getAlbumString() {
        int numberOfTrackSets = albumViews.size();
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
        selectedArtistProperty.get().ifPresent(artist -> {
            var selectedArtistIsUpdated = event.getEntities().values()
                    .stream()
                    .anyMatch(audioItem -> audioItem.getArtistsInvolved().contains(artist.getName()));
            if (selectedArtistIsUpdated) {
                var artistView = audioRepository.getArtistCatalog(artist);
                if (artistView.isEmpty()) {
                    throw new IllegalStateException("Artist " + artist + " should be present at this point");
                }
                modifyArtistView(artistView.get());
            }
        });
    }

    private void modifyArtistView(ArtistView<ObservableAudioItem> artistView) {
        var oldAlbums = albumViews.keySet();
        var newAlbums = artistView.getAlbums();
        var addedAlbums = Sets.difference(newAlbums, oldAlbums).immutableCopy();
        var removedAlbums = Sets.difference(oldAlbums, newAlbums).immutableCopy();
        var keptAlbums = Sets.intersection(oldAlbums, newAlbums).immutableCopy();

        updateKeptAlbums(keptAlbums);
        addedAlbums.forEach(albumView -> Platform.runLater(() -> addAlbumView(albumView)));
        removedAlbums.forEach(albumView -> Platform.runLater(() -> albumViews.remove(albumView)));

        totalTracksLabel.setText(getTotalArtistTracksString());
        totalAlbumsLabel.setText(getAlbumString());
    }

    private void updateKeptAlbums(Collection<AlbumView<ObservableAudioItem>> albumViewsToUpdate) {
        albumViewsToUpdate.forEach(albumView -> {
            var albumTrackSet = albumViews.get(albumView);
            var oldTracks = ImmutableSet.copyOf(albumTrackSet.containedAudioItemsProperty());
            var newTracks = albumView.getAudioItems();
            var addedTracks = Sets.difference(newTracks, oldTracks).immutableCopy();
            var removedTracks = Sets.difference(oldTracks, newTracks).immutableCopy();

            Platform.runLater(() -> {
                albumViews.get(albumView).containedAudioItemsProperty().addAll(addedTracks);
                albumViews.get(albumView).containedAudioItemsProperty().removeAll(removedTracks);
            });
        });
    }

    private void addAlbumView(AlbumView<ObservableAudioItem> albumView) {
        selectedArtistProperty.getValue().ifPresent(showingArtist -> {
            var audioItemsTableView = applicationContext.getBean(SimpleAudioItemTableView.class);
            var artistListRow = applicationContext.getBean(ArtistAlbumListRow.class, showingArtist, albumView, audioItemsTableView);
            subscribe(artistListRow.selectedAudioItemsProperty(), selection -> checkForNullSelectedArtist());
            albumViews.put(albumView, artistListRow);
        });
    }

    // TODO is this needed?
    void checkForNullSelectedArtist() {
        if (artistsListView.getSelectionModel().getSelectedItem() == null ||
                !artistsListView.getItems().isEmpty()) {
            Platform.runLater(() -> {
                artistsListView.getSelectionModel().select(0);
                artistsListView.getFocusModel().focus(0);
                artistsListView.scrollTo(0);
            });
        }
    }

    public ObservableList<ObservableAudioItem> getSelectedTracks() {
        return albumViews.values().stream()
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
            albumViews.values().forEach(trackSet -> {
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
        albumViews.values().forEach(ArtistAlbumListRow::selectAllAudioItems);
    }

    public void deselectAllTracks() {
        albumViews.values().forEach(ArtistAlbumListRow::deselectAllAudioItems);
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
