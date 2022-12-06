package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.checker.EntityCanDoMissionChecker;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementChangeEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.FactionUnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.responses.UnitTypeResponse;
import com.kevinguanchedarias.owgejava.test.answer.InvokeBiConsumerLambdaAnswer;
import com.kevinguanchedarias.owgejava.test.answer.InvokeSupplierLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.business.UnitTypeBo.UNIT_TYPE_CHANGE;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenFaction;
import static com.kevinguanchedarias.owgejava.mock.FactionMock.givenFactionUnitType;
import static com.kevinguanchedarias.owgejava.mock.ImprovementUnitTypeMock.givenImprovementUnitType;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.givenTargetPlanet;
import static com.kevinguanchedarias.owgejava.mock.SpeedImpactGroupMock.givenSpeedImpactGroup;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.UNIT_TYPE_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitTypeMock.givenUnitType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = UnitTypeBo.class
)
@MockBean({
        UnitTypeRepository.class,
        ImprovementBo.class,
        UnitMissionBo.class,
        UserStorageRepository.class,
        ObtainedUnitRepository.class,
        SocketIoService.class,
        FactionUnitTypeRepository.class,
        EntityCanDoMissionChecker.class,
        ObtainedUnitRepository.class,
        DtoUtilService.class
})
class UnitTypeBoTest {
    private static final int SECOND_UNIT_TYPE_ID = 11811;

    private final UnitTypeBo unitTypeBo;
    private final UserStorageRepository userStorageRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ImprovementBo improvementBo;
    private final EntityCanDoMissionChecker entityCanDoMissionChecker;
    private final SocketIoService socketIoService;
    private final DtoUtilService dtoUtilService;
    private final UnitTypeRepository unitTypeRepository;
    private final FactionUnitTypeRepository factionUnitTypeRepository;

    @Autowired
    UnitTypeBoTest(
            UnitTypeBo unitTypeBo,
            UserStorageRepository userStorageRepository,
            ObtainedUnitRepository obtainedUnitRepository,
            ImprovementBo improvementBo,
            EntityCanDoMissionChecker entityCanDoMissionChecker,
            SocketIoService socketIoService,
            DtoUtilService dtoUtilService,
            UnitTypeRepository unitTypeRepository,
            FactionUnitTypeRepository factionUnitTypeRepository
    ) {
        this.unitTypeBo = unitTypeBo;
        this.userStorageRepository = userStorageRepository;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.improvementBo = improvementBo;
        this.entityCanDoMissionChecker = entityCanDoMissionChecker;
        this.socketIoService = socketIoService;
        this.dtoUtilService = dtoUtilService;
        this.unitTypeRepository = unitTypeRepository;
        this.factionUnitTypeRepository = factionUnitTypeRepository;
    }

    @ParameterizedTest
    @MethodSource("init_should_work_arguments")
    void init_should_work(
            ImprovementUnitType improvementUnitType,
            int timesMessage,
            Faction faction,
            FactionUnitType factionUnitType,
            Long expectedComputeMaxCount,
            int timesSetUserBuilt,
            Long expectedUserBuilt,
            boolean expectedIsUsed
    ) {
        var unitType = givenUnitType();
        var spiSpy = spy(givenSpeedImpactGroup());
        unitType.setSpeedImpactGroup(spiSpy);
        var user = givenUser1();
        user.setFaction(faction);
        var answerUnitImprovementsChangeConsumer = new InvokeBiConsumerLambdaAnswer<Integer, Improvement>(1);
        doAnswer(answerUnitImprovementsChangeConsumer).when(improvementBo).addChangeListener(eq(ImprovementChangeEnum.UNIT_IMPROVEMENTS), any());
        var improvementMock = mock(Improvement.class);
        given(improvementMock.getUnitTypesUpgrades()).willReturn(List.of(improvementUnitType));
        var messageContentAnswer = new InvokeSupplierLambdaAnswer<List<UnitTypeResponse>>(2);
        doAnswer(messageContentAnswer).when(socketIoService).sendMessage(eq(USER_ID_1), eq(UNIT_TYPE_CHANGE), any());
        given(unitTypeRepository.findAll()).willReturn(List.of(unitType));
        var unitTypeResponseMock = mock(UnitTypeResponse.class);
        given(dtoUtilService.dtoFromEntity(UnitTypeResponse.class, unitType)).willReturn(unitTypeResponseMock);
        given(userStorageRepository.findById(USER_ID_1)).willReturn(Optional.of(user));
        given(improvementBo.findUserImprovement(user)).willReturn(mock(GroupedImprovement.class));
        given(improvementBo.computeImprovementValue(anyDouble(), anyDouble())).willReturn(Double.valueOf(expectedComputeMaxCount));
        given(obtainedUnitRepository.countByUserAndUnitType(user, unitType)).willReturn(expectedUserBuilt);
        given(unitTypeRepository.existsByUnitsTypeId(UNIT_TYPE_ID)).willReturn(expectedIsUsed);
        given(factionUnitTypeRepository.findOneByFactionAndUnitType(faction, unitType)).willReturn(Optional.ofNullable(factionUnitType));


        unitTypeBo.init();
        answerUnitImprovementsChangeConsumer.getPassedLambda().accept(USER_ID_1, improvementMock);

        verify(socketIoService, times(timesMessage)).sendMessage(eq(USER_ID_1), eq(UNIT_TYPE_CHANGE), any());
        if (timesMessage == 1) {
            verify(spiSpy, times(1)).setRequirementGroups(null);
            var sentMessage = messageContentAnswer.getResult();
            verify(unitTypeResponseMock, times(1)).setComputedMaxCount(expectedComputeMaxCount);
            verify(unitTypeResponseMock, times(timesSetUserBuilt)).setUserBuilt(expectedUserBuilt);
            verify(unitTypeResponseMock, times(1)).setUsed(expectedIsUsed);
            assertThat(sentMessage).isNotEmpty();
            var unitTypeResponse = sentMessage.get(0);
            assertThat(unitTypeResponse).isSameAs(unitTypeResponseMock);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @ParameterizedTest
    @CsvSource({
            "true,true,true",
            "false,true,false",
            "false,false,true",
            "false,false,false"
    })
    void canDoMission_should_work(boolean expected, boolean firstUnitTypeCan, boolean secondUnitTypeCan) {
        var firstUnitType = givenUnitType();
        var secondUnitType = givenUnitType(SECOND_UNIT_TYPE_ID);
        var user = givenUser1();
        var targetPlanet = givenTargetPlanet();
        var missionType = MissionType.EXPLORE;
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanet, firstUnitType, missionType)).willReturn(firstUnitTypeCan);
        given(entityCanDoMissionChecker.canDoMission(user, targetPlanet, secondUnitType, missionType)).willReturn(secondUnitTypeCan);

        assertThat(unitTypeBo.canDoMission(user, targetPlanet, List.of(firstUnitType, secondUnitType), missionType)).isEqualTo(expected);
        verify(entityCanDoMissionChecker, atLeastOnce()).canDoMission(eq(user), eq(targetPlanet), or(eq(firstUnitType), eq(secondUnitType)), eq(missionType));
    }

