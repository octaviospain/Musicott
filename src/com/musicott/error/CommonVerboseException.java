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

package com.musicott.error;

import java.util.List;

/**
 * @author Octavio Calleya
 *
 */
public class CommonVerboseException extends Exception {

	private static final long serialVersionUID = 1L;
	private List<String> exceptionMesages;

	public CommonVerboseException() {
		super();
	}
	
	public CommonVerboseException(String msg) {
		super(msg);
	}
	
	public CommonVerboseException(String msg, List<String> exceptionMessages) {
		super(msg);
		this.exceptionMesages = exceptionMessages;
	}
	
	public CommonVerboseException(Throwable cause) {
		super(cause);
	}
	
	public CommonVerboseException(String msg, Throwable cause) {
		super(msg,cause);
	}
	
	public void setExceptionMessages(List<String> exceptionMesages) {
		this.exceptionMesages = exceptionMesages;
	}
	
	public List<String> getExceptionMessages(){
		return this.exceptionMesages;
	}
}