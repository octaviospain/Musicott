package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * @author Octavio Calleya
 */
public class OpenAudioItemEditorView extends ApplicationEvent {

    public final Set<ObservableAudioItem> audioItems;

    public OpenAudioItemEditorView(Set<ObservableAudioItem> audioItems, Object source) {
        super(source);
        this.audioItems = audioItems;
    }
}
