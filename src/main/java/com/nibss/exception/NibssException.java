package com.nibss.exception;

public class NibssException extends Exception {

	private static final long serialVersionUID = 1L;

	public NibssException(String errorMessage) {
		super(errorMessage);
	}
	
	public NibssException(Exception e) {
		super(e);
	}

}
