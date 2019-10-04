/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.ImprovementUnitTypeBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.dto.DtoWithImprovements;
import com.kevinguanchedarias.owgejava.dto.ImprovementDto;
import com.kevinguanchedarias.owgejava.dto.ImprovementUnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;

/**
 * Adds REST Improvement management for given entity
 *
 * @param <N> Entity <b>Numeric</b> id type
 * @param <E> Entity class
 * @param <S> Business service used for crud operations
 * @param <D> DTO class used to build the response, or to build the
 *            "RequestBody" object for POST PUT crud operations
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface CrudWithImprovementsRestServiceTrait<N extends Number, E extends EntityWithImprovements, S extends BaseBo<E>, D extends DtoFromEntity<E>>
		extends CrudRestServiceTrait<N, E, S, D> {

	public ImprovementBo getImprovementBo();

	public ImprovementUnitTypeBo getImprovementUnitTypeBo();

	public UnitTypeBo getUnitTypeBo();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * AdminCrudRestServiceNoOpEventsTrait#beforeRequestEnd(java.lang.Object)
	 */
	@Override
	public default Optional<D> beforeRequestEnd(D dto) {
		DtoWithImprovements withImprovements = (DtoWithImprovements) dto;
		withImprovements.setImprovement(null);
		return CrudRestServiceTrait.super.beforeRequestEnd(dto);
	}

	/**
	 * Finds the current improvement (if any)
	 * 
	 * @param id
	 * @throws NotFoundException If the parent entity doesn't exists, or if it
	 *                           doesn't have an improvement
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{id}/improvement")
	public default ImprovementDto find(@PathVariable Integer id) {
		E entity = findEntityOrDie(id);
		ImprovementDto retVal = new ImprovementDto();
		Improvement improvement = findImprovementOrDie(entity);
		retVal.dtoFromEntity(improvement);
		return retVal;
	}

	/**
	 * Creates or updates the entity's improvement
	 * 
	 * @param id
	 * @param improvementDto
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PutMapping("{id}/improvement")
	public default ImprovementDto saveImprovement(@PathVariable Integer id,
			@RequestBody ImprovementDto improvementDto) {
		E entity = findEntityOrDie(id);
		improvementDto.setUnitTypesUpgrades(null);
		Improvement improvement = getImprovementBo().createOrUpdateFromDto(entity, improvementDto);
		return getDtoUtilService().dtoFromEntity(ImprovementDto.class, improvement);
	}

	/**
	 * Returns the unitTypeImprovements for the given entity improvement
	 * 
	 * @param id
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{id}/improvement/unitTypeImprovements")
	public default List<ImprovementUnitTypeDto> findUnitTypeImprovements(@PathVariable N id) {
		E entity = findEntityOrDie(id);
		Improvement improvement = findImprovementOrDie(entity);
		getImprovementUnitTypeBo().loadImprovementUnitTypes(improvement);
		return getDtoUtilService().convertEntireArray(ImprovementUnitTypeDto.class, improvement.getUnitTypesUpgrades());
	}

	/**
	 * Creates a new unit type improvement
	 * 
	 * @param id
	 * @param improvementUnitTypeDto
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("{id}/improvement/unitTypeImprovements")
	public default ImprovementUnitTypeDto addUnitTypeImprovement(@PathVariable N id,
			@RequestBody ImprovementUnitTypeDto improvementUnitTypeDto) {
		E entity = findEntityOrDie(id);
		ImprovementUnitType improvementUnitTypeEntity = getDtoUtilService().entityFromDto(ImprovementUnitType.class,
				improvementUnitTypeDto);
		improvementUnitTypeEntity.setUnitType(getUnitTypeBo().findByIdOrDie(improvementUnitTypeDto.getUnitTypeId()));
		Improvement improvement = getImprovementUnitTypeBo().add(entity.getImprovement(), improvementUnitTypeEntity);
		getImprovementBo().save(improvement);
		getImprovementUnitTypeBo().loadImprovementUnitTypes(improvement);
		List<ImprovementUnitTypeDto> improvementUnitTypeDtos = getDtoUtilService()
				.convertEntireArray(ImprovementUnitTypeDto.class, improvement.getUnitTypesUpgrades());
		return improvementUnitTypeDtos.stream()
				.filter(current -> getImprovementUnitTypeBo().isSameTarget(current, improvementUnitTypeDto)).findAny()
				.orElse(null);
	}

	/**
	 * Deletes an existing unit type improvement
	 * 
	 * @param id
	 * @param unitTypeImprovementId
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@DeleteMapping("{id}/improvement/unitTypeImprovements/{unitTypeImprovementId}")
	public default ResponseEntity<Void> deleteUnitTypeImprovement(@PathVariable N id,
			@PathVariable Integer unitTypeImprovementId) {
		Improvement improvement = findImprovementOrDie(findEntityOrDie(id));
		getImprovementUnitTypeBo().checkHasUnitTypeImprovementById(improvement.getId(), unitTypeImprovementId);
		getImprovementUnitTypeBo().removeImprovementUnitType(unitTypeImprovementId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private E findEntityOrDie(Number id) {
		E entity = getBo().findById(id);
		if (entity == null) {
			throw new NotFoundException();
		}
		return entity;
	}

	/**
	 * @param entity
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Improvement findImprovementOrDie(E entity) {
		if (entity.getImprovement() == null) {
			throw new NotFoundException("I18N_ERR_NULL_IMPROVEMENT");
		}
		return entity.getImprovement();
	}
}
