package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

public class ExceptionEvent extends ApplicationEvent {

    public final Exception exception;

    public ExceptionEvent(Exception exception, Object source) {
        super(source);
        this.exception = exception;
    }
}
