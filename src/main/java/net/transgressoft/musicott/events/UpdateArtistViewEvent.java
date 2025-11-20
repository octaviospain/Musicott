package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import net.transgressoft.commons.music.audio.Artist;
import net.transgressoft.commons.music.audio.ReactiveArtistCatalog;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * @author Octavio Calleya
 */
public class UpdateArtistViewEvent extends ApplicationEvent {

    public final Map<Artist, ReactiveArtistCatalog<ObservableAudioItem>> artistViews;

    public UpdateArtistViewEvent(Map<Artist, ReactiveArtistCatalog<ObservableAudioItem>> artistViews, Object source) {
        super(source);
        this.artistViews = artistViews;
    }
}
