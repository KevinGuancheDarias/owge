package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.listener.ImageStoreListener;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MissionUnitsFinderBo {

    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ImageStoreBo imageStoreBo;

    /**
     * Due to lack of support from Quartz to access spring context from the
     * EntityListener of {@link ImageStoreListener} we have to invoke the image URL
     * computation from here
     *
     * @author Kevin Guanche Darias
     */
    public List<ObtainedUnit> findUnitsInvolved(Long missionId) {
        List<ObtainedUnit> retVal = obtainedUnitRepository.findByMissionId(missionId);
        retVal.forEach(current -> imageStoreBo.computeImageUrl(current.getUnit().getImage()));
        return retVal;
    }
}
