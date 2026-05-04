package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class AddToPlayQueueEvent extends ApplicationEvent {

    // ObservableAudioItem isn't Serializable; ApplicationEvent stays in-VM.
    @SuppressWarnings("java:S1948")
    public final List<ObservableAudioItem> audioItems;

    public AddToPlayQueueEvent(List<ObservableAudioItem> audioItems, Object source) {
        super(source);
        this.audioItems = audioItems;
    }
}