    @ParameterizedTest
    @MethodSource("checkWouldReachUnitTypeLimit_should_work_arguments")
    void checkWouldReachUnitTypeLimit_should_work(
            UnitType shareCountRoot,
            UnitType unitTypeUsedForCount,
            Long countByUserAndUnitType,
            Long countByUserAndSharedCountUnitType,
            Long expectedComputeMaxCount
    ) {
        var user = givenUser1();
        var faction = givenFaction();
        user.setFaction(faction);
        var unitType = givenUnitType();
        unitType.setShareMaxCount(shareCountRoot);
        given(unitTypeRepository.findById(UNIT_TYPE_ID)).willReturn(Optional.of(unitType));
        if (shareCountRoot != null) {
            given(unitTypeRepository.findById(shareCountRoot.getId())).willReturn(Optional.of(shareCountRoot));
        }
        given(obtainedUnitRepository.countByUserAndUnitType(user, unitTypeUsedForCount)).willReturn(countByUserAndUnitType);
        given(obtainedUnitRepository.countByUserAndSharedCountUnitType(user, unitTypeUsedForCount)).willReturn(countByUserAndSharedCountUnitType);
        given(improvementBo.findUserImprovement(user)).willReturn(mock(GroupedImprovement.class));
        given(improvementBo.computeImprovementValue(anyDouble(), anyDouble())).willReturn(Double.valueOf(expectedComputeMaxCount));

        unitTypeBo.checkWouldReachUnitTypeLimit(user, UNIT_TYPE_ID, 2L);
    }

    @Test
    void checkWouldReachUnitTypeLimit_should_throw() {
        var user = givenUser1();
        var faction = givenFaction();
        user.setFaction(faction);
        var unitType = givenUnitType();
        unitType.setMaxCount(5L);
        given(unitTypeRepository.findById(UNIT_TYPE_ID)).willReturn(Optional.of(unitType));
        given(obtainedUnitRepository.countByUserAndUnitType(user, unitType)).willReturn(50L);
        given(improvementBo.findUserImprovement(user)).willReturn(mock(GroupedImprovement.class));
        given(improvementBo.computeImprovementValue(anyDouble(), anyDouble())).willReturn(Double.valueOf(5L));

        assertThatThrownBy(() -> unitTypeBo.checkWouldReachUnitTypeLimit(user, UNIT_TYPE_ID, 3L))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("try outside of Spain");
    }

    private static Stream<Arguments> init_should_work_arguments() {
        var faction = givenFaction();
        return Stream.of(
                Arguments.of(givenImprovementUnitType(ImprovementTypeEnum.ATTACK), 0, null, null, 0L, 0, null, false),
                Arguments.of(
                        givenImprovementUnitType(ImprovementTypeEnum.AMOUNT), 1, faction, givenFactionUnitType(), 30L, 1, 5L, false
                ),
                Arguments.of(givenImprovementUnitType(ImprovementTypeEnum.AMOUNT), 1, faction, null, 0L, 0, 0L, true)
        );
    }

    private static Stream<Arguments> checkWouldReachUnitTypeLimit_should_work_arguments() {
        var shareCountRoot = givenUnitType(149);
        shareCountRoot.setMaxCount(40L);
        var unitType = givenUnitType();
        return Stream.of(
                Arguments.of(null, unitType, 2L, 2L, 10L),
                Arguments.of(shareCountRoot, shareCountRoot, 2L, 2L, 10L),
                Arguments.of(null, unitType, null, null, 10L)
        );
    }
}
