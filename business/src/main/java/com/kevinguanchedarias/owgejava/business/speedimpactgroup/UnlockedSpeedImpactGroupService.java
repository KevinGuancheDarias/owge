package com.kevinguanchedarias.owgejava.business.speedimpactgroup;

import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UnlockedSpeedImpactGroupService {
    private final UnlockedRelationBo unlockedRelationBo;

    /**
     * Finds the ids of the unlocked cross galaxy speed impact
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<Integer> findCrossGalaxyUnlocked(UserStorage user) {
        List<SpeedImpactGroup> speedImpactGroups = unlockedRelationBo.unboxToTargetEntity(
                unlockedRelationBo.findByUserIdAndObjectType(user.getId(), ObjectEnum.SPEED_IMPACT_GROUP));
        return speedImpactGroups.stream().map(SpeedImpactGroup::getId).toList();
    }
}
