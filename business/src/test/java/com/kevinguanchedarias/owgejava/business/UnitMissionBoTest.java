package com.kevinguanchedarias.owgejava.business;


import com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTimeManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.attack.AttackMissionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.checker.CrossGalaxyMissionChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.UnitMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnitInterceptionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitModificationBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitMissionBo.class
)
@MockBean({
        MissionRepository.class,
        ObtainedUpgradeBo.class,
        ObjectRelationBo.class,
        UpgradeBo.class,
        UserStorageBo.class,
        MissionTypeRepository.class,
        ImprovementBo.class,
        RequirementBo.class,
        UnlockedRelationBo.class,
        UnitBo.class,
        ObtainedUnitBo.class,
        PlanetBo.class,
        ExceptionUtilService.class,
        UnitTypeBo.class,
        MissionConfigurationBo.class,
        SocketIoService.class,
        MissionReportBo.class,
        MissionSchedulerService.class,
        ConfigurationBo.class,
        PlanetListBo.class,
        AsyncRunnerBo.class,
        EntityManager.class,
        AuditBo.class,
        CriticalAttackBo.class,
        AttackMissionManagerBo.class,
        ObtainedUnitRepository.class,
        TransactionUtilService.class,
        TaggableCacheManager.class,
        HiddenUnitBo.class,
        PlanetLockUtilService.class,
        CrossGalaxyMissionChecker.class,
        PlanetRepository.class,
        MissionEventEmitterBo.class,
        ObtainedUnitFinderBo.class,
        MissionTimeManagerBo.class,
        ObtainedUnitModificationBo.class,
        MissionBo.class,
        UnitMissionRegistrationBo.class,
        ObtainedUnitEventEmitter.class,
        ReturnMissionRegistrationBo.class
})
class UnitMissionBoTest {
    private static final int ALLY_ID = 19282;
    private static final long EXPIRATION_ID = 8;

    private final UnitMissionBo unitMissionBo;
    private final PlanetBo planetBo;
    private final AttackMissionManagerBo attackMissionManagerBo;
    private final MissionRepository missionRepository;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionReportBo missionReportBo;
    private final MissionTypeRepository missionTypeRepository;
    private final RequirementBo requirementBo;
    private final PlanetListBo planetListBo;
    private final TransactionUtilService transactionUtilService;
    private final ConfigurationBo configurationBo;
    private final UserStorageBo userStorageBo;
    private final MissionConfigurationBo missionConfigurationBo;
    private final UnitBo unitBo;
    private final UnitTypeBo unitTypeBo;
    private final SocketIoService socketIoService;
    private final ImprovementBo improvementBo;
    private final HiddenUnitBo hiddenUnitBo;
    private final PlanetLockUtilService planetLockUtilService;
    private final AsyncRunnerBo asyncRunnerBo;
    private final EntityManager entityManager;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final UnitInterceptionFinderBo unitInterceptionFinderBo;
    private final PlanetRepository planetRepository;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;

    @Autowired
    public UnitMissionBoTest(
            UnitMissionBo unitMissionBo,
            PlanetBo planetBo,
            AttackMissionManagerBo attackMissionManagerBo,
            MissionRepository missionRepository,
            ObtainedUnitBo obtainedUnitBo,
            MissionReportBo missionReportBo,
            MissionTypeRepository missionTypeRepository,
            RequirementBo requirementBo,
            PlanetListBo planetListBo,
            TransactionUtilService transactionUtilService,
            ConfigurationBo configurationBo,
            UserStorageBo userStorageBo,
            MissionConfigurationBo missionConfigurationBo,
            UnitBo unitBo,
            UnitTypeBo unitTypeBo,
            SocketIoService socketIoService,
            ImprovementBo improvementBo,
            HiddenUnitBo hiddenUnitBo,
            PlanetLockUtilService planetLockUtilService,
            AsyncRunnerBo asyncRunnerBo,
            EntityManager entityManager,
            ObtainedUnitRepository obtainedUnitRepository,
            UnitInterceptionFinderBo unitInterceptionFinderBo,
            PlanetRepository planetRepository,
            MissionEventEmitterBo missionEventEmitterBo,
            ObtainedUnitFinderBo obtainedUnitFinderBo
    ) {
        // Notice: Test in this class are not full covering the methods, as they are only testing changed lines
        this.unitMissionBo = unitMissionBo;
        this.planetBo = planetBo;
        this.attackMissionManagerBo = attackMissionManagerBo;
        this.missionRepository = missionRepository;
        this.obtainedUnitBo = obtainedUnitBo;
        this.missionReportBo = missionReportBo;
        this.missionTypeRepository = missionTypeRepository;
        this.requirementBo = requirementBo;
        this.planetListBo = planetListBo;
        this.transactionUtilService = transactionUtilService;
        this.obtainedUnitFinderBo = obtainedUnitFinderBo;
        this.configurationBo = configurationBo;
        this.userStorageBo = userStorageBo;
        this.missionConfigurationBo = missionConfigurationBo;
        this.unitBo = unitBo;
        this.unitTypeBo = unitTypeBo;
        this.socketIoService = socketIoService;
        this.improvementBo = improvementBo;
        this.hiddenUnitBo = hiddenUnitBo;
        this.planetLockUtilService = planetLockUtilService;
        this.asyncRunnerBo = asyncRunnerBo;
        this.entityManager = entityManager;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.unitInterceptionFinderBo = unitInterceptionFinderBo;
        this.planetRepository = planetRepository;
        this.missionEventEmitterBo = missionEventEmitterBo;
    }
}
