package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class DeleteSelectedPlaylistEvent extends ApplicationEvent {

    public DeleteSelectedPlaylistEvent(Object source) {
        super(source);
    }
}
