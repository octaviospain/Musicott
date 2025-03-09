package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class CreatePlaylistEvent extends ApplicationEvent {

    public CreatePlaylistEvent(Object source) {
        super(source);
    }
}
