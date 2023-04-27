package com.kevinguanchedarias.owgejava.business.event.listener.timespecial;

import com.kevinguanchedarias.owgejava.business.ScheduledTasksManagerService;
import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.schedule.TemporalUnitScheduleListener;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.util.UnitImprovementUtilService;
import com.kevinguanchedarias.owgejava.context.OwgeContextHolder;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.jdbc.ObtainedUnitTemporalInformation;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveTemporalUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID;
import static com.kevinguanchedarias.owgejava.mock.ActiveTimeSpecialMock.givenActiveTimeSpecialMock;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.OBJECT_RELATION_ID;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.*;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.DESTINATION_ID;
import static com.kevinguanchedarias.owgejava.mock.RuleMock.givenRuleDto;
import static com.kevinguanchedarias.owgejava.mock.TimeSpecialMock.TIME_SPECIAL_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(
        classes = TemporalUnitsListener.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        RuleBo.class,
        UnitRepository.class,
        ObtainedUnitTemporalInformationRepository.class,
        ScheduledTasksManagerService.class,
        ObtainedUnitRepository.class,
        ObtainedUnitEventEmitter.class,
        UnitImprovementUtilService.class,
        PlanetRepository.class,
        ObjectRelationsRepository.class
})
class TemporalUnitListenerTest {
    private final TemporalUnitsListener temporalUnitsListener;
    private final RuleBo ruleBo;
    private final UnitRepository unitRepository;
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;
    private final ScheduledTasksManagerService scheduledTasksManagerService;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final UnitImprovementUtilService unitImprovementUtilService;
    private final PlanetRepository planetRepository;
    private final ObjectRelationsRepository objectRelationsRepository;

    @Autowired
    public TemporalUnitListenerTest(
            TemporalUnitsListener temporalUnitsListener,
            RuleBo ruleBo,
            UnitRepository unitRepository,
            ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository,
            ScheduledTasksManagerService scheduledTasksManagerService,
            ObtainedUnitRepository obtainedUnitRepository,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter,
            UnitImprovementUtilService unitImprovementUtilService,
            PlanetRepository planetRepository,
            ObjectRelationsRepository objectRelationsRepository
    ) {
        this.temporalUnitsListener = temporalUnitsListener;
        this.ruleBo = ruleBo;
        this.unitRepository = unitRepository;
        this.obtainedUnitTemporalInformationRepository = obtainedUnitTemporalInformationRepository;
        this.scheduledTasksManagerService = scheduledTasksManagerService;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
        this.unitImprovementUtilService = unitImprovementUtilService;
        this.planetRepository = planetRepository;
        this.objectRelationsRepository = objectRelationsRepository;
    }

