package com.kevinguanchedarias.owgejava.exceptionhandler;

import com.kevinguanchedarias.kevinsuite.commons.rest.exceptionhandler.RestExceptionHandler;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.exception.*;
import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;
import com.kevinguanchedarias.owgejava.util.GitUtilService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@Slf4j
@AllArgsConstructor
public class SgtGameRestExceptionHandler extends RestExceptionHandler {


    private final GitUtilService gitUtilService;

    @ExceptionHandler({SgtBackendInvalidInputException.class})
    public ResponseEntity<Object> handleInvalidInput(Exception e, WebRequest request) {
        return handleGameException(e, request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<Object> handleNotFoundResource(Exception e, WebRequest request) {
        return handleGameException(e, request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<Object> handleAccessDenied(Exception e, WebRequest request) {
        return handleGameException(e, request, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ProgrammingException.class})
    public ResponseEntity<Object> handleProgrammingException(ProgrammingException e, WebRequest request) {
        return handleExceptionInternal(e, new GameBackendErrorPojo(e), prepareCommonHeaders(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Handles game exceptions
     *
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
     * @author Kevin Guanche Darias
     */
    @Override
    protected ResponseEntity<Object> handleGameException(Exception e, WebRequest request, HttpStatus status) {
        GameBackendErrorPojo response = new GameBackendErrorPojo(e);
        response.setExceptionType(e.getClass().getSimpleName());
        response.setMessage(e.getMessage());
        if (e instanceof NotFoundException notFoundException) {
            response.setReporter(ObjectUtils.firstNonNull(notFoundException.getReporter(), NotFoundException.class));
            response.setExtra(notFoundException.getExtra());
            if (!StringUtils.hasLength(notFoundException.getDeveloperHint())) {
                response.setDeveloperHint(gitUtilService.createDocUrl(GameProjectsEnum.BUSINESS,
                        NotFoundException.class, DocTypeEnum.EXCEPTIONS, notFoundException.getMessage()));
            } else {
                response.setDeveloperHint(notFoundException.getDeveloperHint());
            }
        } else {
            NotFoundException.class.isAssignableFrom(e.getClass());
            if (e instanceof CommonException commonException) {
                response.setDeveloperHint(commonException.getDeveloperHint());
                response.setReporter(commonException.getReporter());
                response.setExtra(commonException.getExtra());
            } else {
                CommonException.class.isAssignableFrom(e.getClass());
            }
        }
        return handleExceptionInternal(e, response, prepareCommonHeaders(), status, request);
    }
}
