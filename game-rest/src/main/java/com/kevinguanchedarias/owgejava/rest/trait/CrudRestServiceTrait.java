/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.SupportedOperations;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Has basic crud methods used by most of the internal entities <br>
 * <b>Hint:</b> If your RestService class doesn't extend any class you can
 *
 * @param <N> The numeric id type used for the entity (this class doesn't
 *            support non numeric id classes)
 * @param <E> Target entity class <b>MUST implement {@link EntityWithId} </b>
 * @param <D> Target DTO class (used to receive&send from/to the browser as
 *            JSON, <b>MUST implement {@link DtoFromEntity}</b>
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface CrudRestServiceTrait<N extends Number, E extends EntityWithId<N>, R extends JpaRepository<E, N>, D extends DtoFromEntity<E>>
        extends CrudRestServiceNoOpEventsTrait<D, E>, WithReadRestServiceTrait<N, E, R, D>,
        WithDeleteRestServiceTrait<N, E, R, D> {

    @Override
    RestCrudConfigBuilder<N, E, R, D> getRestCrudConfigBuilder();

    /**
     * POST mapping that saves a new entity (expects a DTO as request body)
     *
     * @param entityDto The request body
     * @throws SgtBackendInvalidInputException If the body object has id (it MUST
     *                                         not)
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PostMapping
    @Transactional
    default D saveNew(@RequestBody D entityDto) {
        if (!findSupportedOperations().canCreate()) {
            throw AccessDeniedException.fromUnsupportedOperation();
        }
        var parsedDto = beforeConversion(entityDto).orElse(entityDto);
        var transientEntity = getDtoUtilService().entityFromDto(getEntityClass(), parsedDto);
        var parsedEntity = beforeSave(parsedDto, transientEntity).orElse(transientEntity);
        if (hasId(parsedEntity)) {
            throw new SgtBackendInvalidInputException(
                    "New entities can't have an id, use PUT instead if you wish to update an entity");
        }
        var savedEntity = getRepository().save(parsedEntity);
        var finalDto = getDtoUtilService().dtoFromEntity(getDtoClass(), afterSave(savedEntity).orElse(savedEntity));
        return beforeRequestEnd(finalDto, savedEntity).orElse(finalDto);
    }

    /**
     * PUT mapping to save an existing entity
     *
     * @param id        The path param id
     * @param entityDto request body
     * @throws SgtBackendInvalidInputException If the entity id is not specified, or
     *                                         doesn't match path/body one
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PutMapping("{id}")
    @Transactional
    public default D saveExisting(@PathVariable() N id, @RequestBody D entityDto) {
        if (!findSupportedOperations().canUpdateAny()) {
            throw AccessDeniedException.fromUnsupportedOperation();
        }
        var parsedDto = beforeConversion(entityDto).orElse(entityDto);
        var transientEntity = getDtoUtilService().entityFromDto(getEntityClass(), parsedDto);
        var parsedEntity = beforeSave(parsedDto, transientEntity).orElse(transientEntity);
        if (!hasId(parsedEntity) || !id.equals(parsedEntity.getId())) {
            throw new SgtBackendInvalidInputException("Id not specified, or path id doesn't match body id");
        }
        var savedEntity = getRepository().save(parsedEntity);
        var finalDto = getDtoUtilService().dtoFromEntity(getDtoClass(), afterSave(savedEntity).orElse(savedEntity));
        return beforeRequestEnd(finalDto, savedEntity).orElse(finalDto);
    }

    private boolean hasId(E transientEntity) {
        return transientEntity.getId() != null && !transientEntity.getId().equals(0);
    }

    private DtoUtilService getDtoUtilService() {
        return getBeanFactory().getBean(DtoUtilService.class);
    }

    private R getRepository() {
        return getRestCrudConfigBuilder().build().getRepository();
    }

    private BeanFactory getBeanFactory() {
        return getRestCrudConfigBuilder().build().getBeanFactory();
    }

    private Class<D> getDtoClass() {
        return getRestCrudConfigBuilder().build().getDtoClass();
    }

    private Class<E> getEntityClass() {
        return getRestCrudConfigBuilder().build().getEntityClass();
    }

    private SupportedOperations findSupportedOperations() {
        return getRestCrudConfigBuilder().build().getSupportedOperationsBuilder().build();
    }
}
