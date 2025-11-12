/******************************************************************************
 *     Copyright (C) 2025  Octavio Calleya Garcia                             *
 *                                                                            *
 *     This program is free software: you can redistribute it and/or modify   *
 *     it under the terms of the GNU General Public License as published by   *
 *     the Free Software Foundation, either version 3 of the License, or      *
 *     (at your option) any later version.                                    *
 *                                                                            *
 *     This program is distributed in the hope that it will be useful,        *
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *     GNU General Public License for more details.                           *
 *                                                                            *
 *     You should have received a copy of the GNU General Public License      *
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>. *
 ******************************************************************************/

package net.transgressoft.musicott.events;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;
import net.transgressoft.commons.fx.music.audio.ObservableAudioItem;
import org.springframework.context.ApplicationEvent;

public class ShowTableViewContextMenuEvent extends ApplicationEvent {

    public final ObservableList<ObservableAudioItem> selectedAudioItems;
    public final double screenX;
    public final double screenY;

    public ShowTableViewContextMenuEvent(ObservableList<ObservableAudioItem> selectedAudioItems, MouseEvent event) {
        super(event.getSource());
        this.selectedAudioItems = selectedAudioItems == null ? FXCollections.emptyObservableList() : selectedAudioItems;
        this.screenX = event.getScreenX();
        this.screenY = event.getScreenY();
    }
}
