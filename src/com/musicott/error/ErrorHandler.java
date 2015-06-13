package com.musicott.error;

import java.util.ArrayList;
import java.util.List;

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