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

package com.transgressoft.musicott.view.custom;

import com.google.inject.*;
import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.player.*;
import com.transgressoft.musicott.util.guice.annotations.*;
import com.transgressoft.musicott.view.*;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import org.fxmisc.easybind.*;

import java.util.*;
import java.util.Map.*;
import java.util.stream.*;

import static com.transgressoft.musicott.model.CommonObject.*;

/**
 * Custom {@link TableRow} that represents an {@link Entry} with
 * a track id as a key and the {@link Track} as value.
 *
 * @author Octavio Calleya
 * @version 0.10.1-b
 * @since 0.9.1-b
 */
public class TrackTableRow extends TableRow<Entry<Integer, Track>> {

    public static final DataFormat TRACK_IDS_MIME_TYPE = new DataFormat("application/x-java-tracks-id");

    static final Image DRAGBOARD_ICON = new Image(TrackTableRow.class.getResourceAsStream(DRAGBOARD_ICON_PATH.toString()));

    private final PlayerFacade playerFacade;
    private RootController rootController;
    private NavigationController navigationController;

    @Inject
    public TrackTableRow(@RootCtrl RootController rootCtrl, @NavigationCtrl NavigationController navCtrl,
            PlayerFacade playerFacade) {
        this.playerFacade = playerFacade;
        rootController = rootCtrl;
        navigationController = navCtrl;
        setOnMouseClicked(this::playTrackOnMouseClickedHandler);
        setOnDragDetected(this::onDragDetectedMovingTracks);
        EasyBind.subscribe(hoverProperty(), newHovered -> {
            if (newHovered && getItem() != null) {
                Optional<byte[]> cover = getItem().getValue().getCoverImage();
                rootController.updateTrackHoveredCover(cover);
            }
        });
    }

    /**
     * Fires the play of a {@link Track}
     * when the user double-clicks a row.
     */
    private void playTrackOnMouseClickedHandler(MouseEvent event) {
        if (event.getClickCount() == 2 && ! isEmpty()) {
            playerFacade.addTracksToPlayQueue(Collections.singletonList(getItem().getValue()), true);
            navigationController.updateCurrentPlayingPlaylist();
        }
    }

    private void onDragDetectedMovingTracks(MouseEvent event) {
        if (! isEmpty()) {
            Dragboard dragboard = startDragAndDrop(TransferMode.COPY);
            dragboard.setDragView(DRAGBOARD_ICON);

            ObservableList<Entry<Integer, Track>> selection = rootController.getSelectedTracks();
            List<Integer> selectionTracks = selection.stream().map(Entry::getKey).collect(Collectors.toList());
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.put(TRACK_IDS_MIME_TYPE, selectionTracks);
            dragboard.setContent(clipboardContent);
            event.consume();
        }
    }
}
