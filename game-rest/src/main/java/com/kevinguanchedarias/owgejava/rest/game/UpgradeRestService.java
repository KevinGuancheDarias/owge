package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.ObtainedUpgradeBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUpgradeDto;
import com.kevinguanchedarias.owgejava.dto.RunningUpgradeDto;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("game/upgrade")
@ApplicationScope
@AllArgsConstructor
public class UpgradeRestService implements SyncSource {
    private final UserSessionService userSessionService;
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final MissionBo missionBo;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final MissionRepository missionRepository;
    private final TaggableCacheManager taggableCacheManager;

    @GetMapping("registerLevelUp")
    public RunningUpgradeDto registerLevelUp(@RequestParam("upgradeId") Integer upgradeId) {
        var userId = userSessionService.findLoggedIn().getId();
        missionBo.registerLevelUpAnUpgrade(userId, upgradeId);
        RunningUpgradeDto retVal = missionBo.findRunningLevelUpMission(userId);
        retVal.setMissionsCount(missionRepository.countByUserIdAndResolvedFalse(userId));
        return retVal;
    }

    @GetMapping("cancelUpgrade")
    public Object cancelUpgrade() {
        missionBo.cancelUpgradeMission(userSessionService.findLoggedIn().getId());
        return "{}";
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create().withHandler("obtained_upgrades_change", this::findObtained)
                .withHandler("running_upgrade_change", this::findRunningUpgrade).build();
    }

    private List<ObtainedUpgradeDto> findObtained(UserStorage user) {
        var obtainedUpgradeList = obtainedUpgradeRepository.findByUserId(user.getId());
        obtainedUpgradeList.forEach(obtained -> initImprovement(obtained.getUpgrade()));
        return obtainedUpgradeBo.toDto(obtainedUpgradeList);
    }

    private RunningUpgradeDto findRunningUpgrade(UserStorage user) {
        return missionBo.findRunningLevelUpMission(user.getId());
    }

    private void initImprovement(Upgrade upgrade) {
        if (upgrade.getImprovement() != null) {
            upgrade.setImprovement(taggableCacheManager.computeIfAbsent(
                    getClass().getCanonicalName() + "_initImprovement_" + upgrade.getId(),
                    List.of(Upgrade.UPGRADE_CACHE_TAG),
                    () -> {
                        Hibernate.initialize(upgrade.getImprovement());
                        return upgrade.getImprovement();
                    }
            ));
        }
    }
}
