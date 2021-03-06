package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.BaseBo;
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
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 * Crud for requirement groups
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 * @param <N>
 * @param <E>
 * @param <S>
 * @param <D>
 */
public interface CrudWithRequirementGroupsRestServiceTrait<E extends EntityWithRequirementGroups, S extends BaseBo<Integer, E, D>, D extends DtoFromEntity<E>>
		extends CrudRestServiceNoOpEventsTrait<D, E> {

	public RestCrudConfigBuilder<Integer, E, S, D> getRestCrudConfigBuilder();

	/**
	 * Returns the object the entity <i>E</i> represents in the {@link ObjectEntity}
	 * table
	 *
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectEnum getObject();

	/**
	 * Test for correct {@link CrudWithRequirementsRestServiceTrait#getObject()}
	 * implementation <br>
	 * <b>NOTICE:</b> Won't work properly if you don't use @Scope,
	 * note @ApplicationScope doesn't work properly
	 *
	 * @param objectEntityBo
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostConstruct
	@Autowired
	public default void init() {
		Logger.getLogger(getClass()).debug("Initializing crud with requirements");
		getBeanFactory().getBean(ObjectEntityBo.class).existsByDescriptionOrDie(getObject());
	}

	/**
	 *
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{id}/requirement-group")
	public default List<RequirementGroupDto> findRequirements(@PathVariable Integer id) {
		DtoUtilService dtoUtilService = getBeanFactory().getBean(DtoUtilService.class);
		return dtoUtilService.convertEntireArray(RequirementGroupDto.class,
				getBo().findByIdOrDie(id).getRequirementGroups());
	}

	/**
	 *
	 * @param id
	 * @param requirementGroupDto
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("{id}/requirement-group")
	public default RequirementGroupDto addGroup(@PathVariable Integer id,
			@RequestBody RequirementGroupDto requirementGroupDto) {
		getBo().existsOrDie(id);
		RequirementGroupBo requirementGroupBo = getBeanFactory().getBean(RequirementGroupBo.class);
		return requirementGroupBo.toDto(requirementGroupBo.addRequirementGroup(getObject(), id, requirementGroupDto));
	}

	/**
	 * Deletes a group
	 *
	 * @param id
	 * @param groupId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@DeleteMapping("{id}/requirement-group/{groupId}")
	public default ResponseEntity<Void> deleteGroup(@PathVariable Integer id, @PathVariable Integer groupId) {
		RequirementGroupBo requirementGroupBo = getBeanFactory().getBean(RequirementGroupBo.class);
		requirementGroupBo.delete(groupId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	/**
	 *
	 * @param id
	 * @param requirementGroupId
	 * @param requirementInformationDto
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PostMapping("{id}/requirement-group/{requirementGroupId}/requirement")
	public default RequirementInformationDto addRequirement(@PathVariable Integer id,
			@PathVariable Integer requirementGroupId,
			@RequestBody RequirementInformationDto requirementInformationDto) {
		getBo().existsOrDie(id);
		requirementInformationDto
				.setRelation(new ObjectRelationDto(ObjectEnum.REQUIREMENT_GROUP.name(), requirementGroupId));
		return getBeanFactory().getBean(RequirementBo.class).addRequirementFromDto(requirementInformationDto);
	}

	/**
	 *
	 * @param requirementInformationId
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@DeleteMapping("{id}/requirement-group/{requirementGroupId}/requirement/{requirementInformationId}")
	public default ResponseEntity<Void> deleteRequirement(@PathVariable Integer requirementInformationId) {
		getBeanFactory().getBean(RequirementInformationBo.class).delete(requirementInformationId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private S getBo() {
		return getRestCrudConfigBuilder().build().getBoService();
	}

	private BeanFactory getBeanFactory() {
		return getRestCrudConfigBuilder().build().getBeanFactory();
	}
}
