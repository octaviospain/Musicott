package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class InvalidAudioItemsForEditionEvent extends ApplicationEvent {

    // ObservableAudioItem isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final List<? extends ObservableAudioItem> invalidAudioItems;

    public InvalidAudioItemsForEditionEvent(List<ObservableAudioItem> invalidAudioItems, Object source) {
        super(source);
        this.invalidAudioItems = invalidAudioItems;
    }
}
