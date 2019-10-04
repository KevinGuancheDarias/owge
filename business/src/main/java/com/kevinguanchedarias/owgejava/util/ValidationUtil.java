package com.kevinguanchedarias.owgejava.util;

import java.util.stream.Stream;

import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;

/**
 *
 * This Util class acts as a "Singleton" and uses the getInstance() pattern
 * (which is not test friendly, but "builder pattern friendly")
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class ValidationUtil {
	private static final ValidationUtil INSTANCE = new ValidationUtil();

	/**
	 * Returns the singleton instance
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static ValidationUtil getInstance() {
		return INSTANCE;
	}

	private ValidationUtil() {
		// An util class doesn't have a constructor
	}

	/**
	 * Checks if the <i>target</i> is null in which case throws
	 * {@link SgtBackendInvalidInputException}
	 * 
	 * @throws SgtBackendInvalidInputException
	 * @param target
	 * @param position "The position to report in the target object, for example {
	 *                 "person": { "name": "Paco" } }, value "person", and
	 *                 "person.name" can be passed as <i>position</i>
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ValidationUtil requireNotNull(Object target, String position) {
		if (target == null) {
			throw new SgtBackendInvalidInputException("Value for " + position + " can't be null");
		}
		return this;
	}

	/**
	 * Checks if the <i>target</i> is null in which case throws
	 * {@link SgtBackendInvalidInputException}
	 * 
	 * @throws SgtBackendInvalidInputException
	 * @param target
	 * @param position "The position to report in the target object, for example {
	 *                 "person": { "name": "Paco" } }, value "person", and
	 *                 "person.name" can be passed as <i>position</i>
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ValidationUtil requireNull(Object target, String position) {
		if (target != null) {
			throw new SgtBackendInvalidInputException("Value for " + position + " MUST not be passed");
		}
		return this;
	}

	/**
	 * Checks if the target <b>is not null</b> and exists in the input enum, throws
	 * {@link SgtBackendInvalidInputException}
	 * 
	 * @throws SgtBackendInvalidInputException
	 * @param target     String to find in targetEnum
	 * @param targetEnum Enum to search the value
	 * @param position   "The position to report in the target object, for example {
	 *                   "person": { "docType": "Passport" } }, value "person", and
	 *                   "person.docType" can be passed as <i>position</i>
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <E extends Enum<E>> ValidationUtil requireValidEnumValue(String target, Class<E> targetEnum,
			String position) {
		requireNotNull(target, position);
		try {
			Enum.valueOf(targetEnum, target);
			return this;
		} catch (IllegalArgumentException e) {
			GameBackendErrorPojo gameBackendErrorPojo = new GameBackendErrorPojo("I18N_ERR_BAD_ENUM_VALUE", "value for "
					+ position + " is invalid: " + target + " is not a valid value for " + targetEnum.getName(),
					getClass());
			SgtBackendInvalidInputException sgtBackendInvalidInputException = new SgtBackendInvalidInputException(
					gameBackendErrorPojo);
			String validValues = String.join(", ",
					Stream.of(targetEnum.getEnumConstants()).map(E::name).toArray(String[]::new));
			throw sgtBackendInvalidInputException.addExtraDeveloperHint("Valid values: " + validValues);

		}
	}

	/**
	 * Checks if the target is <b>is not null</b> and a positive number
	 * 
	 * @throws SgtBackendInvalidInputException If number is 0 or negative
	 * @param target
	 * @param position
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ValidationUtil requirePositiveNumber(Integer target, String position) {
		requireNotNull(target, position);
		if (target <= 0) {
			throw new SgtBackendInvalidInputException(
					"Value for " + position + " must be a pssitive greater than zero number");
		}
		return this;
	}
}
