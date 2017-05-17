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

import com.transgressoft.musicott.model.*;
import com.transgressoft.musicott.view.*;
import javafx.collections.*;
import javafx.scene.control.*;
import org.fxmisc.easybind.*;

/**
 * ListView for the application navigation showing modes.
 *
 * @author Octavio Calleya
 * @version 0.10-b
 */
public class NavigationMenuListView extends ListView<NavigationMode> {

    public NavigationMenuListView(NavigationController navigationController) {
        super();
        setId("navigationModeListView");
        setPrefHeight(USE_COMPUTED_SIZE);
        setPrefWidth(USE_COMPUTED_SIZE);

        NavigationMode[] navigationModes = {NavigationMode.ALL_TRACKS, NavigationMode.ARTISTS};
        setItems(FXCollections.observableArrayList(navigationModes));

        EasyBind.subscribe(getSelectionModel().selectedItemProperty(), mode -> {
            if (mode != null)
                navigationController.setNavigationMode(mode);
        });
        setCellFactory(listView -> new NavigationListCell());
    }
}
