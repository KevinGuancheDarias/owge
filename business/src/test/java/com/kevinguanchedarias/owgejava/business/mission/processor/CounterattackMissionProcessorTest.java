package com.kevinguanchedarias.owgejava.business.mission.processor;


import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenAttackMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = CounterattackMissionProcessor.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(AttackMissionProcessor.class)
class CounterattackMissionProcessorTest {
    private final CounterattackMissionProcessor counterattackMissionProcessor;
    private final AttackMissionProcessor attackMissionProcessor;

    @Autowired
    public CounterattackMissionProcessorTest(
            CounterattackMissionProcessor counterattackMissionProcessor,
            AttackMissionProcessor attackMissionProcessor
    ) {
        this.counterattackMissionProcessor = counterattackMissionProcessor;
        this.attackMissionProcessor = attackMissionProcessor;
    }

    @Test
    void supports_should_work() {
        assertThat(counterattackMissionProcessor.supports(MissionType.EXPLORE)).isFalse();
        assertThat(counterattackMissionProcessor.supports(MissionType.COUNTERATTACK)).isTrue();
    }

    @Test
    void process_works() {
        var mission = givenAttackMission();
        var involvedUnits = List.of(givenObtainedUnit1());
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        given(attackMissionProcessor.process(mission, involvedUnits)).willReturn(reportBuilderMock);

        var retVal = counterattackMissionProcessor.process(mission, involvedUnits);

        assertThat(retVal).isSameAs(reportBuilderMock);
    }
}
