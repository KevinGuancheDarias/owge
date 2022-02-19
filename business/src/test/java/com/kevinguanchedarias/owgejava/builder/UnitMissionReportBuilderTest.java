package com.kevinguanchedarias.owgejava.builder;

import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.listener.UnitCaptureContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.kevinguanchedarias.owgejava.mock.AttackMock.givenAttackObtainedUnit;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit2;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnitWithBypassShields;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;

class UnitMissionReportBuilderTest {

    @SuppressWarnings("unchecked")
    @Test
    void withUnitCaptureInformation_should_work() {
        var firstExpected = createUnitCaptureContext(
                givenAttackObtainedUnit(),
                givenAttackObtainedUnit(givenObtainedUnit2()),
                8
        );
        var secondExpected = createUnitCaptureContext(
                givenAttackObtainedUnit(givenObtainedUnitWithBypassShields(givenUser1())),
                givenAttackObtainedUnit(givenObtainedUnit2()),
                14
        );
        var builder = UnitMissionReportBuilder.create();
        var maybeOtherInstance = builder.withUnitCaptureInformation(List.of(firstExpected, secondExpected));

        assertThat(maybeOtherInstance).isSameAs(builder);
        var createdMap = maybeOtherInstance.build();
        assertThat(createdMap).containsKey("unitCaptureInformation");
        List<Map<String, Object>> information = (List<Map<String, Object>>) createdMap.get("unitCaptureInformation");
        assertThat(information).hasSize(2);
        var firstResult = information.get(0);
        var secondResult = information.get(1);
        assertThat(firstResult).extractingByKey("unit").isInstanceOf(UnitDto.class);
        assertThat(firstResult).extractingByKey("oldOwner").isInstanceOf(UserStorageDto.class);
        assertThat(firstResult).extractingByKey("capturedCount").isEqualTo(firstExpected.getCapturedUnits());
        assertThat(secondResult).extractingByKey("unit")
                .isInstanceOf(UnitDto.class)
                .extracting(unit -> ((UnitDto) unit).getId())
                .isEqualTo(secondExpected.getVictimUnit().getObtainedUnit().getUnit().getId());
        assertThat(secondResult).extractingByKey("oldOwner")
                .isInstanceOf(UserStorageDto.class)
                .extracting(user -> ((UserStorageDto) user).getId())
                .isEqualTo(secondExpected.getVictimUnit().getUser().getUser().getId());
        assertThat(secondResult).extractingByKey("capturedCount").isEqualTo(secondExpected.getCapturedUnits());
    }

    private UnitCaptureContext createUnitCaptureContext(AttackObtainedUnit captorUnit, AttackObtainedUnit victim, long captured) {
        return UnitCaptureContext.builder()
                .captorUnit(captorUnit)
                .victimUnit(victim)
                .capturedUnits(captured)
                .build();
    }
}
