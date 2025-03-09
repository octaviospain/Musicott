package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

public class ErrorEvent extends ApplicationEvent {

    public final String title;
    public final String content;

    public ErrorEvent(String title, String content, Object source) {
        super(source);
        this.title = title;
        this.content = content;
    }
}
