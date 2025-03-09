package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

import java.nio.file.Path;

public class ChangeApplicationDirectoryEvent extends ApplicationEvent {

    public final Path newDirectory;

    public ChangeApplicationDirectoryEvent(Path newDirectory, Object source) {
        super(source);
        this.newDirectory = newDirectory;
    }
}
