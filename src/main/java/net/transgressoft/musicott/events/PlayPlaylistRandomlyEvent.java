package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.playlist.ObservablePlaylist;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class PlayPlaylistRandomlyEvent extends ApplicationEvent {

    // ObservablePlaylist isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final ObservablePlaylist playlist;

    public PlayPlaylistRandomlyEvent(ObservablePlaylist playlist, Object source) {
        super(source);
        this.playlist = playlist;
    }
}
