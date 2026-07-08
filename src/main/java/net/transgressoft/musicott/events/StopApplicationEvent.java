package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * Published to request an orderly application shutdown.
 */
public class StopApplicationEvent extends ApplicationEvent {

    public StopApplicationEvent(Object source) {
        super(source);
    }
}
