package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class PlayPlaylistRandomlyEvent extends ApplicationEvent {

    public final ObservablePlaylist playlist;

    public PlayPlaylistRandomlyEvent(ObservablePlaylist playlist, Object source) {
        super(source);
        this.playlist = playlist;
    }
}
