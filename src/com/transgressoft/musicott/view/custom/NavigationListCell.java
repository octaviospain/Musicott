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

import com.transgressoft.musicott.model.NavigationMode;
import javafx.beans.value.*;
import javafx.css.*;
import javafx.scene.control.*;

import static com.transgressoft.musicott.model.NavigationMode.*;

/**
 * Custom {@link ListCell} that define the style of each row in
 * the {@link NavigationMenuListView}, managed by pseudo classes.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 * @since 0.10-b
 */
public class NavigationListCell extends ListCell<NavigationMode> {

    private PseudoClass tracks = PseudoClass.getPseudoClass("tracks");
    private PseudoClass tracksSelected = PseudoClass.getPseudoClass("tracks-selected");
    private PseudoClass artists = PseudoClass.getPseudoClass("artists");
    private PseudoClass artistsSelected = PseudoClass.getPseudoClass("artists-selected");

    public NavigationListCell() {
        super();

        ChangeListener<Boolean> isSelectedListener = (obs, oldModeSelected, newModeSelected) -> {
            NavigationMode mode = itemProperty().getValue();
            boolean isSelected = newModeSelected;
            updatePseudoClassStates(mode, isSelected);
        };

        itemProperty().addListener((obs, oldMode, newMode) -> {
            if (oldMode != null) {
                setText("");
                selectedProperty().removeListener(isSelectedListener);
            }

            if (newMode != null) {
                setText(newMode.toString());
                selectedProperty().addListener(isSelectedListener);
                updatePseudoClassStates(newMode, selectedProperty().get());
            }
            else
                disablePseudoClassesStates();
        });
    }

    private void updatePseudoClassStates(NavigationMode mode, boolean isSelected) {
        pseudoClassStateChanged(tracks, mode.equals(ALL_TRACKS) && ! isSelected);
        pseudoClassStateChanged(tracksSelected, mode.equals(ALL_TRACKS) && isSelected);
        pseudoClassStateChanged(artists, mode.equals(ARTISTS) && ! isSelected);
        pseudoClassStateChanged(artistsSelected, mode.equals(ARTISTS) && isSelected);
    }

    private void disablePseudoClassesStates() {
        pseudoClassStateChanged(tracks, false);
        pseudoClassStateChanged(tracksSelected, false);
        pseudoClassStateChanged(artists, false);
        pseudoClassStateChanged(artistsSelected, false);
    }
}