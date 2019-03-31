package com.kevinguanchedarias.owgejava.exceptionhandler;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.kevinguanchedarias.kevinsuite.commons.rest.exceptionhandler.RestExceptionHandler;
import com.kevinguanchedarias.owgejava.exception.UserLoginException;

@ControllerAdvice
public class SgtAccountRestExceptionHandler extends RestExceptionHandler {

	@ExceptionHandler({ UserLoginException.class })
	public ResponseEntity<Object> handleLoginException(RuntimeException e, WebRequest request) {
		return handleGameException(e, request);
	}
}
