package net.transgressoft.musicott.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author Octavio Calleya
 */
public class ExportSelectedPlaylistsEvent extends ApplicationEvent {

    public ExportSelectedPlaylistsEvent(Object source) {
        super(source);
    }
}
