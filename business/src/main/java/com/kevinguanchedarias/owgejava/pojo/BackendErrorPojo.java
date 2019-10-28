package com.kevinguanchedarias.owgejava.pojo;

public class BackendErrorPojo {
	private String message;
	private String exceptionType;

	public BackendErrorPojo() {

	}

	public BackendErrorPojo(Exception e) {
		setMessage(e.getMessage());
		setExceptionType(e.getClass().getSimpleName());
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(String exceptionType) {
		this.exceptionType = exceptionType;
	}

}
