package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

public class ExceptionEvent extends ApplicationEvent {

    public final Throwable exception;

    public ExceptionEvent(Throwable exception, Object source) {
        super(source);
        this.exception = exception;
    }
}
