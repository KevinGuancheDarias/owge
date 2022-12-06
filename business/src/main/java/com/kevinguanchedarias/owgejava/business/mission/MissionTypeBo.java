package com.kevinguanchedarias.owgejava.business.mission;


import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MissionTypeBo {
    private final MissionTypeRepository missionTypeRepository;

    public MissionType resolve(Mission mission) {
        if (mission != null) {
            return MissionType.valueOf(mission.getType().getCode());
        } else {
            return null;
        }
    }

    /**
     * @param type enum based mission type
     * @return persisted mission type
     * @author Kevin Guanche Darias
     */
    public com.kevinguanchedarias.owgejava.entity.MissionType find(MissionType type) {
        return missionTypeRepository.findOneByCode(type.name())
                .orElseThrow(() -> new SgtBackendInvalidInputException("No MissionType " + type.name() + " was found in the database"));
    }
}
