package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.mission.MissionTimeManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@AllArgsConstructor
public class MissionRegistrationPreparer {
    private final UserStorageRepository userStorageRepository;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final PlanetRepository planetRepository;
    private final MissionTypeBo missionTypeBo;

    public Mission prepareMission(UnitMissionInformation missionInformation, MissionType type) {
        Mission retVal = new Mission();
        retVal.setStartingDate(LocalDateTime.now(ZoneOffset.UTC));
        Double requiredTime = missionTimeManagerBo.calculateRequiredTime(type);
        retVal.setMissionInformation(null);
        retVal.setType(missionTypeBo.find(type));
        retVal.setUser(userStorageRepository.getById(missionInformation.getUserId()));
        retVal.setRequiredTime(requiredTime);
        Long sourcePlanetId = missionInformation.getSourcePlanetId();
        Long targetPlanetId = missionInformation.getTargetPlanetId();
        if (sourcePlanetId != null) {
            retVal.setSourcePlanet(planetRepository.getById(sourcePlanetId));
        }
        if (targetPlanetId != null) {
            retVal.setTargetPlanet(planetRepository.getById(targetPlanetId));
        }

        retVal.setTerminationDate(missionTimeManagerBo.computeTerminationDate(requiredTime));
        return retVal;
    }
}
