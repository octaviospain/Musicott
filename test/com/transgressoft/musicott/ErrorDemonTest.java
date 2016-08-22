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

package com.transgressoft.musicott;

import com.transgressoft.musicott.tests.*;
import org.junit.*;
import org.junit.runner.*;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Octavio Calleya
 */
@RunWith (PowerMockRunner.class)
@PrepareForTest (StageDemon.class)
public class ErrorDemonTest extends BaseJavaFxTest {

	@Before
	public void beforeEachTest() {
		PowerMockito.mockStatic(StageDemon.class);
		StageDemon stageDemonMock = mock(StageDemon.class);
		when(stageDemonMock.getMainStage()).thenReturn(testStage);
		when(StageDemon.getInstance()).thenReturn(stageDemonMock);
	}

	@Test
	public void errorDialogMessageTest() throws Exception {
		ErrorDemon errorDemon = ErrorDemon.getInstance();
		errorDemon.showErrorDialog("Error message");

		fail("Still unable to test an Alert layout");
	}

	@Test
	public void errorDialogMessageWithContentTest() throws Exception {
		ErrorDemon errorDemon = ErrorDemon.getInstance();
		errorDemon.showErrorDialog("Error message", "Content message");

		fail("Still unable to test an Alert layout");
	}

	@Test
	public void errorDialogMessageWithContentAndExceptionTest() throws Exception {
		ErrorDemon errorDemon = ErrorDemon.getInstance();
		IllegalArgumentException exception = new IllegalArgumentException("Test exception");
		StackTraceElement[] stackTraceSample = new StackTraceElement[]{new StackTraceElement(getClass().getName(), "method", "file", 31415)};
		exception.setStackTrace(stackTraceSample);

		errorDemon.showErrorDialog("Error message", "Content message", exception);

		fail("Still unable to test an Alert layout");
	}

	@Test
	public void errorDialogWithErrorCollection() throws Exception {
		ErrorDemon errorDemon = ErrorDemon.getInstance();
		List<String> errors = Arrays.asList("Error 1", "Error 2", "Error 3");

		errorDemon.showExpandableErrorsDialog("Error message", "Content message", errors);

		fail("Still unable to test an Alert layout");
	}

	@Test
	public void lastFmErrorDialog() throws Exception {
		ErrorDemon errorDemon = ErrorDemon.getInstance();

		errorDemon.showLastFmErrorDialog("LastFm Error message", "LastFm Error content");

		fail("Still unable to test an Alert layout");
	}
}