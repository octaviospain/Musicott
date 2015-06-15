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

package com.musicott.error;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Octavio Calleya
 *
 */
public class ErrorHandler {
	
	private static ErrorHandler instance;
	private List<ParseException> pExcs;

	private ErrorHandler() {
	}
	
	public static ErrorHandler getInstance() {
		if(instance == null)
			instance = new ErrorHandler();
		return instance;
	}
	
	public void addParseException(ParseException e) {
		if(pExcs == null)
			pExcs = new ArrayList<ParseException>();
		pExcs.add(e);
	}
	
	public boolean hasErrors() {
		return pExcs != null && pExcs.size() != 0;
	}
	
	public List<ParseException> getParseExceptions(){
		return pExcs;
	}
}