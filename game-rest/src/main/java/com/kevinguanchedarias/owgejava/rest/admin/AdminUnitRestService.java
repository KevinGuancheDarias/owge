package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.*;
import com.kevinguanchedarias.owgejava.dto.InterceptableSpeedGroupDto;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.rest.trait.CrudWithFullRestService;
import com.kevinguanchedarias.owgejava.rest.trait.WithImageRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Optional;

/**
 * @author Kevin Guanche Darias
 * @since 0.9.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/unit")
@AllArgsConstructor
public class AdminUnitRestService implements CrudWithFullRestService<Integer, Unit, UnitRepository, UnitDto>,
        WithImageRestServiceTrait<Integer, Unit, UnitDto, UnitRepository> {

    private final UnitRepository unitRepository;
    private final UnitBo unitBo;
    private final UnitTypeBo unitTypeBo;
    private final AutowireCapableBeanFactory beanFactory;
    private final ExceptionUtilService exceptionUtilService;
    private final SpeedImpactGroupBo speedImpactGroupBo;
    private final DtoUtilService dtoUtilService;
    private final RequirementInformationBo requirementInformationBo;
    private final CriticalAttackBo criticalAttackBo;

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.
     * CrudWithRequirementsRestServiceTrait#getRestCrudConfigBuilder()
     */
    @Override
    public RestCrudConfigBuilder<Integer, Unit, UnitRepository, UnitDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, Unit, UnitRepository, UnitDto> builder = RestCrudConfigBuilder.create();
        return builder.withBeanFactory(beanFactory).withRepository(unitRepository).withDtoClass(UnitDto.class)
                .withEntityClass(Unit.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    @Override
    public Optional<UnitDto> beforeRequestEnd(UnitDto dto, Unit savedEntity) {
        dto.setRequirements(dtoUtilService.convertEntireArray(RequirementInformationDto.class,
                requirementInformationBo.findRequirements(getObject(), dto.getId())));
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    @GetMapping("{unitId}/interceptableSpeedGroups")
    public List<InterceptableSpeedGroupDto> findInterceptables(@PathVariable Integer unitId) {
        return SpringRepositoryUtil.findByIdOrDie(unitRepository, unitId).getInterceptableSpeedGroups().stream().map(current -> {
            var dto = new InterceptableSpeedGroupDto();
            dto.dtoFromEntity(current);
            return dto;
        }).toList();
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    @PutMapping("{unitId}/interceptableSpeedGroups")
    public void saveInterceptables(@PathVariable Integer unitId,
                                   @RequestBody List<InterceptableSpeedGroupDto> interceptables) {
        unitBo.saveSpeedImpactGroupInterceptors(unitId, interceptables);
    }

    @DeleteMapping("{unitId}/criticalAttack")
    public void unsetCriticalAttack(@PathVariable Integer unitId) {
        var unit = SpringRepositoryUtil.findByIdOrDie(unitRepository, unitId);
        unit.setCriticalAttack(null);
        unitRepository.save(unit);
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
