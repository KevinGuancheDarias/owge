package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
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
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Adds REST Improvement management for given entity
 *
 * @param <N> Entity <b>Numeric</b> id type
 * @param <E> Entity class
 * @param <S> Business service used for crud operations
 * @param <D> DTO class used to build the response, or to build the
 *            "RequestBody" object for POST PUT crud operations
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface CrudWithImprovementsRestServiceTrait<N extends Number, E extends EntityWithImprovements<N>, S extends BaseBo<N, E, D>, D extends DtoFromEntity<E>>
        extends CrudRestServiceTrait<N, E, S, D> {

    /**
     * Config
     *
     * @since 0.8.0
     */
    @Override
    RestCrudConfigBuilder<N, E, S, D> getRestCrudConfigBuilder();

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.
     * AdminCrudRestServiceNoOpEventsTrait#beforeRequestEnd(java.lang.Object)
     */
    @Override
    default Optional<D> beforeRequestEnd(D dto, E savedEntity) {
        DtoWithImprovements withImprovements = (DtoWithImprovements) dto;
        withImprovements.setImprovement(null);
        return CrudRestServiceTrait.super.beforeRequestEnd(dto, savedEntity);
    }

    @Override
    default Optional<E> beforeSave(D parsedDto, E entity) {
        if (entity.getId() != null && entity.getImprovement() == null) {
            entity.setImprovement(getBo().findById(entity.getId()).getImprovement());
        }
        if (entity.getImprovement() == null) {
            entity.setImprovement(getBeanFactory().getBean(ImprovementBo.class).save(new Improvement()));
        }
        return CrudRestServiceTrait.super.beforeSave(parsedDto, entity);
    }

    /**
     * Finds the current improvement (if any)
     *
     * @throws NotFoundException If the parent entity doesn't exists, or if it
     *                           doesn't have an improvement
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @GetMapping("{id}/improvement")
    default ImprovementDto find(@PathVariable N id) {
        var entity = findEntityOrDie(id);
        var retVal = new ImprovementDto();
        var improvement = findImprovementOrDie(entity);
        retVal.dtoFromEntity(improvement);
        return retVal;
    }

    /**
     * Creates or updates the entity's improvement
     *
     * @todo In the future refactor not to use the controller for the saving of the
     * entity
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @Transactional
    @PutMapping("{id}/improvement")
    @SuppressWarnings("unchecked")
    default ImprovementDto saveImprovement(@PathVariable N id, @RequestBody ImprovementDto improvementDto) {
        var entity = findEntityOrDie(id);
        improvementDto.setUnitTypesUpgrades(null);
        var improvement = getBeanFactory().getBean(ImprovementBo.class)
                .createOrUpdateFromDto((EntityWithImprovements<Number>) entity, improvementDto);
        getBo().save(entity);
        return getDtoUtilService().dtoFromEntity(ImprovementDto.class, improvement);
    }

    /**
     * Returns the unitTypeImprovements for the given entity improvement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @GetMapping("{id}/improvement/unitTypeImprovements")
    default List<ImprovementUnitTypeDto> findUnitTypeImprovements(@PathVariable N id) {
        var entity = findEntityOrDie(id);
        var improvement = findImprovementOrDie(entity);
        getBeanFactory().getBean(ImprovementUnitTypeBo.class).loadImprovementUnitTypes(improvement);
        return getDtoUtilService().convertEntireArray(ImprovementUnitTypeDto.class, improvement.getUnitTypesUpgrades());
    }

    /**
     * Creates a new unit type improvement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PostMapping("{id}/improvement/unitTypeImprovements")
    default ImprovementUnitTypeDto addUnitTypeImprovement(@PathVariable N id,
                                                          @RequestBody ImprovementUnitTypeDto improvementUnitTypeDto) {
        var entity = findEntityOrDie(id);
        var improvementUnitTypeEntity = getDtoUtilService().entityFromDto(ImprovementUnitType.class,
                improvementUnitTypeDto);
        improvementUnitTypeEntity.setUnitType(
                getBeanFactory().getBean(UnitTypeBo.class).findByIdOrDie(improvementUnitTypeDto.getUnitTypeId()));
        var improvementUnitTypeBo = getBeanFactory().getBean(ImprovementUnitTypeBo.class);
        var improvement = improvementUnitTypeBo.add(entity.getImprovement(), improvementUnitTypeEntity);
        getBeanFactory().getBean(ImprovementBo.class).save(improvement);
        improvementUnitTypeBo.loadImprovementUnitTypes(improvement);
        List<ImprovementUnitTypeDto> improvementUnitTypeDtos = getDtoUtilService()
                .convertEntireArray(ImprovementUnitTypeDto.class, improvement.getUnitTypesUpgrades());
        return improvementUnitTypeDtos.stream()
                .filter(current -> improvementUnitTypeBo.isSameTarget(current, improvementUnitTypeDto)).findAny()
                .orElse(null);
    }

    /**
     * Deletes an existing unit type improvement
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @DeleteMapping("{id}/improvement/unitTypeImprovements/{unitTypeImprovementId}")
    default ResponseEntity<Void> deleteUnitTypeImprovement(@PathVariable N id,
                                                           @PathVariable Integer unitTypeImprovementId) {
        var improvementUnitTypeBo = getBeanFactory().getBean(ImprovementUnitTypeBo.class);
        var improvement = findImprovementOrDie(findEntityOrDie(id));
        improvementUnitTypeBo.checkHasUnitTypeImprovementById(improvement.getId(), unitTypeImprovementId);
        improvementUnitTypeBo.removeImprovementUnitType(unitTypeImprovementId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private E findEntityOrDie(N id) {
        var entity = getBo().findById(id);
        if (entity == null) {
            throw new NotFoundException();
        }
        return entity;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    private Improvement findImprovementOrDie(E entity) {
        if (entity.getImprovement() == null) {
            throw new NotFoundException("I18N_ERR_NULL_IMPROVEMENT");
        }
        return entity.getImprovement();
    }

    private DtoUtilService getDtoUtilService() {
        return getBeanFactory().getBean(DtoUtilService.class);
    }

    private S getBo() {
        return getRestCrudConfigBuilder().build().getBoService();
    }

    private BeanFactory getBeanFactory() {
        return getRestCrudConfigBuilder().build().getBeanFactory();
    }
}
