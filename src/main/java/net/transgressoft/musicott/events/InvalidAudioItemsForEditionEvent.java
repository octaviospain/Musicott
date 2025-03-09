package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class InvalidAudioItemsForEditionEvent extends ApplicationEvent {

    public final List<? extends ObservableAudioItem> invalidAudioItems;

    public InvalidAudioItemsForEditionEvent(List<ObservableAudioItem> invalidAudioItems, Object source) {
        super(source);
        this.invalidAudioItems = invalidAudioItems;
    }
}
