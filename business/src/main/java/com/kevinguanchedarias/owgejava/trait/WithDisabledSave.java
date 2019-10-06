/**
 * 
 */
package com.kevinguanchedarias.owgejava.trait;

import java.util.List;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;

/**
 * Disables the save actions of {@link BaseBo}
 * 
 * @param <E>
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface WithDisabledSave<E> {

	public default E save(E entity) {
		throw new ProgrammingException(
				"The save action is not allowed, tried to save one of " + entity.getClass().getName());
	}

	public default void save(List<E> entities) {
		String targetClassName = entities.isEmpty() ? "unknown" : entities.get(0).getClass().getName();
		throw new ProgrammingException("The save action is not allowed, tried to save multiple of " + targetClassName);
	}

	public default E saveAndFlush(E entity) {
		throw new ProgrammingException(
				"The save action is not allowed, tried to save one of " + entity.getClass().getName());
	}
}
