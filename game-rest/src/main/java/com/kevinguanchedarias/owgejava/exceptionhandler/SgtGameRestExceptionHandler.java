package com.kevinguanchedarias.owgejava.exceptionhandler;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.kevinguanchedarias.kevinsuite.commons.rest.exceptionhandler.RestExceptionHandler;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;
import com.kevinguanchedarias.owgejava.util.GitUtilService;

@ControllerAdvice
public class SgtGameRestExceptionHandler extends RestExceptionHandler {

	private static final Logger LOG = Logger.getLogger(SgtGameRestExceptionHandler.class);

	@Autowired
	private GitUtilService gitUtilService;

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

	@ExceptionHandler({ ProgrammingException.class })
	public ResponseEntity<Object> handleProgrammingException(ProgrammingException e, WebRequest request) {
		return handleExceptionInternal(e, new GameBackendErrorPojo(e), prepareCommonHeaders(),
				HttpStatus.INTERNAL_SERVER_ERROR, request);
	}

	/**
	 * Handles game exceptions
	 * 
	 * @param e
	 * @param request
	 * @return
	 * @author Kevin Guanche Darias
	 */
	@Override
	protected ResponseEntity<Object> handleGameException(Exception e, WebRequest request) {
		return handleGameException(e, request, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Handles game exceptions <br>
	 * If exception is {@link CommonException} or extends it, will also add the
	 * developerHint
	 * 
	 * @param e
	 * @param request
	 * @return
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected ResponseEntity<Object> handleGameException(Exception e, WebRequest request, HttpStatus status) {
		GameBackendErrorPojo response = new GameBackendErrorPojo(e);
		response.setExceptionType(e.getClass().getSimpleName());
		response.setMessage(e.getMessage());
		if (NotFoundException.class.isInstance(e) || NotFoundException.class.isAssignableFrom(e.getClass())) {
			NotFoundException notFoundException = (NotFoundException) e;
			response.setReporter(ObjectUtils.firstNonNull(notFoundException.getReporter(), NotFoundException.class));
			response.setExtra(notFoundException.getExtra());
			if (StringUtils.isEmpty(notFoundException.getDeveloperHint())) {
				response.setDeveloperHint(gitUtilService.createDocUrl(GameProjectsEnum.BUSINESS,
						NotFoundException.class, DocTypeEnum.EXCEPTIONS, notFoundException.getMessage()));
			} else {
				response.setDeveloperHint(notFoundException.getDeveloperHint());
			}
		} else if (CommonException.class.isInstance(e) || CommonException.class.isAssignableFrom(e.getClass())) {
			CommonException commonException = (CommonException) e;
			response.setDeveloperHint(commonException.getDeveloperHint());
			response.setReporter(commonException.getReporter());
			response.setExtra(commonException.getExtra());
		}
		LOG.debug(e);
		return handleExceptionInternal(e, response, prepareCommonHeaders(), status, request);
	}
}
