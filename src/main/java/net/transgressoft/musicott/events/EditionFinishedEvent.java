package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class EditionFinishedEvent extends ApplicationEvent {

    public EditionFinishedEvent(Object source) {
        super(source);
    }
}