    @Test
    void onTimeSpecialActivated_should_be_properly_annotated() throws NoSuchMethodException {
        var method = TemporalUnitsListener.class.getMethod("onTimeSpecialActivated", ActiveTimeSpecial.class);
        assertThat(method.getAnnotation(EventListener.class)).isNotNull();
        var transactionalAnnotation = method.getAnnotation(TransactionalEventListener.class);
        assertThat(transactionalAnnotation).isNotNull();
        assertThat(transactionalAnnotation.phase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ParameterizedTest
    @MethodSource("onTimeSpecialActivated_parameters")
    void onTimeSpecialActivated_should_work(
            List<String> extraArgs,
            String destinationType,
            Unit unit,
            OwgeContextHolder.OwgeContext owgeContext,
            Planet expectedPlanet,
            CapturedOutput capturedOutput
    ) {
        var or = givenObjectRelation();
        or.setReferenceId(TIME_SPECIAL_ID);
        var ats = givenActiveTimeSpecialMock(TimeSpecialStateEnum.ACTIVE);
        var user = givenUser1();
        var homePlanet = givenSourcePlanet();
        var duration = Long.parseLong(extraArgs.get(0));
        user.setHomePlanet(homePlanet);
        ats.setUser(user);
        var ruleDto = givenRuleDto().toBuilder()
                .destinationType(destinationType)
                .extraArgs((List) extraArgs)
                .build();
        var isValidRule = extraArgs.size() == 2 && ObjectEnum.UNIT.name().equals(destinationType);
        var idGenerator = new AtomicLong(0);
        given(ruleBo.findByOriginTypeAndOriginIdAndType(
                ObjectEnum.TIME_SPECIAL.name(),
                TIME_SPECIAL_ID,
                TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID
        )).willReturn(List.of(ruleDto, ruleDto));
        given(unitRepository.findById((int) DESTINATION_ID)).willReturn(Optional.ofNullable(unit));
        given(planetRepository.getReferenceById(TARGET_PLANET_ID)).willReturn(givenTargetPlanet());
        given(objectRelationsRepository.findOneByObjectCodeAndReferenceId(ObjectEnum.TIME_SPECIAL.name(), TIME_SPECIAL_ID))
                .willReturn(or);
        doAnswer(invocationOnMock -> {
            var entity = invocationOnMock.getArgument(0, ObtainedUnitTemporalInformation.class);
            entity.setId(idGenerator.incrementAndGet());
            return entity;
        }).when(obtainedUnitTemporalInformationRepository).save(any());

        try (var owgeContextHolderMockStatic = mockStatic(OwgeContextHolder.class)) {
            owgeContextHolderMockStatic.when(OwgeContextHolder::get).thenReturn(Optional.ofNullable(owgeContext));

            temporalUnitsListener.onTimeSpecialActivated(ats);

            verify(unitRepository, times(isValidRule ? 2 : 0)).findById((int) DESTINATION_ID);
            var captor = ArgumentCaptor.forClass(ObtainedUnitTemporalInformation.class);
            var shouldProcessUnit = isValidRule && unit != null;
            verify(obtainedUnitTemporalInformationRepository, times(shouldProcessUnit ? 1 : 0)).save(captor.capture());
            if (shouldProcessUnit) {
                var saved = captor.getValue();
                assertThat(saved.getDuration()).isEqualTo(duration);
                assertThat(saved.getExpiration())
                        .isAfter(Instant.now())
                        .isBefore(Instant.now().plusSeconds(saved.getDuration() + 1));
                assertThat(saved.getRelationId()).isEqualTo(OBJECT_RELATION_ID);
                ArgumentCaptor<List<ObtainedUnit>> savedUnits = ArgumentCaptor.forClass(List.class);
                verify(obtainedUnitRepository, times(1)).saveAll(savedUnits.capture());
                verify(obtainedUnitEventEmitter, times(1)).emitObtainedUnits(user);
                verify(unitImprovementUtilService, times(1)).maybeTriggerClearImprovement(eq(user), any());
                var units = savedUnits.getValue();
                assertThat(units).hasSize(1);
                var ou = units.get(0);
                assertThat(ou.getUnit()).isEqualTo(unit);
                assertThat(ou.getUser()).isEqualTo(user);
                assertThat(ou.getSourcePlanet()).isEqualTo(expectedPlanet);
                assertThat(ou.getCount()).isEqualTo(8);
                assertThat(ou.getExpirationId()).isNotNull();
                var scheduledTaskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
                verify(scheduledTasksManagerService, times(1)).registerEvent(scheduledTaskCaptor.capture(), eq(duration));
                var value = scheduledTaskCaptor.getValue();
                assertThat(value.getType()).isEqualTo(TemporalUnitScheduleListener.TASK_NAME);
                assertThat(value.getContent()).isNotNull();
            }
            if (isValidRule && unit == null) {
                assertThat(capturedOutput.getOut())
                        .contains("Unit with id")
                        .contains("doesn't exists for rule");
            }
        }

    }

    private static Stream<Arguments> onTimeSpecialActivated_parameters() {
        var args = List.of("60", "8");
        var withPlanetContext = new OwgeContextHolder.OwgeContext(TARGET_PLANET_ID, null);
        var knownHomePlanet = givenSourcePlanet();
        return Stream.of(
                Arguments.of(args, ObjectEnum.UNIT.name(), null, null, knownHomePlanet),
                Arguments.of(args, ObjectEnum.UNIT.name(), givenUnit1(), withPlanetContext, givenTargetPlanet()),
                Arguments.of(args, "NO_VALID_DESTINATION_TYPE", null, null, knownHomePlanet),
                Arguments.of(List.of("555"), ObjectEnum.UNIT.name(), null, null, knownHomePlanet)
        );
    }
}
