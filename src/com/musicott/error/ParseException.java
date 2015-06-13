package com.musicott.error;

import java.io.File;

public class ParseException extends Exception{

	public ParseException() {
		super();
	}
	
	public ParseException(String msg) {
		super(msg);
	}
	
	public ParseException(Throwable cause) {
		super(cause);
	}
	
	public ParseException(String msg, Throwable cause, File f) {
		super(msg+" in "+f.getName(), cause);
	}
}