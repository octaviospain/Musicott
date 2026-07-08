package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Published when a metadata-edit request contains items that cannot be edited,
 * carrying the rejected items so the UI can inform the user.
 */
public class InvalidAudioItemsForEditionEvent extends ApplicationEvent {

    // ObservableAudioItem isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final List<? extends ObservableAudioItem> invalidAudioItems;

    public InvalidAudioItemsForEditionEvent(List<ObservableAudioItem> invalidAudioItems, Object source) {
        super(source);
        this.invalidAudioItems = invalidAudioItems;
    }
}
