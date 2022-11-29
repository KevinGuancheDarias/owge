package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.WithToDtoTrait;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.loader.UnitDataLoader;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
public class ObtainedUnitFinderBo implements WithToDtoTrait<ObtainedUnit, ObtainedUnitDto> {
    private final List<UnitDataLoader> unitDataLoaders;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final EntityManager entityManager;
    private final HiddenUnitBo hiddenUnitBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;

    @Override
    public Class<ObtainedUnitDto> getDtoClass() {
        return ObtainedUnitDto.class;
    }

    public List<ObtainedUnitDto> findCompletedAsDto(UserStorage user) {
        return findCompletedAsDto(user, obtainedUnitRepository.findDeployedInUserOwnedPlanets(user.getId()));
    }

    public List<ObtainedUnitDto> findCompletedAsDto(UserStorage user, List<ObtainedUnit> entities) {
        entities.stream()
                .filter(ou -> ou.getUnit().getSpeedImpactGroup() != null)
                .map(ObtainedUnit::getUnit)
                .forEach(unit -> {
                    Hibernate.initialize(unit.getSpeedImpactGroup());
                    unit.getSpeedImpactGroup().setRequirementGroups(null);
                });
        entities.forEach(current -> {
            var unit = current.getUnit();
            Hibernate.initialize(unit.getInterceptableSpeedGroups());
            entityManager.detach(unit);
            unit.setIsInvisible(hiddenUnitBo.isHiddenUnit(current));
        });
        entities
                .stream()
                .map(ObtainedUnit::getUnit)
                .filter(unit -> unit.getSpeedImpactGroup() == null)
                .forEach(
                        unitWithNullSpeedImpact -> {
                            unitWithNullSpeedImpact.setSpeedImpactGroup(speedImpactGroupFinderBo.findApplicable(user, unitWithNullSpeedImpact));
                            unitWithNullSpeedImpact.getSpeedImpactGroup().setRequirementGroups(null);
                        }
                );
        var dtoList = toDto(entities);
        loadNonJpaData(entities, dtoList);
        return dtoList;
    }

    public List<ObtainedUnit> findInPlanetOrInMissionToPlanet(Planet planet) {
        List<ObtainedUnit> retVal = new ArrayList<>();
        retVal.addAll(obtainedUnitRepository.findBySourcePlanetIdAndMissionIsNull(planet.getId()));
        retVal.addAll(obtainedUnitRepository.findByTargetPlanetIdAndMissionTypeCode(planet.getId(),
                MissionType.DEPLOYED.toString()));
        return retVal;
    }

    /**
     * Finds the involved units in an attack
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public List<ObtainedUnit> findInvolvedInAttack(Planet attackedPlanet) {
        List<ObtainedUnit> retVal = new ArrayList<>();
        List<String> allowedMissions = new ArrayList<>();
        allowedMissions.add(MissionType.CONQUEST.name());
        retVal.addAll(findInPlanetOrInMissionToPlanet(attackedPlanet));
        retVal.addAll(obtainedUnitRepository.findByTargetPlanetIdWhereReferencePercentageTimePassed(attackedPlanet.getId(), 0.1d,
                allowedMissions, new Date()));
        return retVal;
    }

    private void loadNonJpaData(List<ObtainedUnit> entities, List<ObtainedUnitDto> dtoList) {
        IntStream.range(0, dtoList.size()).forEach(i -> {
            var entity = entities.get(i);
            var dto = dtoList.get(i);
            unitDataLoaders.forEach(loader -> loader.addInformationToDto(entity, dto));
        });
    }
}
