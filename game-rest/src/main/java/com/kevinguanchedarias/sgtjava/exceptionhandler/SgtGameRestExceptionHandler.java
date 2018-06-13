package com.kevinguanchedarias.sgtjava.exceptionhandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.kevinguanchedarias.kevinsuite.commons.rest.exceptionhandler.RestExceptionHandler;
import com.kevinguanchedarias.sgtjava.exception.AccessDeniedException;
import com.kevinguanchedarias.sgtjava.exception.NotFoundException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;

@ControllerAdvice
public class SgtGameRestExceptionHandler extends RestExceptionHandler {

	@ExceptionHandler({ SgtBackendInvalidInputException.class })
	public ResponseEntity<Object> handleInvalidInput(Exception e, WebRequest request) {
		return handleGameException(e, request, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler({ NotFoundException.class })
	public ResponseEntity<Object> handleNotFoundResource(Exception e, WebRequest request) {
		return handleGameException(e, request, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler({ AccessDeniedException.class })
	public ResponseEntity<Object> handleAccessDenied(Exception e, WebRequest request) {
		return handleGameException(e, request, HttpStatus.FORBIDDEN);
	}
}
