package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class MissionTimeManagerBo {
    private final MissionConfigurationBo missionConfigurationBo;
    private final ImprovementBo improvementBo;
    private final ConfigurationBo configurationBo;

    public LocalDateTime computeTerminationDate(Double requiredTime) {
        return LocalDateTime.now(ZoneOffset.UTC).plusSeconds(requiredTime.intValue());
    }

    /**
     * Calculates time required to complete the mission
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public Double calculateRequiredTime(MissionType type) {
        return (double) missionConfigurationBo.findMissionBaseTimeByType(type);
    }

    /**
     * Alters the mission and adds the required time and the termination date
     */
    public void handleMissionTimeCalculation(List<ObtainedUnit> obtainedUnits, Mission mission, MissionType missionType) {
        if (!obtainedUnits.stream().allMatch(obtainedUnit -> obtainedUnit.getUnit().getSpeedImpactGroup() != null
                && obtainedUnit.getUnit().getSpeedImpactGroup().getIsFixed())) {
            var user = mission.getUser();
            Optional<Double> lowestSpeedOptional = obtainedUnits.stream().map(ObtainedUnit::getUnit)
                    .filter(unit -> unit.getSpeed() != null && unit.getSpeed() > 0.000D
                            && (unit.getSpeedImpactGroup() == null || !unit.getSpeedImpactGroup().getIsFixed()))
                    .map(Unit::getSpeed).reduce((a, b) -> a > b ? b : a);
            if (lowestSpeedOptional.isPresent()) {
                double lowestSpeed = lowestSpeedOptional.get();
                var unitType = obtainedUnits.stream()
                        .map(ObtainedUnit::getUnit)
                        .filter(unit -> lowestSpeed == unit.getSpeed())
                        .map(Unit::getType)
                        .findFirst()
                        .orElseThrow(() -> new ProgrammingException("Should never ever happend, you know"));
                var improvement = improvementBo.findUserImprovement(user);
                var speedWithImprovement = lowestSpeed + (lowestSpeed * improvementBo.findAsRational(
                        (double) improvement.findUnitTypeImprovement(ImprovementTypeEnum.SPEED, unitType)));
                double missionTypeTime = calculateRequiredTime(missionType);
                double requiredTime = calculateTimeUsingSpeed(mission, missionType, missionTypeTime, speedWithImprovement);
                mission.setRequiredTime(requiredTime);
                mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
            }
        }
    }

    public void handleCustomDuration(Mission mission, Long customDuration) {
        if (customDuration != null && customDuration > mission.getRequiredTime()) {
            mission.setRequiredTime(customDuration.doubleValue());
            mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
        }
    }

    private double calculateTimeUsingSpeed(Mission mission, MissionType missionType, double missionTypeTime,
                                           double lowestUnitSpeed) {
        int missionTypeDivisor = findMissionTypeDivisor(missionType);
        missionTypeDivisor = missionTypeDivisor == 0 ? 1 : missionTypeDivisor;
        int leftMultiplier = findSpeedLeftMultiplier(mission, missionType);
        float moveCost = calculateMoveCost(missionType, mission.getSourcePlanet(), mission.getTargetPlanet());
        double retVal = missionTypeTime + ((leftMultiplier * moveCost) * (100 - lowestUnitSpeed)) / missionTypeDivisor;
        return Math.max(missionTypeTime, retVal);
    }

    private int findMissionTypeDivisor(MissionType missionType) {
        return Integer.parseInt(
                configurationBo.findOrSetDefault("MISSION_SPEED_DIVISOR_" + missionType.name(), "1").getValue());
    }

    /**
     * Finds the speed left multiplier <b>also known as the "mission penalty"</b>
     * which depends of the mission type and if it's on different quadrant
     */
    private int findSpeedLeftMultiplier(Mission mission, MissionType missionType) {
        final String prefix = "MISSION_SPEED_";
        String missionTypeName = missionType.name();
        Long sourceQuadrant = mission.getSourcePlanet().getQuadrant();
        Long targetQuadrant = mission.getTargetPlanet().getQuadrant();
        Long sourceSector = mission.getSourcePlanet().getSector();
        Long targetSector = mission.getTargetPlanet().getSector();
        Integer sourceGalaxy = mission.getSourcePlanet().getGalaxy().getId();
        Integer targetGalaxy = mission.getTargetPlanet().getGalaxy().getId();
        int defaultMultiplier;
        String configurationName;
        if (sourceQuadrant.equals(targetQuadrant) && sourceSector.equals(targetSector)
                && sourceGalaxy.equals(targetGalaxy)) {
            configurationName = prefix + missionTypeName + "_SAME_Q";
            defaultMultiplier = 50;
        } else if (!sourceGalaxy.equals(targetGalaxy)) {
            configurationName = prefix + missionTypeName + "_DIFF_G";
            defaultMultiplier = 2000;
        } else if (!sourceSector.equals(targetSector)) {
            configurationName = prefix + missionTypeName + "_DIFF_S";
            defaultMultiplier = 200;
        } else {
            configurationName = prefix + missionTypeName + "_DIFF_Q";
            defaultMultiplier = 100;
        }
        return NumberUtils.toInt(
                configurationBo.findOrSetDefault(configurationName, String.valueOf(defaultMultiplier)).getValue(),
                defaultMultiplier);
    }

    private float calculateMoveCost(MissionType missionType, Planet sourcePlanet, Planet targetPlanet) {
        final String prefix = "MISSION_SPEED_";
        String missionTypeName = missionType.name();
        long positionInQuadrant = Math.abs(sourcePlanet.getPlanetNumber() - targetPlanet.getPlanetNumber());
        long quadrants = Math.abs(sourcePlanet.getQuadrant() - targetPlanet.getQuadrant());
        long sectors = Math.abs(sourcePlanet.getSector() - targetPlanet.getSector());
        float planetDiff = NumberUtils.toFloat(
                configurationBo.findOrSetDefault(prefix + missionTypeName + "_P_MOVE_COST", "0.01").getValue(), 0.01f);
        float quadrantDiff = NumberUtils.toFloat(
                configurationBo.findOrSetDefault(prefix + missionTypeName + "_Q_MOVE_COST", "0.02").getValue(), 0.02f);
        float sectorDiff = NumberUtils.toFloat(
                configurationBo.findOrSetDefault(prefix + missionTypeName + "_S_MOVE_COST", "0.03").getValue(), 0.03f);
        float galaxyDiff = NumberUtils.toFloat(
                configurationBo.findOrSetDefault(prefix + missionTypeName + "_G_MOVE_COST", "0.15").getValue(), 0.15f);
        return (positionInQuadrant * planetDiff) + (quadrants * quadrantDiff) + (sectors * sectorDiff)
                + (!targetPlanet.getGalaxy().getId().equals(sourcePlanet.getGalaxy().getId()) ? galaxyDiff : 0);
    }
}
