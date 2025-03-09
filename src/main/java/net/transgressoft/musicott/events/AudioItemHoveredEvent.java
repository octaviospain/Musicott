package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class AudioItemHoveredEvent extends ApplicationEvent {

    public final ObservableAudioItem audioItem;

    public AudioItemHoveredEvent(ObservableAudioItem audioItem, Object source) {
        super(source);
        this.audioItem = audioItem;
    }
}
