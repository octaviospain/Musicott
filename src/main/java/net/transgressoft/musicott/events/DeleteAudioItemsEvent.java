package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.Collection;

/**
 * @author Octavio Calleya
 */
public class DeleteAudioItemsEvent extends ApplicationEvent {

    // ObservableAudioItem isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final Collection<ObservableAudioItem> audioItems;

    public DeleteAudioItemsEvent(Collection<ObservableAudioItem> audioItems, Object source) {
        super(source);
        this.audioItems = audioItems;
    }
}
