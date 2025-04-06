package net.transgressoft.musicott.view;

import javafx.beans.value.ObservableValue;
import net.transgressoft.commons.event.CrudEvent;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItemJsonRepository;
import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;
import net.transgressoft.commons.music.audio.AlbumView;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.ArtistView;
import net.transgressoft.commons.music.audio.ImmutableArtist;
import net.transgressoft.musicott.events.PlayItemEvent;
import net.transgressoft.musicott.view.custom.ArtistCell;
import net.transgressoft.musicott.view.custom.table.ArtistListRow;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.neovisionaries.i18n.CountryCode;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.input.MouseEvent;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ObservableAudioItemJsonRepository audioRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @FXML
    private SplitPane artistsViewSplitPane;
    @FXML
    private ListView<Artist> artistsListView;
    @FXML
    private ListView<ArtistListRow> albumsListView;
    @FXML
    private Label nameLabel;
    @FXML
    private Label totalAlbumsLabel;
    @FXML
    private Label totalTracksLabel;
    @FXML
    private Button artistRandomButton;

    private ObservableMap<AlbumView<ObservableAudioItem>, ArtistListRow> albumViews;
    private ObjectProperty<Optional<Artist>> selectedArtistProperty;
    private StringProperty searchTextProperty;
    private ReadOnlyListProperty<Artist> artistsListProperty;

    @Autowired
    public ArtistViewController(ObservableAudioItemJsonRepository audioRepository, ApplicationEventPublisher applicationEventPublisher) {
        this.audioRepository = audioRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @FXML
    public void initialize() {
        albumViews = FXCollections.observableMap(new TreeMap<>());
        albumViews.addListener(this::albumTrackSetsListener);

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

        artistsListProperty = audioRepository.getArtistsProperty();
        // TODO solve the searchTextProperty null pointer exception
        if (searchTextProperty != null) {
            artistsListView.setItems(bindArtistsToSearchField());
        }
    }

    private void albumTrackSetsListener(MapChangeListener.Change<? extends AlbumView<ObservableAudioItem>, ? extends ArtistListRow> change) {
        if (change.wasAdded()) {
            albumsListView.getItems().add(change.getValueAdded());
        } else if (change.wasRemoved()) {
            albumsListView.getItems().remove(change.getValueRemoved());
        }
        totalTracksLabel.setText(getTotalArtistTracksString());
        totalAlbumsLabel.setText(getAlbumString());
        checkSelectedArtist();
    }

    private FilteredList<Artist> bindArtistsToSearchField() {
        FilteredList<Artist> filteredArtists = new FilteredList<>(artistsListProperty, artist -> true);

        subscribe(searchTextProperty, query -> filteredArtists.setPredicate(filterArtistsByQuery(query)));
        return filteredArtists;
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

    private void selectedArtistListener(ObservableValue<? extends Artist> obs, Artist oldArtist, Artist newArtist) {
        if (newArtist != null) {
            // if the selected artist is not already selected
            if (! nameLabel.getText().equals(newArtist.getName())) {
                selectedArtistProperty.set(Optional.of(newArtist));
                audioRepository.getArtistCatalog(newArtist).ifPresent(it -> Platform.runLater(() -> setArtistView(it)));
            }
        } else {
            if (artistsListView.getItems().isEmpty()) {
                selectedArtistProperty.set(Optional.empty());
                albumViews.clear();
            } else {
                checkSelectedArtist();
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
        applicationEventPublisher.publishEvent(new PlayItemEvent(randomAudioItemsFromArtist, this));
    }

    private void setArtistView(ArtistView<ObservableAudioItem> artist) {
        albumViews.clear();
        artist.getAlbums().forEach(this::addAlbumView);
        if (albumViews.isEmpty() && !artistsListView.getItems().isEmpty()) {
            checkSelectedArtist();
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
            var artistListRow = new ArtistListRow(showingArtist, albumView, applicationEventPublisher);
            subscribe(artistListRow.selectedAudioItemsProperty(), selection -> checkSelectedArtist());
            albumViews.put(albumView, artistListRow);
        });
    }

    // TODO is this needed?
    void checkSelectedArtist() {
        if (artistsListView.getSelectionModel().getSelectedItem() == null) {
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
        Set<String> artistsInvolved = audioItem.getArtistsInvolved();
        Optional<Artist> selectedArtist = selectedArtistProperty.getValue();
        if (selectedArtist.isPresent() && !artistsInvolved.contains(selectedArtist.get().getName())) {
            Artist newArtist = new ImmutableArtist(artistsInvolved.stream().findFirst().get(), CountryCode.UNDEFINED);
            selectedArtistProperty.setValue(Optional.of(newArtist));

            artistsListView.getSelectionModel().select(newArtist);  // This should work

            artistsListView.getSelectionModel().select(newArtist);
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
        albumViews.values().forEach(ArtistListRow::selectAllAudioItems);
    }

    public void deselectAllTracks() {
        albumViews.values().forEach(ArtistListRow::deselectAllAudioItems);
    }

    public void setSearchTextProperty(StringProperty searchTextProperty) {
        this.searchTextProperty = searchTextProperty;
        artistsListView.setItems(bindArtistsToSearchField());
        if (!artistsListView.getItems().isEmpty()) {
            checkSelectedArtist();
        }
    }

    public ReadOnlyObjectProperty<Optional<Artist>> selectedArtistProperty() {
        return selectedArtistProperty;
    }
}
