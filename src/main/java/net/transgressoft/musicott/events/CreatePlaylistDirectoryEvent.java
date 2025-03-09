package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class CreatePlaylistDirectoryEvent extends ApplicationEvent {

    public CreatePlaylistDirectoryEvent(Object source) {
        super(source);
    }
}
