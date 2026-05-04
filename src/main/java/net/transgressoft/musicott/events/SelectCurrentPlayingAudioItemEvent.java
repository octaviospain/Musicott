package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class SelectCurrentPlayingAudioItemEvent extends ApplicationEvent {

    // ObservableAudioItem isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final ObservableAudioItem audioItem;

    public SelectCurrentPlayingAudioItemEvent(ObservableAudioItem audioItem, Object source) {
        super(source);
        this.audioItem = audioItem;
    }
}
