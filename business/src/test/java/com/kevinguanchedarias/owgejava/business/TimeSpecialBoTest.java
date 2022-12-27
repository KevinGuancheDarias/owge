package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.dto.ActiveTimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.RequirementInformationRepository;
import com.kevinguanchedarias.owgejava.repository.TimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.*;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = TimeSpecialBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementBo.class,
        ActiveTimeSpecialBo.class,
        UserSessionService.class,
        TimeSpecialRepository.class,
        ObjectRelationsRepository.class,
        UnlockedRelationRepository.class,
        RequirementInformationRepository.class
})
class TimeSpecialBoTest {
    private final TimeSpecialBo timeSpecialBo;
    private final ObjectRelationsRepository objectRelationsRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;
    private final TimeSpecialRepository timeSpecialRepository;
    private final ActiveTimeSpecialBo activeTimeSpecialBo;
    private final UserSessionService userSessionService;

    @Autowired
    public TimeSpecialBoTest(
            TimeSpecialBo timeSpecialBo,
            ObjectRelationsRepository objectRelationsRepository,
            UnlockedRelationRepository unlockedRelationRepository,
            TimeSpecialRepository timeSpecialRepository,
            ActiveTimeSpecialBo activeTimeSpecialBo,
            UserSessionService userSessionService
    ) {
        this.timeSpecialBo = timeSpecialBo;
        this.objectRelationsRepository = objectRelationsRepository;
        this.unlockedRelationRepository = unlockedRelationRepository;
        this.timeSpecialRepository = timeSpecialRepository;
        this.activeTimeSpecialBo = activeTimeSpecialBo;
        this.userSessionService = userSessionService;
    }

    @Test
    void delete_should_work() {
        var timeSpecial = givenTimeSpecial();
        var or = givenObjectRelation();
        given(timeSpecialRepository.findById(TIME_SPECIAL_ID)).willReturn(Optional.of(timeSpecial));
        given(objectRelationsRepository.findOneByObjectCodeAndReferenceId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(or);

        timeSpecialBo.delete(timeSpecial);

        verify(activeTimeSpecialBo, times(1)).deleteByTimeSpecial(timeSpecial);
        verify(unlockedRelationRepository, times(1)).deleteByRelation(or);
        verify(objectRelationsRepository, times(1)).delete(or);
        verify(timeSpecialRepository, times(1)).delete(timeSpecial);
    }

    @ParameterizedTest
    @MethodSource("toDto_should_work_arguments")
    void toDto_should_work(UserStorage user, ActiveTimeSpecialDto expectedActiveTimeSpecialDto, int timesFindAndSetActive) {
        var timeSpecial = givenTimeSpecial();
        var activeTimeSpecial = givenActiveTimeSpecial();
        given(userSessionService.findLoggedIn()).willReturn(user);
        given(activeTimeSpecialBo.findOneByTimeSpecial(TIME_SPECIAL_ID, USER_ID_1)).willReturn(activeTimeSpecial);
        given(activeTimeSpecialBo.toDto(activeTimeSpecial)).willReturn(expectedActiveTimeSpecialDto);

        var retVal = timeSpecialBo.toDto(timeSpecial);

        assertThat(retVal.getId()).isEqualTo(TIME_SPECIAL_ID);
        assertThat(retVal.getActiveTimeSpecialDto()).isEqualTo(expectedActiveTimeSpecialDto);
        verify(activeTimeSpecialBo, times(timesFindAndSetActive)).findOneByTimeSpecial(TIME_SPECIAL_ID, USER_ID_1);
        verify(activeTimeSpecialBo, times(timesFindAndSetActive)).toDto(activeTimeSpecial);
    }

    private static Stream<Arguments> toDto_should_work_arguments() {
        var dto = new ActiveTimeSpecialDto();
        dto.setId(ACTIVE_TIME_SPECICAL_ID);
        return Stream.of(
                Arguments.of(givenUser1(), dto, 1),
                Arguments.of(null, null, 0)
        );
    }
}
