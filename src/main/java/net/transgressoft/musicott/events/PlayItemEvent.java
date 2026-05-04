package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @author Octavio Calleya
 */
public class PlayItemEvent extends ApplicationEvent {

    // ObservableAudioItem isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final List<ObservableAudioItem> audioItems;

    public PlayItemEvent(List<ObservableAudioItem> audioItems, Object source) {
        super(source);
        this.audioItems = audioItems;
    }
}
