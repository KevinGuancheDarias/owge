/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;
import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.rest.AbstractCrudRestService;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 * Has basic crud methods used by most of the internal entities <br>
 * <b>Hint:</b> If your RestService class doesn't extend any class you can
 * extend {@link AbstractCrudRestService} to save some lines
 *
 * @todo Make the owned things work
 * @param <N> The numeric id type used for the entity (this class doesn't
 *            support non numeric id classes)
 * @param <E> Target entity class <b>MUST implement {@link SimpleIdEntity} </b>
 * @param <S> Bo service used for crud operations, <b>MUST implement
 *            {@link BaseBo}</b>
 * @param <D> Target DTO class (used to receive&send from/to the browser as
 *            JSON, <b>MUST implement {@link DtoFromEntity}</b>
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface CrudRestServiceTrait<N extends Number, E extends SimpleIdEntity, S extends BaseBo<E>, D extends DtoFromEntity<E>>
		extends CrudRestServiceNoOpEventsTrait<D, E> {

	/**
	 * Returns the class used as entity
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Class<E> getEntityClass();

	/**
	 * Returns the service associated with the entity (usually "autowired")
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public S getBo();

	/**
	 * Returns the class used as DTO for the entities returned by the Business
	 * object
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Class<D> getDtoClass();

	/**
	 * Returns the service used to convert entities to DTOS <br>
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public DtoUtilService getDtoUtilService();

	/**
	 * GET mapping that returns all entities (converted to the Dto)
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping
	public default List<D> findAll() {
		if (!getSupportedOperationsBuilder().build().canRead()) {
			AccessDeniedException.fromUnsupportedOperation();
		}
		return getDtoUtilService().convertEntireArray(getDtoClass(), getBo().findAll()).stream()
				.map(current -> beforeRequestEnd(current).orElse(current)).collect(Collectors.toList());
	}

	/**
	 * POST mapping that saves a new entity (expects a DTO as request body)
	 * 
	 * @throws SgtBackendInvalidInputException If the body object has id (it MUST
	 *                                         not)
	 * @param entityDto The request body
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping
	@Transactional
	public default D saveNew(@RequestBody D entityDto) {
		if (!getSupportedOperationsBuilder().build().canCreate()) {
			throw AccessDeniedException.fromUnsupportedOperation();
		}
		D parsedDto = beforeConversion(entityDto).orElse(entityDto);
		E transientEntity = getDtoUtilService().entityFromDto(getEntityClass(), parsedDto);
		E parsedEntity = beforeSave(transientEntity).orElse(transientEntity);
		if (hasId(parsedEntity)) {
			throw new SgtBackendInvalidInputException(
					"New entities can't have an id, use PUT instead if you wish to update an entity");
		}
		E savedEntity = getBo().save(parsedEntity);
		D finalDto = getDtoUtilService().dtoFromEntity(getDtoClass(), afterSave(savedEntity).orElse(savedEntity));
		return beforeRequestEnd(finalDto).orElse(finalDto);
	}

	/**
	 * PUT mapping to save an existing entity
	 * 
	 * @throws SgtBackendInvalidInputException If the entity id is not specified, or
	 *                                         doesn't match path/body one
	 * @param id        The path param id
	 * @param entityDto request body
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PutMapping("{id}")
	@Transactional
	public default D saveExisting(@PathVariable() N id, @RequestBody D entityDto) {
		if (!getSupportedOperationsBuilder().build().canUpdateAny()) {
			throw AccessDeniedException.fromUnsupportedOperation();
		}
		D parsedDto = beforeConversion(entityDto).orElse(entityDto);
		E transientEntity = getDtoUtilService().entityFromDto(getEntityClass(), parsedDto);
		E parsedEntity = beforeSave(transientEntity).orElse(transientEntity);
		if (!hasId(parsedEntity) || !id.equals(parsedEntity.getId())) {
			throw new SgtBackendInvalidInputException("Id not specified, or path id doesn't match body id");
		}
		E savedEntity = getBo().save(parsedEntity);
		D finalDto = getDtoUtilService().dtoFromEntity(getDtoClass(), afterSave(savedEntity).orElse(savedEntity));
		return beforeRequestEnd(finalDto).orElse(finalDto);
	}

	/**
	 * DELETE mapping to delete an entity
	 * 
	 * @throws NotFoundException If there is not an entity with the specified
	 *                           <i>id</i>
	 * @param id The path param id
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@DeleteMapping("{id}")
	public default ResponseEntity<Void> delete(@PathVariable() N id) {
		if (!getSupportedOperationsBuilder().build().canDeleteAny()) {
			throw AccessDeniedException.fromUnsupportedOperation();
		}
		getBo().delete(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private boolean hasId(E transientEntity) {
		return transientEntity.getId() != null && !transientEntity.getId().equals(0);
	}
}
