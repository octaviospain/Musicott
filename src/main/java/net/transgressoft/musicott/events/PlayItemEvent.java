package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @author Octavio Calleya
 */
public class PlayItemEvent extends ApplicationEvent {

    public final List<ObservableAudioItem> audioItems;

    public PlayItemEvent(List<ObservableAudioItem> audioItems, Object source) {
        super(source);
        this.audioItems = audioItems;
    }
}
