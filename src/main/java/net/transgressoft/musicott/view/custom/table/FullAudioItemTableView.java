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
public class FullAudioItemTableView extends AudioItemTableViewBase {

    @SuppressWarnings ("unchecked")
    public FullAudioItemTableView(ObjectProperty<Optional<ObservablePlaylist>> selectedPlaylistProperty, StringProperty searchTextProperty,
                                  ReadOnlySetProperty<ObservablePlaylist> playlistsProperty, ApplicationEventPublisher applicationEventPublisher) {
        super(selectedPlaylistProperty, searchTextProperty, playlistsProperty, applicationEventPublisher);
        getColumns().addAll(artistCol, nameCol, albumCol, genreCol, labelCol, bpmCol, totalTimeCol);
        getColumns().addAll(yearCol, sizeCol, trackNumberCol, discNumberCol, albumArtistCol, commentsCol);
        getColumns().addAll(bitRateCol, playCountCol, dateModifiedCol, dateAddedCol);
        setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);
        getSortOrder().add(dateAddedCol);
    }
}
