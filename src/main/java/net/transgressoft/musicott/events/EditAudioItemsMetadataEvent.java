package net.transgressoft.musicott.events;

import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

import static net.transgressoft.musicott.view.EditController.AudioItemMetadataChange;

/**
 * @author Octavio Calleya
 */
public class EditAudioItemsMetadataEvent extends ApplicationEvent {

    public final Set<ObservableAudioItem> audioItems;
    public final AudioItemMetadataChange audioItemMetadataChange;

    public EditAudioItemsMetadataEvent(Set<ObservableAudioItem> audioItems, AudioItemMetadataChange audioItemMetadataChange, Object source) {
        super(source);
        this.audioItems = audioItems;
        this.audioItemMetadataChange = audioItemMetadataChange;
    }
}
