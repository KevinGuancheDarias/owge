package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.CriticalAttackBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.UnitBo;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.dto.InterceptableSpeedGroupDto;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithFullRestService;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 * @author Kevin Guanche Darias
 * @since 0.9.0
 *
 */
@RestController
@ApplicationScope
@RequestMapping("admin/unit")
public class AdminUnitRestService implements CrudWithFullRestService<Integer, Unit, UnitBo, UnitDto>,
		WithImageRestServiceTrait<Integer, Unit, UnitDto, UnitBo> {

	@Autowired
	private UnitBo unitBo;

	@Autowired
	private UnitTypeBo unitTypeBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@Autowired
	private ExceptionUtilService exceptionUtilService;

	@Autowired
	private SpeedImpactGroupBo speedImpactGroupBo;

	@Autowired
	private DtoUtilService dtoUtilService;

	@Autowired
	private RequirementBo requirementBo;

	@Autowired
	private CriticalAttackBo criticalAttackBo;

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithRequirementsRestServiceTrait#getRestCrudConfigBuilder()
	 */
	@Override
	public RestCrudConfigBuilder<Integer, Unit, UnitBo, UnitDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, Unit, UnitBo, UnitDto> builder = RestCrudConfigBuilder.create();
		return builder.withBeanFactory(beanFactory).withBoService(unitBo).withDtoClass(UnitDto.class)
				.withEntityClass(Unit.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

	@Override
	public Optional<UnitDto> beforeRequestEnd(UnitDto dto, Unit savedEntity) {
		dto.setRequirements(dtoUtilService.convertEntireArray(RequirementInformationDto.class,
				requirementBo.findRequirements(getObject(), dto.getId())));
		return CrudWithFullRestService.super.beforeRequestEnd(dto, savedEntity);
	}

	@Override
	public Optional<Unit> beforeSave(UnitDto parsedDto, Unit entity) {
		if (parsedDto.getTypeId() == null) {
			throw exceptionUtilService
					.createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_UNIT_TYPE_IS_MANDATORY")
					.build();
		} else {
			entity.setType(unitTypeBo.findByIdOrDie(parsedDto.getTypeId()));
		}
		if (parsedDto.getSpeedImpactGroup() != null && parsedDto.getSpeedImpactGroup().getId() != null) {
			entity.setSpeedImpactGroup(speedImpactGroupBo.findByIdOrDie(parsedDto.getSpeedImpactGroup().getId()));
		}
		if (parsedDto.getCriticalAttack() != null && parsedDto.getCriticalAttack().getId() != null) {
			entity.setCriticalAttack(criticalAttackBo.getOne(parsedDto.getCriticalAttack().getId()));
		}
		entity.setInterceptableSpeedGroups(null);
		CrudWithFullRestService.super.beforeSave(parsedDto, entity);
		return WithImageRestServiceTrait.super.beforeSave(parsedDto, entity);
	}

	/**
	 *
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{unitId}/interceptableSpeedGroups")
	public List<InterceptableSpeedGroupDto> findInterceptables(@PathVariable Integer unitId) {
		return unitBo.findByIdOrDie(unitId).getInterceptableSpeedGroups().stream().map(current -> {
			var dto = new InterceptableSpeedGroupDto();
			dto.dtoFromEntity(current);
			return dto;
		}).collect(Collectors.toList());
	}

	/**
	 *
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@PutMapping("{unitId}/interceptableSpeedGroups")
	public void saveInterceptables(@PathVariable Integer unitId,
								   @RequestBody List<InterceptableSpeedGroupDto> interceptables) {
		unitBo.saveSpeedImpactGroupInterceptors(unitId, interceptables);
	}

	@DeleteMapping("{unitId}/criticalAttack")
	public void unsetCriticalAttack(@PathVariable Integer unitId) {
		var unit = unitBo.findByIdOrDie(unitId);
		unit.setCriticalAttack(null);
		unitBo.save(unit);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.kevinguanchedarias.owgejava.rest.trait.
	 * CrudWithRequirementsRestServiceTrait#getObject()
	 */
	@Override
	public ObjectEnum getObject() {
		return ObjectEnum.UNIT;
	}

}
