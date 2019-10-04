/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.Optional;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;

/**
 * Defines the default actions for the events (and the supportedOperations
 *
 * @param <D> Dto class
 * @param <E> Entity class
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface CrudRestServiceNoOpEventsTrait<D, E> {

	public SupportedOperationsBuilder getSupportedOperationsBuilder();

	/**
	 * Can be used to transform the DTO prior to converting it to an entity
	 * 
	 * @param dto
	 * @return It's really optional
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default Optional<D> beforeConversion(D dto) {
		// By default do nothing before conversion
		return Optional.of(dto);
	}

	/**
	 * Can be used to transform the entity <br>
	 * <b>NOTICE:</b> if {@link CrudRestServiceTrait} is used, probably the method
	 * is inside a transaction
	 * 
	 * @param entity An entity in a transient state (It's <b>not</b> recommended to
	 *               invoke the save action from inside)
	 * @return It's really optional
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.SUPPORTS)
	public default Optional<E> beforeSave(E entity) {
		// By default do nothing before save
		return Optional.of(entity);
	}

	/**
	 * Can be used after the entity has been saved
	 * 
	 * @param entity An entity in a <b>persistent</b> state and probably within a
	 *               transaction
	 * @return It's really optional
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.SUPPORTS)
	public default Optional<E> afterSave(E entity) {
		// By default do nothing after save
		return Optional.of(entity);
	}

	/**
	 * Can be used to transform the DTO before sending it over the wire
	 * 
	 * @param dto
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default Optional<D> beforeRequestEnd(D dto) {
		// By default do not <b>transform</b> the dto prior to submission
		return Optional.of(dto);
	}

}
