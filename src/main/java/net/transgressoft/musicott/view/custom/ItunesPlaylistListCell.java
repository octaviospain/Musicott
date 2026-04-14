/*
 * This file is part of Musicott software.
 *
 * Musicott software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Musicott library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Musicott. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2015 - 2017 Octavio Calleya
 */

package net.transgressoft.musicott.view.custom;

import net.transgressoft.commons.music.itunes.ItunesPlaylist;
import net.transgressoft.musicott.view.ItunesPlaylistsPickerController;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;

/**
 * Custom {@link ListCell} that defines the behaviour of an {@link ItunesPlaylist}
 * in the {@link ItunesPlaylistsPickerController} window.
 *
 * @author Octavio Calleya
 */
public class ItunesPlaylistListCell extends ListCell<ItunesPlaylist> {

    private final ItunesPlaylistsPickerController playlistsPickerController;

    public ItunesPlaylistListCell(ItunesPlaylistsPickerController playlistsPickerController) {
        super();
        this.playlistsPickerController = playlistsPickerController;
        setOnMouseClicked(this::onMouseClicked);
    }

    private void onMouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2 && ! isEmpty())
            playlistsPickerController.movePlaylist(getItem());
    }

    @Override
    protected void updateItem(ItunesPlaylist itunesPlaylist, boolean empty) {
        super.updateItem(itunesPlaylist, empty);
        if (empty || itunesPlaylist == null)
            setGraphic(null);
        else
            setGraphic(new Label(getPlaylistString(itunesPlaylist)));
    }

    private String getPlaylistString(ItunesPlaylist itunesPlaylist) {
        var numTracks = itunesPlaylist.getTrackIds().size();
        var name = itunesPlaylist.getName();
        return name + " [" + numTracks + " tracks]";
    }
}
