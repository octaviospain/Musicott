package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * @author Octavio Calleya
 */
public class DeleteAudioItemsEvent extends ApplicationEvent {

    public final Set<ObservableAudioItem> audioItems;

    public DeleteAudioItemsEvent(Set<ObservableAudioItem> audioItems, Object source) {
        super(source);
        this.audioItems = audioItems;
    }
}
