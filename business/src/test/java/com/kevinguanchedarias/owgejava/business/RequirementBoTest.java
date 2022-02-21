package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.requirement.RequirementSource;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructRequirementBo;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.OBJECT_RELATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.REFERENCE_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.UNLOCKED_RELATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenUnlockedRelation;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformation;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.SECOND_VALUE;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.givenTimeSpecial;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.givenObtainedUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UpgradeMock.givenUpgrade;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = NonPostConstructRequirementBo.class

)
@MockBean({
        RequirementRepository.class,
        UserStorageBo.class,
        RequirementInformationDao.class,
        UnlockedRelationBo.class,
        ObtainedUpgradeBo.class,
        UpgradeBo.class,
        ObjectRelationBo.class,
        ObjectRelationToObjectRelationBo.class,
        DtoUtilService.class,
        RequirementInformationBo.class,
        SocketIoService.class,
        TimeSpecialBo.class,
        UnitBo.class,
        SpeedImpactGroupBo.class,
        PlanetBo.class,
        EntityManager.class,
        ObtainedUnitRepository.class,
        UnitRepository.class,
        RequirementSource.class,
        TransactionUtilService.class
})
class RequirementBoTest {
    private final RequirementBo requirementBo;
    private final RequirementSource requirementSource;
    private final ObjectRelationBo objectRelationBo;
    private final UnlockedRelationBo unlockedRelationBo;
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final UpgradeBo upgradeBo;

    @Autowired
    RequirementBoTest(
            RequirementBo requirementBo,
            RequirementSource requirementSource,
            ObjectRelationBo objectRelationBo,
            UnlockedRelationBo unlockedRelationBo,
            ObtainedUpgradeBo obtainedUpgradeBo,
            UpgradeBo upgradeBo
    ) {
        this.requirementBo = requirementBo;
        this.requirementSource = requirementSource;
        this.objectRelationBo = objectRelationBo;
        this.unlockedRelationBo = unlockedRelationBo;
        this.obtainedUpgradeBo = obtainedUpgradeBo;
        this.upgradeBo = upgradeBo;
    }

    @Test
    void triggerTimeSpecialActivated_should_work_and_mark_relation_as_unlocked() {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        timeSpecial.setId((int) SECOND_VALUE);
        var or = givenObjectRelation();
        var requirement = givenRequirementInformation(TIME_SPECIAL_ID, RequirementTypeEnum.HAVE_SPECIAL_ENABLED);
        var upgrade = givenUpgrade(REFERENCE_ID);
        or.setRequirements(List.of(requirement));
        given(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE))
                .willReturn(List.of(or));
        given(requirementSource.supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name())).willReturn(true);
        given(requirementSource.checkRequirementIsMet(requirement, user)).willReturn(true);
        given(upgradeBo.findById(upgrade.getId())).willReturn(upgrade);

        requirementBo.triggerTimeSpecialStateChange(user, timeSpecial);

        verify(objectRelationBo, times(1)).findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE);
        verify(requirementSource, times(1)).supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name());
        verify(requirementSource, times(1)).checkRequirementIsMet(requirement, user);
        var captor = ArgumentCaptor.forClass(UnlockedRelation.class);
        verify(unlockedRelationBo, times(1)).save(captor.capture());
        var savedUnlocked = captor.getValue();
        assertThat(savedUnlocked.getRelation()).isEqualTo(or);
        assertThat(savedUnlocked.getUser()).isEqualTo(user);
        checkRegisterObtainedUpgrade(or, upgrade, user);
    }

    @Test
    void triggerTimeSpecialActivated_should_work_and_mark_relation_as_lost() {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        timeSpecial.setId((int) SECOND_VALUE);
        var or = givenObjectRelation();
        var requirement = givenRequirementInformation(TIME_SPECIAL_ID, RequirementTypeEnum.HAVE_SPECIAL_ENABLED);
        var upgrade = givenUpgrade(REFERENCE_ID);
        var obtainedUpgrade = givenObtainedUpgrade(REFERENCE_ID, user);
        obtainedUpgrade.setAvailable(true);
        var ur = givenUnlockedRelation(user);
        or.setRequirements(List.of(requirement));
        given(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE))
                .willReturn(List.of(or));
        given(requirementSource.supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name())).willReturn(true);
        given(upgradeBo.findById(upgrade.getId())).willReturn(upgrade);
        given(unlockedRelationBo.findOneByUserIdAndRelationId(USER_ID_1, OBJECT_RELATION_ID)).willReturn(ur);
        given(obtainedUpgradeBo.findUserObtainedUpgrade(USER_ID_1, REFERENCE_ID)).willReturn(obtainedUpgrade);
        given(obtainedUpgradeBo.userHasUpgrade(USER_ID_1, REFERENCE_ID)).willReturn(true);


        requirementBo.triggerTimeSpecialStateChange(user, timeSpecial);

        verify(objectRelationBo, times(1)).findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE);
        verify(requirementSource, times(1)).supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name());
        verify(requirementSource, times(1)).checkRequirementIsMet(requirement, user);

        verify(unlockedRelationBo, times(1)).findOneByUserIdAndRelationId(USER_ID_1, OBJECT_RELATION_ID);
        verify(unlockedRelationBo, times(1)).delete(UNLOCKED_RELATION_ID);
        verify(obtainedUpgradeBo, times(1)).userHasUpgrade(USER_ID_1, REFERENCE_ID);
        var captor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeBo, times(1)).save(captor.capture());
        var savedLostUpgrade = captor.getValue();
        assertThat(savedLostUpgrade).isSameAs(obtainedUpgrade);
        assertThat(savedLostUpgrade.isAvailable()).isFalse();

    }

    @Test
    void triggerTimeSpecialActivated_should_throw_when_or_has_unsupported_requirement() {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        timeSpecial.setId((int) SECOND_VALUE);
        var or = givenObjectRelation();
        var requirement = givenRequirementInformation(TIME_SPECIAL_ID, RequirementTypeEnum.HAVE_SPECIAL_ENABLED);
        or.setRequirements(List.of(requirement));
        given(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE))
                .willReturn(List.of(or));

        assertThatThrownBy(() -> requirementBo.triggerTimeSpecialStateChange(user, timeSpecial))
                .isInstanceOf(SgtBackendNotImplementedException.class)
                .hasMessageContaining("Not implemented requirement type")
                .hasMessageContaining(requirement.getRequirement().getCode());
    }

    private void checkUnlockedUpgrade(ObjectRelation or, UserStorage user) {
        verify(obtainedUpgradeBo, times(1)).userHasUpgrade(user.getId(), or.getReferenceId());
    }

    private void checkRegisterObtainedUpgrade(ObjectRelation or, Upgrade upgrade, UserStorage user) {
        checkUnlockedUpgrade(or, user);
        verify(upgradeBo, times(1)).findById(upgrade.getId());
        var captor = ArgumentCaptor.forClass(ObtainedUpgrade.class);
        verify(obtainedUpgradeBo, times(1)).save(captor.capture());
        var savedObtainedUpgrade = captor.getValue();
        assertThat(savedObtainedUpgrade.getLevel()).isZero();
        assertThat(savedObtainedUpgrade.getUpgrade()).isEqualTo(upgrade);
        assertThat(savedObtainedUpgrade.getUser()).isEqualTo(user);
        assertThat(savedObtainedUpgrade.isAvailable()).isTrue();
    }
}
