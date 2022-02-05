package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.requirement.TimeSpecialEnabledRequirementSourceBo.REQUIREMENT_SOURCE_ID;
import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.RequirementMock.givenRequirementInformation;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = TimeSpecialEnabledRequirementSourceBo.class
)
@MockBean(ActiveTimeSpecialRepository.class)
class TimeSpecialEnabledRequirementSourceBoTest {
    private final TimeSpecialEnabledRequirementSourceBo timeSpecialEnabledRequirementSourceBo;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;

    @Autowired
    TimeSpecialEnabledRequirementSourceBoTest(
            TimeSpecialEnabledRequirementSourceBo timeSpecialEnabledRequirementSourceBo,
            ActiveTimeSpecialRepository activeTimeSpecialRepository
    ) {
        this.timeSpecialEnabledRequirementSourceBo = timeSpecialEnabledRequirementSourceBo;
        this.activeTimeSpecialRepository = activeTimeSpecialRepository;
    }

    @Test
    void supports_should_work() {
        assertThat(timeSpecialEnabledRequirementSourceBo.supports("foo")).isFalse();
        assertThat(timeSpecialEnabledRequirementSourceBo.supports(REQUIREMENT_SOURCE_ID)).isTrue();
    }

    @Test
    void checkRequirementIsMet_should_return_true_when_wanted_active_time_special_is_active() {
        when(activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1))
                .thenReturn(Optional.of(givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE)));
        var user = givenUser1();

        assertThat(timeSpecialEnabledRequirementSourceBo.checkRequirementIsMet(givenRequirementInformation(TIME_SPECIAL_ID), user))
                .isTrue();

        verify(activeTimeSpecialRepository, times(1)).findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1);
    }

    @Test
    void checkRequirementIsMet_should_return_false_when_wanted_active_time_special_is_NOT_active() {
        when(activeTimeSpecialRepository.findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1))
                .thenReturn(Optional.of(givenActiveTimeSpecialMock(TimeSpecialStateEnum.RECHARGE)));
        var user = givenUser1();

        assertThat(timeSpecialEnabledRequirementSourceBo.checkRequirementIsMet(givenRequirementInformation(TIME_SPECIAL_ID), user))
                .isFalse();

        verify(activeTimeSpecialRepository, times(1)).findOneByTimeSpecialIdAndUserId(TIME_SPECIAL_ID, USER_ID_1);
    }

    @Test
    void checkRequirementIsMet_should_return_false_when_wanted_active_time_special_has_unknown_state() {
        assertThat(timeSpecialEnabledRequirementSourceBo.checkRequirementIsMet(givenRequirementInformation(TIME_SPECIAL_ID), givenUser2()))
                .isFalse();
    }
}
