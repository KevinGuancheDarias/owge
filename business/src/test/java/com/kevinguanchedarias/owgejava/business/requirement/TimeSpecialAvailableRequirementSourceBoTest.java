package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformation;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = TimeSpecialAvailableRequirementSourceBo.class
)
@MockBean({
        ObjectRelationsRepository.class,
        UnlockedRelationRepository.class
})
class TimeSpecialAvailableRequirementSourceBoTest {
    private final TimeSpecialAvailableRequirementSourceBo timeSpecialAvailableRequirementSourceBo;
    private final ObjectRelationsRepository objectRelationsRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;

    @Autowired
    public TimeSpecialAvailableRequirementSourceBoTest(
            TimeSpecialAvailableRequirementSourceBo timeSpecialAvailableRequirementSourceBo,
            ObjectRelationsRepository objectRelationsRepository,
            UnlockedRelationRepository unlockedRelationRepository
    ) {
        this.timeSpecialAvailableRequirementSourceBo = timeSpecialAvailableRequirementSourceBo;
        this.objectRelationsRepository = objectRelationsRepository;
        this.unlockedRelationRepository = unlockedRelationRepository;
    }

    @Test
    void supports_should_work() {
        assertThat(timeSpecialAvailableRequirementSourceBo.supports(RequirementTypeEnum.HAVE_SPECIAL_AVAILABLE.name())).isTrue();
        assertThat(timeSpecialAvailableRequirementSourceBo.supports("yeah")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void checkRequirementIsMet_should_work(boolean isUnlockedRelation) {
        var user = givenUser1();
        var relation = givenObjectRelation();
        var requirementInformation = givenRequirementInformation(TIME_SPECIAL_ID);
        given(objectRelationsRepository.findOneByObjectDescriptionAndReferenceId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(relation);
        given(unlockedRelationRepository.existsByUserAndRelation(user, relation)).willReturn(isUnlockedRelation);

        assertThat(timeSpecialAvailableRequirementSourceBo.checkRequirementIsMet(requirementInformation, user)).isEqualTo(isUnlockedRelation);
    }

    @Test
    void checkRequirementIsMet_should_log_warn_when_relation_is_missing(CapturedOutput capturedOutput) {
        assertThat(timeSpecialAvailableRequirementSourceBo.checkRequirementIsMet(givenRequirementInformation(TIME_SPECIAL_ID), givenUser1())).isFalse();
        assertThat(capturedOutput.getOut()).contains("Missing object relation for time special with id " + TIME_SPECIAL_ID);
    }
}
