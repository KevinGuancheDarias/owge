/**
 *
 */
package com.kevinguanchedarias.owgejava.util;

import java.sql.SQLIntegrityConstraintViolationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.builder.ExceptionBuilder;
import com.kevinguanchedarias.owgejava.exception.CommonException;

/**
 * Eases the usage of {@link ExceptionBuilder} by automatically passing the
 * {@link MavenUtilService}
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class ExceptionUtilService {
	@Autowired
	private GitUtilService gitUtilService;

	/**
	 * Is the input exception a duplicated key exception?
	 *
	 * @param e
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static boolean isSqlDuplicatedKey(Exception e) {
		return e.getCause() != null && e.getCause().getCause() != null
				&& SQLIntegrityConstraintViolationException.class.isInstance(e.getCause().getCause());
	}

	/**
	 * Creates and exception builder with the {@link MavenUtilService} availaable
	 *
	 * @param exceptionClass
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder createExceptionBuilder(Class<? extends CommonException> exceptionClass) {
		return ExceptionBuilder.create(gitUtilService, exceptionClass);
	}

	/**
	 * Creates and exception builder with the {@link MavenUtilService} availaable
	 *
	 * @param exceptionClass
	 * @param message
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder createExceptionBuilder(Class<? extends CommonException> exceptionClass, String message) {
		return ExceptionBuilder.create(gitUtilService, exceptionClass, message);
	}
}
