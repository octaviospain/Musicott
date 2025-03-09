package net.transgressoft.musicott.view.custom.table;

import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.StringProperty;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

/**
 * @author Octavio Calleya
 */
public class SimpleAudioItemTableView extends AudioItemTableViewBase {

    private static final String SIMPLE_TRACK_TABLE_STYLE = "/css/tracktable-trackareaset.css";

    @SuppressWarnings ("unchecked")
    public SimpleAudioItemTableView(ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty,
                                    ReadOnlySetProperty<ObservablePlaylist> playlistsProperty,
                                    StringProperty searchTextProperty,
                                    ApplicationEventPublisher applicationEventPublisher) {
        super(selectedPlaylistProperty, searchTextProperty, playlistsProperty, applicationEventPublisher);

        getColumns().addAll(trackNumberCol, nameCol, totalTimeCol);
        getSortOrder().add(trackNumberCol);
        getStylesheets().add(getClass().getResource(SIMPLE_TRACK_TABLE_STYLE).toExternalForm());
        getStyleClass().add("no-header");
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        setFixedCellSize(25);

        nameCol.setMinWidth(150);
        nameCol.setPrefWidth(USE_COMPUTED_SIZE);
        nameCol.setStyle(CENTER_LEFT_ALIGN);
        artistCol.setPrefWidth(150);
        artistCol.setStyle(CENTER_LEFT_ALIGN);
        trackNumberCol.setMinWidth(24);
        trackNumberCol.setMaxWidth(24);
    }

    public void removeArtistColumn() {
        getColumns().remove(artistCol);
    }

    public void placeArtistColumn() {
        if (! getColumns().contains(artistCol)) {
            getColumns().add(2, artistCol);
        }
    }
}
