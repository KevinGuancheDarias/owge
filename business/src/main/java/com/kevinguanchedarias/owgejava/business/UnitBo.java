package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.InterceptableSpeedGroupDto;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.CriticalAttack;
import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.owgejava.repository.InterceptableSpeedGroupRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

@Service
@AllArgsConstructor
public class UnitBo implements WithNameBo<Integer, Unit, UnitDto>, WithUnlockableBo<Integer, Unit, UnitDto> {
    public static final String UNIT_CACHE_TAG = "unit";

    @Serial
    private static final long serialVersionUID = 8956360591688432113L;

    private final UnitRepository unitRepository;

    private final UnlockedRelationBo unlockedRelationBo;

    private final transient InterceptableSpeedGroupRepository interceptableSpeedGroupRepository;

    private final SpeedImpactGroupBo speedImpactGroupBo;

    private final transient CriticalAttackBo criticalAttackBo;

    private final ObtainedUnitRepository obtainedUnitRepository;

    @Override
    public JpaRepository<Unit, Integer> getRepository() {
        return unitRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<UnitDto> getDtoClass() {
        return UnitDto.class;
    }

    @Override
    public UnlockedRelationBo getUnlockedRelationBo() {
        return unlockedRelationBo;
    }

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.UNIT;
    }

    @Transactional
    public void delete(Integer id) {
        unitRepository.deleteById(id);
    }

    /**
     * Calculates the requirements according to the count to operate!
     *
     * @throws SgtBackendInvalidInputException can't be negative
     * @author Kevin Guanche Darias
     */
    public ResourceRequirementsPojo calculateRequirements(Unit unit, Long count) {
        if (count < 1) {
            throw new SgtBackendInvalidInputException("Input can't be negative");
        }

        var retVal = new ResourceRequirementsPojo();
        retVal.setRequiredPrimary((double) (unit.getPrimaryResource() * count));
        retVal.setRequiredSecondary((double) (unit.getSecondaryResource() * count));
        retVal.setRequiredTime((double) (unit.getTime() * count));
        retVal.setRequiredEnergy((double) (ObjectUtils.firstNonNull(unit.getEnergy(), 0) * count));
        return retVal;
    }

    public boolean isUnique(Unit unit) {
        return unit.getIsUnique();
    }

    /**
     * Checks if the unique unit has been build by the user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void checkIsUniqueBuilt(UserStorage user, Unit unit) {
        if (isUnique(unit) && obtainedUnitRepository.countByUserAndUnit(user, unit) > 0) {
            throw new SgtBackendInvalidInputException(
                    "Unit with id " + unit.getId() + " has been already build by user " + user.getId());
        }

    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    @Transactional
    public void saveSpeedImpactGroupInterceptors(int unitId,
                                                 List<InterceptableSpeedGroupDto> interceptableSpeedGroupDtos) {
        var unit = getOne(unitId);
        interceptableSpeedGroupRepository.deleteByUnit(unit);
        interceptableSpeedGroupDtos.forEach(current -> {
            var interceptableSpeedGroup = new InterceptableSpeedGroup();
            interceptableSpeedGroup.setUnit(unit);
            interceptableSpeedGroup
                    .setSpeedImpactGroup(speedImpactGroupBo.getOne(current.getSpeedImpactGroup().getId()));
            interceptableSpeedGroupRepository.save(interceptableSpeedGroup);
        });
    }

    public CriticalAttack findUsedCriticalAttack(int unitId) {
        var unit = findByIdOrDie(unitId);
        return ObjectUtils.firstNonNull(unit.getCriticalAttack(), criticalAttackBo.findUsedCriticalAttack(unit.getType()));
    }
}
