package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.requirement.RequirementSource;
import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructRequirementBo;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.EntityManager;
import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenAllRequirements;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformation;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.SECOND_VALUE;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.givenTimeSpecial;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
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
        RequirementSource.class
})
class RequirementBoTest {
    private final RequirementBo requirementBo;
    private final RequirementSource requirementSource;
    private final ObjectRelationBo objectRelationBo;
    private final ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo;
    private final RequirementRepository requirementRepository;

    @Autowired
    RequirementBoTest(
            RequirementBo requirementBo,
            RequirementSource requirementSource,
            ObjectRelationBo objectRelationBo,
            ObjectRelationToObjectRelationBo objectRelationToObjectRelationBo,
            RequirementRepository requirementRepository
    ) {
        this.requirementBo = requirementBo;
        this.requirementSource = requirementSource;
        this.objectRelationBo = objectRelationBo;
        this.objectRelationToObjectRelationBo = objectRelationToObjectRelationBo;
        this.requirementRepository = requirementRepository;
    }

    @Test
    void triggerTimeSpecialActivated_should_work() {
        var user = givenUser1();
        var timeSpecial = givenTimeSpecial();
        timeSpecial.setId((int) SECOND_VALUE);
        var or = givenObjectRelation();
        var requirement = givenRequirementInformation(TIME_SPECIAL_ID, RequirementTypeEnum.HAVE_SPECIAL_ENABLED);
        or.setRequirements(List.of(requirement));
        given(objectRelationBo.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE))
                .willReturn(List.of(or));
        given(requirementRepository.findAll()).willReturn(givenAllRequirements());
        given(requirementSource.supports(RequirementTypeEnum.HAVE_SPECIAL_ENABLED.name())).willReturn(true);

        requirementBo.triggerTimeSpecialStateChange(user, timeSpecial);

        verify(objectRelationBo, times(1)).findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_SPECIAL_ENABLED, SECOND_VALUE);
        verify(requirementSource, times(1)).checkRequirementIsMet(requirement, user);
    }

}
