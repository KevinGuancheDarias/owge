package com.kevinguanchedarias.owgejava.business.unit;


import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.loader.UnitDataLoader;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.InterceptableSpeedGroupMock.givenInterceptableSpeedGroup;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.*;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.TARGET_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = ObtainedUnitFinderBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        UnitDataLoader.class,
        ObtainedUnitRepository.class,
        EntityManager.class,
        HiddenUnitBo.class,
        SpeedImpactGroupFinderBo.class
})
class ObtainedUnitFinderBoTest {

    private final ObtainedUnitFinderBo obtainedUnitFinderBo;
    private final EntityManager entityManager;
    private final HiddenUnitBo hiddenUnitBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final UnitDataLoader unitDataLoader;

    @Autowired
    public ObtainedUnitFinderBoTest(
            ObtainedUnitFinderBo obtainedUnitFinderBo,
            EntityManager entityManager,
            HiddenUnitBo hiddenUnitBo,
            SpeedImpactGroupFinderBo speedImpactGroupFinderBo,
            ObtainedUnitRepository obtainedUnitRepository,
            UnitDataLoader unitDataLoader
    ) {
        this.obtainedUnitFinderBo = obtainedUnitFinderBo;
        this.entityManager = entityManager;
        this.hiddenUnitBo = hiddenUnitBo;
        this.speedImpactGroupFinderBo = speedImpactGroupFinderBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.unitDataLoader = unitDataLoader;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void findCompletedAsDto_should_work(boolean hasSig) {
        var ou = givenObtainedUnit1();
        var user = ou.getUser();
        var sig = givenSpeedImpactGroup();
        var isgList = List.of(givenInterceptableSpeedGroup());

        var requirementGroup = RequirementGroup.builder().build();
        var unit = ou.getUnit();
        if (hasSig) {
            unit.setSpeedImpactGroup(sig);
        }
        sig.setRequirementGroups(List.of(requirementGroup));
        unit.setInterceptableSpeedGroups(isgList);
        given(hiddenUnitBo.isHiddenUnit(ou.getUser(), ou.getUnit())).willReturn(true);
        given(speedImpactGroupFinderBo.findApplicable(user, unit)).willReturn(sig);
        given(obtainedUnitRepository.findDeployedInUserOwnedPlanets(USER_ID_1))
                .willReturn(List.of(ou));

        try (var hibernateMock = mockStatic(Hibernate.class)) {
            var result = obtainedUnitFinderBo.findCompletedAsDto(user);

            if (hasSig) {
                hibernateMock.verify(() -> Hibernate.initialize(sig));
            }
            hibernateMock.verify(() -> Hibernate.initialize(isgList));
            verify(entityManager, times(1)).detach(unit);
            verify(hiddenUnitBo, times(1)).isHiddenUnit(ou.getUser(), ou.getUnit());
            verify(speedImpactGroupFinderBo, times(hasSig ? 0 : 1)).findApplicable(user, unit);
            assertThat(result)
                    .hasSize(1);
            var ouDto = result.get(0);
            verify(unitDataLoader, times(1)).addInformationToDto(ou, ouDto);
            var unitDto = ouDto.getUnit();
            assertThat(unitDto.getIsInvisible()).isTrue();
            assertThat(unitDto.getSpeedImpactGroup()).isNotNull();
            assertThat(unitDto.getSpeedImpactGroup().getRequirementsGroups()).isNull();
        }
    }

    @Test
    void findInPlanetOrInMissionToPlanet_should_work() {
        var ou1 = givenObtainedUnit1();
        var ou2 = givenObtainedUnit2();
        var planet = givenTargetPlanet();
        given(obtainedUnitRepository.findBySourcePlanetIdAndMissionIsNull(TARGET_PLANET_ID)).willReturn(List.of(ou1));
        given(obtainedUnitRepository.findByTargetPlanetIdAndMissionTypeCode(TARGET_PLANET_ID, MissionType.DEPLOYED.name()))
                .willReturn(List.of(ou2));

        assertThat(obtainedUnitFinderBo.findInPlanetOrInMissionToPlanet(planet))
                .contains(ou1)
                .contains(ou2)
                .hasSize(2);

    }

    @Test
    void findInvolvedInAttack_should_work() {
        var ou1 = givenObtainedUnit1();
        var ou2 = givenObtainedUnit2();
        var planet = givenTargetPlanet();
        var allowedMissions = List.of(MissionType.CONQUEST.name());
        given(obtainedUnitRepository.findBySourcePlanetIdAndMissionIsNull(TARGET_PLANET_ID)).willReturn(List.of(ou1));
        given(obtainedUnitRepository.findByTargetPlanetIdWhereReferencePercentageTimePassed(
                eq(TARGET_PLANET_ID), eq(0.1d), eq(allowedMissions), any())).willReturn(List.of(ou2));

        assertThat(obtainedUnitFinderBo.findInvolvedInAttack(planet))
                .contains(ou1)
                .contains(ou2)
                .hasSize(2);

    }

    @ParameterizedTest
    @MethodSource("determineTargetUnit_should_work_arguments")
    void determineTargetUnit_should_work(
            ObtainedUnit ou,
            ObtainedUnit ownerOu,
            Unit expectedUnit,
            int timesFindUnitByOu) {
        ou.setOwnerUnit(ownerOu);
        if (ownerOu != null) {
            given(obtainedUnitRepository.findUnitByOuId(ownerOu.getId())).willReturn(ownerOu.getUnit());
        }

        var unit = obtainedUnitFinderBo.determineTargetUnit(ou);

        verify(obtainedUnitRepository, times(timesFindUnitByOu)).findUnitByOuId(ownerOu != null ? ownerOu.getId() : any());
        assertThat(unit).isEqualTo(expectedUnit);
    }

    private static Stream<Arguments> determineTargetUnit_should_work_arguments() {
        var ouWithoutOwner = givenObtainedUnit1();
        var ownerUnit = new Unit();
        ownerUnit.setId(192814);
        var ownerOu = ouWithoutOwner.toBuilder()
                .id(OBTAINED_UNIT_1_ID + 1)
                .unit(ownerUnit)
                .build();
        var ouWithOwner = ouWithoutOwner.toBuilder().ownerUnit(ownerOu).build();
        return Stream.of(
                Arguments.of(ouWithoutOwner, null, ouWithoutOwner.getUnit(), 0),
                Arguments.of(ouWithOwner, ownerOu, ownerUnit, 1)
        );
    }
}
