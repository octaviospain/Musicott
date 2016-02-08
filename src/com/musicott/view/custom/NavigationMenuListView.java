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
 */

package com.musicott.view.custom;

import javafx.scene.control.ListView;

/**
 * @author Octavio Calleya
 *
 */
public class NavigationMenuListView extends ListView<String> {

	private MusicottScene musicottScene;
	
	public NavigationMenuListView(MusicottScene scene) {
		super();
		musicottScene = scene;
		setId("showMenuListView");
		setPrefHeight(200);
		setPrefWidth(150);
		getSelectionModel().selectedItemProperty().addListener(listener -> {
			String selectedMenu = getSelectionModel().getSelectedItem();
			if(selectedMenu != null) musicottScene.showMode(selectedMenu);
		});
	}
}