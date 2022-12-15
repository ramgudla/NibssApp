package com.nibbs.exception;

public class NibbsException extends Exception {

	private static final long serialVersionUID = 1L;

	public NibbsException(String errorMessage) {
		super(errorMessage);
	}
	
	public NibbsException(Exception e) {
		super(e);
	}

}
