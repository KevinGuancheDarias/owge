/**
 * 
 */
package com.kevinguanchedarias.owgejava.builder;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.pojo.AffectedItem;
import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;
import com.kevinguanchedarias.owgejava.util.GitUtilService;
import com.kevinguanchedarias.owgejava.util.MavenUtilService;

/**
 * Has useful methods to improve creation of game based exception messages
 * 
 * @todo In the future find the git repository from the properties of the
 *       project
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class ExceptionBuilder {
	private static final Logger LOG = Logger.getLogger(ExceptionBuilder.class);

	private GitUtilService gitUtilService;
	private Class<? extends CommonException> exceptionClass;
	private String message;
	private Exception cause;
	private String developerHint;
	private Class<?> reporter;
	private AffectedItem affectedItem;

	public static ExceptionBuilder create(Class<? extends CommonException> exceptionClass) {
		return create(null, exceptionClass);
	}

	public static ExceptionBuilder create(Class<? extends CommonException> exceptionClass, String message) {
		return create(null, exceptionClass, message);
	}

	/**
	 * Creates a builder instance
	 * 
	 * @param gitUtilService {@link MavenUtilService} is required when using a Git
	 *                       MD Doc
	 * @param exceptionClass
	 * @throws ProgrammingException If <i>exceptionClass</i> can NOT be cast to
	 *                              CommonException
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static ExceptionBuilder create(GitUtilService gitUtilService,
			Class<? extends CommonException> exceptionClass) {
		if (!CommonException.class.isAssignableFrom(exceptionClass)) {
			throw new ProgrammingException(
					"Class " + exceptionClass.getName() + " can't be assigned to " + CommonException.class.getName());
		}
		ExceptionBuilder instance = new ExceptionBuilder();
		instance.gitUtilService = gitUtilService;
		instance.exceptionClass = exceptionClass;
		return instance;
	}

	/**
	 * Creates a builder instance
	 * 
	 * @param gitUtilService {@link MavenUtilService} is required when using a Git
	 *                       MD Doc
	 * @param exceptionClass
	 * @param message
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static ExceptionBuilder create(GitUtilService gitUtilService,
			Class<? extends CommonException> exceptionClass, String message) {
		return create(gitUtilService, exceptionClass).withMessage(message);
	}

	private ExceptionBuilder() {
		// Builder uses the static method create, to create an instance
	}

	/**
	 * Returns the exception instance with the specified settings
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CommonException build() {
		CommonException exception;
		try {
			exception = exceptionClass.getDeclaredConstructor(GameBackendErrorPojo.class, Exception.class)
					.newInstance(createGameBackendErrorPojo(), cause);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			LOG.warn(
					"Not able to create exception class " + exceptionClass.getName() + " defaulting to "
							+ CommonException.class.getName() + " this is typically because " + exceptionClass.getName()
							+ " doesn't extend the correct super constructor (having GameBackendErrorPojo, Exception)",
					e);
			exception = new CommonException(createGameBackendErrorPojo(), cause);
		}
		if (affectedItem != null) {
			exception.getExtra().put("affectedItem", affectedItem);
		}
		return exception;
	}

	/**
	 * Adds the exception message
	 * 
	 * @param message
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder withMessage(String message) {
		this.message = message;
		return this;
	}

	/**
	 * Adds the exception <i>developerHint</i>
	 * 
	 * @param developerHint
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder withDeveloperHint(String developerHint) {
		this.developerHint = developerHint;
		return this;
	}

	/**
	 * Adds a developer hint with the following format, where doc <b>by default is
	 * the exception message itself</b>
	 * https://github.com/owner/repo/<i>projectPath</i>/docs/<i>docsPath</i>/<i>doc</i>
	 * 
	 * @param project  The target git project
	 * @param clazz    Class to use as name (Usually the class that throws the
	 *                 exception)
	 * @param docsPath The docs path because it's a custom doc
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder withDeveloperHintDoc(GameProjectsEnum project, Class<?> clazz, DocTypeEnum docsPath) {
		if (StringUtils.isEmpty(message)) {
			throw new ProgrammingException(
					"If doc is ommited, message can't be null (please, define a message, or make sure withMessage() is invoked before this");
		}
		return withDeveloperHintDoc(project, clazz, docsPath, message);
	}

	/**
	 * Adds a developer hint with the following format
	 * https://github.com/owner/repo/<i>projectPath</i>/docs/<i>docsPath</i>/<i>doc</i>
	 * <br>
	 * <b>Notice:</b> Invoking this will invoke too
	 * <code>withReporter(<i>clazz</i>)</code>
	 * 
	 * @param project  The target git project
	 * @param clazz    Class to use as name (Usually the class that throws the
	 *                 exception)
	 * @param docsPath the docs path because it's a custom doc
	 * @param doc      the document path (for example exceptions/invalid_type.md)
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder withDeveloperHintDoc(GameProjectsEnum project, Class<?> clazz, DocTypeEnum docsPath,
			String doc) {
		if (gitUtilService == null) {
			throw new ProgrammingException(
					"When using withDeveloperHintDoc , gitUtilService must be passed to the builder create method");
		}
		withReporter(clazz);
		return withDeveloperHint(gitUtilService.createDocUrl(project, clazz, docsPath, doc));
	}

	/**
	 * Adds the class that reports the exception
	 * 
	 * @param reporter
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder withReporter(Class<?> reporter) {
		this.reporter = reporter;
		return this;
	}

	/**
	 * Adds the affectedItem to the exception extra information
	 * 
	 * @param affectedItem
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ExceptionBuilder withAffectedItem(AffectedItem affectedItem) {
		this.affectedItem = affectedItem;
		return this;
	}

	private GameBackendErrorPojo createGameBackendErrorPojo() {
		return new GameBackendErrorPojo(message, developerHint, reporter);
	}
}
