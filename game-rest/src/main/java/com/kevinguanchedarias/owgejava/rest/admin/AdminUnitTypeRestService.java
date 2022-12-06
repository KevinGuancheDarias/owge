/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.admin;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.*;
import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@RestController
@ApplicationScope
@RequestMapping("admin/unit_type")
public class AdminUnitTypeRestService implements CrudRestServiceTrait<Integer, UnitType, UnitTypeRepository, UnitTypeDto> {

    @Autowired
    private UnitTypeRepository unitTypeRepository;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private SpeedImpactGroupBo speedImpactGroupBo;

    @Autowired
    private AttackRuleBo attackRuleBo;

    @Autowired
    private CriticalAttackBo criticalAttackBo;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<UnitType> beforeSave(UnitTypeDto parsedDto, UnitType entity) {
        if (parsedDto.getImage() != null) {
            entity.setImage(getRestCrudConfigBuilder().build().getBeanFactory().getBean(ImageStoreBo.class)
                    .findByIdOrDie(parsedDto.getImage()));
        }
        if (parsedDto.getSpeedImpactGroup() != null && parsedDto.getSpeedImpactGroup().getId() != null) {
            entity.setSpeedImpactGroup(speedImpactGroupBo.findByIdOrDie(parsedDto.getSpeedImpactGroup().getId()));
        }
        if (parsedDto.getAttackRule() != null && parsedDto.getAttackRule().getId() != null) {
            entity.setAttackRule(attackRuleBo.findByIdOrDie(parsedDto.getAttackRule().getId()));
        }
        if (parsedDto.getCriticalAttack() != null && parsedDto.getCriticalAttack().getId() != null) {
            entity.setCriticalAttack(criticalAttackBo.getOne(parsedDto.getCriticalAttack().getId()));
        }

        if (parsedDto.getParent() != null && parsedDto.getParent().getId() != null) {
            entity.setParent(SpringRepositoryUtil.findByIdOrDie(unitTypeRepository, parsedDto.getParent().getId()));
        }
        if (parsedDto.getShareMaxCount() != null && parsedDto.getShareMaxCount().getId() != null) {
            entity.setShareMaxCount(SpringRepositoryUtil.findByIdOrDie(unitTypeRepository, parsedDto.getShareMaxCount().getId()));
        }
        return CrudRestServiceTrait.super.beforeSave(parsedDto, entity);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceTrait#
     * getRestCrudConfigBuilder()
     */
    @Override
    public RestCrudConfigBuilder<Integer, UnitType, UnitTypeRepository, UnitTypeDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, UnitType, UnitTypeRepository, UnitTypeDto> builder = RestCrudConfigBuilder.create();
        return builder.withBeanFactory(beanFactory).withRepository(unitTypeRepository).withDtoClass(UnitTypeDto.class)
                .withEntityClass(UnitType.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @DeleteMapping("{unitTypeId}/attackRule")
    public void unsetAttackRule(@PathVariable Integer unitTypeId) {
        var unitType = SpringRepositoryUtil.findByIdOrDie(unitTypeRepository, unitTypeId);
        unitType.setAttackRule(null);
        unitTypeRepository.save(unitType);
    }

    @DeleteMapping("{unitTypeId}/criticalAttack")
    public void unsetCriticalAttack(@PathVariable Integer unitTypeId) {
        var unitType = SpringRepositoryUtil.findByIdOrDie(unitTypeRepository, unitTypeId);
        unitType.setCriticalAttack(null);
        unitTypeRepository.save(unitType);
    }
}
