package net.transgressoft.musicott.view.custom;

import net.transgressoft.commons.music.audio.Artist;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

/**
 * @author Octavio Calleya
 */
public class ArtistCell extends ListCell<Artist> {

    @Override
    protected void updateItem(Artist item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null)
            setGraphic(null);
        else
            setGraphic(new Label(item.getName()));
    }
}
