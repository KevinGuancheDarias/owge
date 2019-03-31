package com.kevinguanchedarias.owgejava.exception;

public class SgtException extends RuntimeException{

	private static final long serialVersionUID = -9075820321876068073L;
	
	public SgtException(String message){
		super(message);
	}
	
	public SgtException(String message,Exception e){
		super(message,e);
	}

}
