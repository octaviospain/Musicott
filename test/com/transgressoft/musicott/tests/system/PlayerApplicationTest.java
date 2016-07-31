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
 * Copyright (C) 2015, 2016 Octavio Calleya
 */

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
 * along with Musicott library.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.transgressoft.musicott.tests.system;

import com.transgressoft.musicott.*;
import com.transgressoft.musicott.model.*;
import javafx.application.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.junit.*;
import org.testfx.framework.junit.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.loadui.testfx.GuiTest.*;
import static org.loadui.testfx.controls.TableViews.*;
import static org.testfx.api.FxAssert.*;
import static org.testfx.matcher.base.NodeMatchers.*;

/**
 * @author Octavio Calleya
 *
 */
public class PlayerApplicationTest extends ApplicationTest {

	private Application frame;
	private Stage stage;
	
	@Override
	public void start(Stage stage) throws Exception {
		frame = new MusicottApplication();
		this.stage = stage;
		frame.init();
		frame.start(this.stage);
	}
	
	@After
	public void closeUp() {
		TableView<Track> tv = find("#trackTable");
		tv.getItems().clear();
	}
	
	@Test
	public void singlePlayTest() {
		verifyThat("#playButton", isDisabled());
		verifyThat("#prevButton", isDisabled());
		verifyThat("#nextButton", isDisabled());
		verifyThat("#titleLabel", hasText(""));
		verifyThat("#artistAlbumLabel", hasText(""));
		ListView<GridPane> playList = find("#listView");
		assertTrue(playList.getItems().isEmpty());
		
		clickOn("#menuFile");
		clickOn("#menuItemImport");
		clickOn("#openButton");		// Because the Filechooser is not selectable with testFX, I set the correct folder
		press(KeyCode.ENTER);		// previously selecting it in a normal execution, and the filechooser opens thereverifyThat("#pBar", isVisible());
		TableView<Track> tv = find("#trackTable");
		while(tv.getItems() == null || tv.getItems().size() == 0);
		verifyThat(numberOfRowsIn("#trackTable"), greaterThan(0));
		
		ToggleButton playBtn= find("#playButton");
		clickOn("#playButton");
		assertTrue(playBtn.isSelected());
		verifyThat("#prevButton", isEnabled());
		verifyThat("#nextButton", isEnabled());
		Label titleLbl = find("#titleLabel");
		assertTrue(!titleLbl.getText().equals(""));
		Label artistAlbumLbl = find("#artistAlbumLabel");
		assertTrue(!artistAlbumLbl.getText().equals(""));
		assertTrue(playList.getItems().size() == 7);
		
		clickOn("#playQueueButton");
		verifyThat("#titleQueueLabel", hasText("Play Queue"));
		clickOn("#historyQueueButton");
		verifyThat("#titleQueueLabel", hasText("History"));
		assertTrue(playList.getItems().size() == 1);
	}
}
