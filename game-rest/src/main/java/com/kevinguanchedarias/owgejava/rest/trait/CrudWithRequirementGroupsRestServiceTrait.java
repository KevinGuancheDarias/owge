package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.ObjectEntityBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.RequirementGroupBo;
import com.kevinguanchedarias.owgejava.business.RequirementInformationBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.dto.ObjectRelationDto;
import com.kevinguanchedarias.owgejava.dto.RequirementGroupDto;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.kevinguanchedarias.owgejava.entity.RequirementGroup.REQUIREMENT_GROUP_CACHE_TAG;

/**
 * Crud for requirement groups
 *
 * @param <E>
 * @param <R>
 * @param <D>
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
public interface CrudWithRequirementGroupsRestServiceTrait<
        E extends EntityWithRequirementGroups,
        R extends JpaRepository<E, Integer>,
        D extends DtoFromEntity<E>
        > extends CrudRestServiceNoOpEventsTrait<D, E> {

    RestCrudConfigBuilder<Integer, E, R, D> getRestCrudConfigBuilder();

    /**
     * Returns the object the entity <i>E</i> represents in the {@link ObjectEntity}
     * table
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    ObjectEnum getObject();

    /**
     * Test for correct {@link CrudWithRequirementsRestServiceTrait#getObject()}
     * implementation <br>
     * <b>NOTICE:</b> Won't work properly if you don't use @Scope,
     * note @ApplicationScope doesn't work properly
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @PostConstruct
    @Autowired
    default void init() {
        Logger.getLogger(getClass()).debug("Initializing crud with requirements");
        getBeanFactory().getBean(ObjectEntityBo.class).existsByDescriptionOrDie(getObject());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @GetMapping("{id}/requirement-group")
    default List<RequirementGroupDto> findRequirements(@PathVariable Integer id) {
        DtoUtilService dtoUtilService = getBeanFactory().getBean(DtoUtilService.class);
        return dtoUtilService.convertEntireArray(RequirementGroupDto.class,
                SpringRepositoryUtil.findByIdOrDie(getRepository(), id).getRequirementGroups());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostMapping("{id}/requirement-group")
    default RequirementGroupDto addGroup(@PathVariable Integer id,
                                         @RequestBody RequirementGroupDto requirementGroupDto) {
        SpringRepositoryUtil.existsOrDie(getRepository(), id);
        RequirementGroupBo requirementGroupBo = getBeanFactory().getBean(RequirementGroupBo.class);
        return requirementGroupBo.toDto(requirementGroupBo.add(getObject(), id, requirementGroupDto));
    }

    /**
     * Deletes a group
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @DeleteMapping("{id}/requirement-group/{groupId}")
    default ResponseEntity<Void> deleteGroup(@PathVariable Integer id, @PathVariable Integer groupId) {
        var requirementGroupBo = getBeanFactory().getBean(RequirementGroupRepository.class);
        requirementGroupBo.deleteById(groupId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostMapping("{id}/requirement-group/{requirementGroupId}/requirement")
    default RequirementInformationDto addRequirement(@PathVariable Integer id,
                                                     @PathVariable Integer requirementGroupId,
                                                     @RequestBody RequirementInformationDto requirementInformationDto) {
        SpringRepositoryUtil.existsOrDie(getRepository(), id);
        requirementInformationDto
                .setRelation(new ObjectRelationDto(ObjectEnum.REQUIREMENT_GROUP.name(), requirementGroupId));
        var retVal = getBeanFactory().getBean(RequirementBo.class).addRequirementFromDto(requirementInformationDto);
        clearGroupCache();
        return retVal;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @DeleteMapping("{id}/requirement-group/{requirementGroupId}/requirement/{requirementInformationId}")
    default ResponseEntity<Void> deleteRequirement(@PathVariable Integer requirementGroupId, @PathVariable Integer requirementInformationId) {
        getBeanFactory().getBean(RequirementInformationBo.class).delete(requirementInformationId);
        clearGroupCache();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private R getRepository() {
        return getRestCrudConfigBuilder().build().getRepository();
    }

    private BeanFactory getBeanFactory() {
        return getRestCrudConfigBuilder().build().getBeanFactory();
    }

    private void clearGroupCache() {
        getBeanFactory().getBean(TaggableCacheManager.class).evictByCacheTag(REQUIREMENT_GROUP_CACHE_TAG);
    }
}
