package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.*;
import com.kevinguanchedarias.owgejava.business.requirement.RequirementInternalEventEmitterService;
import com.kevinguanchedarias.owgejava.business.requirement.RequirementSource;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnlockedSpeedImpactGroupService;
import com.kevinguanchedarias.owgejava.business.timespecial.UnlockableTimeSpecialService;
import com.kevinguanchedarias.owgejava.business.unit.UnlockableUnitService;
import com.kevinguanchedarias.owgejava.business.user.UserPlanetLockService;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.repository.*;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.util.List;

@Service
@Primary
public class NonPostConstructRequirementBo extends RequirementBo {

    @Serial
    private static final long serialVersionUID = -3154640390017555899L;

    public NonPostConstructRequirementBo(RequirementRepository requirementRepository, UnlockedRelationBo unlockedRelationBo, UpgradeBo upgradeBo, ObjectRelationBo objectRelationBo, ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo, DtoUtilService dtoUtilService, RequirementInformationRepository requirementInformationRepository, AutowireCapableBeanFactory beanFactory, SocketIoService socketIoService, UnlockableTimeSpecialService unlockableTimeSpecialService, UnlockableUnitService unlockableUnitService, UnlockedSpeedImpactGroupService unlockedSpeedImpactGroupService, PlanetRepository planetRepository, EntityManager entityManager, ObtainedUnitRepository obtainedUnitRepository, UnitRepository unitRepository, List<RequirementSource> requirementSources, TransactionUtilService transactionUtilService, ObtainedUpgradeRepository obtainedUpgradeRepository, UnlockedRelationRepository unlockedRelationRepository, UserStorageRepository userStorageRepository, UserPlanetLockService userPlanetLockService, RequirementInternalEventEmitterService requirementInternalEventEmitterService) {
        super(requirementRepository, unlockedRelationBo, upgradeBo, objectRelationBo, objectRelationToObjectRelationBo, dtoUtilService, requirementInformationRepository, beanFactory, socketIoService, unlockableTimeSpecialService, unlockableUnitService, unlockedSpeedImpactGroupService, planetRepository, entityManager, obtainedUnitRepository, unitRepository, requirementSources, transactionUtilService, obtainedUpgradeRepository, unlockedRelationRepository, userStorageRepository, userPlanetLockService, requirementInternalEventEmitterService);
    }


    @Override
    @PostConstruct
    public void init() {
        // Do nothing
    }

    public void realInit() {
        super.init();
    }
}
