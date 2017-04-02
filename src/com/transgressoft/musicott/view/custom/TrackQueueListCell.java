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

import com.transgressoft.musicott.view.*;
import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.input.*;

import java.util.*;
import java.util.stream.*;

import static com.transgressoft.musicott.view.MusicottController.*;
import static com.transgressoft.musicott.view.custom.TrackTableRow.*;

/**
 * Custom {@link ListCell} that defines the behaviour of the {@link TrackQueueRow}
 * that represents
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class TrackQueueListCell extends ListCell<TrackQueueRow> {

    private static final String DRAG_OVER_STYLE = "-fx-border-color: rgb(99, 255, 109); -fx-border-width: 0px 0px 1px 0px";
    private final PlayQueueController playQueueController;

    public TrackQueueListCell(PlayQueueController playQueueController) {
        super();
        this.playQueueController = playQueueController;
        setOnDragOver(this::onDragOver);
        setOnDragDetected(this::onDragDetected);
        setOnDragDropped(this::onDragDropped);
        setOnDragExited(this::onDragExited);
    }

    private void onDragOver(DragEvent event) {
        if (! isEmpty() && getItem() != null) {
            event.acceptTransferModes(TransferMode.MOVE);
            setStyle(DRAG_OVER_STYLE);
        }
        event.consume();
    }

    private void onDragExited(DragEvent event) {
        setStyle("");
        event.consume();
    }

    private void onDragDetected(MouseEvent event) {
        if (! isEmpty() && ! playQueueController.isShowingHistoryQueue()) {
            Dragboard dragboard = startDragAndDrop(TransferMode.COPY_OR_MOVE);
            dragboard.setDragView(DRAGBOARD_ICON);

            List<TrackQueueRow> selection = getListView().getSelectionModel().getSelectedItems();
            List<Integer> selectionTracks = selection.stream()
                                                     .map(tqr -> tqr.getRepresentedTrack().getTrackId())
                                                     .collect(Collectors.toList());

            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.put(TRACK_IDS_MIME_TYPE, selectionTracks);
            dragboard.setContent(clipboardContent);
            event.consume();
        }
    }

    private void onDragDropped (DragEvent event) {
        TrackQueueRow selection = getListView().getSelectionModel().getSelectedItem();
        ObservableList<TrackQueueRow> itemsList = getListView().getItems();

        int thisIndex = itemsList.indexOf(getItem());
        int draggedIndex = getListView().getSelectionModel().getSelectedIndex();
        itemsList.set(draggedIndex, getItem());
        itemsList.set(thisIndex, selection);
        event.consume();
    }

    @Override
    protected void updateItem(TrackQueueRow trackQueueRow, boolean empty) {
        super.updateItem(trackQueueRow, empty);
        if (empty || trackQueueRow == null)
            setGraphic(null);
        else
            setGraphic(trackQueueRow);
    }
}