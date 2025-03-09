package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

public class StopApplicationEvent extends ApplicationEvent {

    public StopApplicationEvent(Object source) {
        super(source);
    }
}
