package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class PauseEvent extends ApplicationEvent {

    public PauseEvent(Object source) {
        super(source);
    }
}
