package com.kevinguanchedarias.owgejava.exception;

public class SgtInvalidInputException extends SgtException{
	
	private static final long serialVersionUID = 7803777181397697297L;

	public SgtInvalidInputException(String message){
		super(message);
	}
	
	public SgtInvalidInputException(String message,Exception e){
		super(message,e);
	}
}
