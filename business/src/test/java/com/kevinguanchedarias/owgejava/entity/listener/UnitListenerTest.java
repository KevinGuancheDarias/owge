package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ImprovementMock.givenImprovement;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
        classes = UnitListener.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ImprovementBo.class,
        ObtainedUnitRepository.class,
        ObjectRelationBo.class,
        ObtainedUnitEventEmitter.class
})
class UnitListenerTest {
    private final UnitListener unitListener;
    private final ImprovementBo improvementBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObjectRelationBo objectRelationBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;

    @Autowired
    public UnitListenerTest(
            UnitListener unitListener,
            ImprovementBo improvementBo,
            ObtainedUnitRepository obtainedUnitRepository,
            ObjectRelationBo objectRelationBo,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter
    ) {
        this.unitListener = unitListener;
        this.improvementBo = improvementBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.objectRelationBo = objectRelationBo;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
    }

    @SneakyThrows
    @Test
    void onSaveClearCacheIfRequired_should_work() {
        var unit = givenUnit1();
        var method = UnitListener.class.getMethod("onSaveClearCacheIfRequired", Unit.class);

        unitListener.onSaveClearCacheIfRequired(unit);

        verify(improvementBo, times(1)).clearCacheEntriesIfRequired(eq(unit), any(ObtainedUnitImprovementCalculationService.class));
        assertThat(method.getAnnotation(PostUpdate.class)).isNotNull();
        assertThat(method.getAnnotation(PostPersist.class)).isNotNull();
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("onDeleteClearCacheIfRequired_should_work_arguments")
    void onDeleteClearCacheIfRequired_should_work(Improvement unitImprovement, int expectedCalls) {
        var or = givenObjectRelation();
        var ou = givenObtainedUnit1();
        var unit = ou.getUnit();
        unit.setImprovement(unitImprovement);
        var user = ou.getUser();
        given(objectRelationBo.findOneOpt(ObjectEnum.UNIT, UNIT_ID_1)).willReturn(Optional.of(or));
        given(obtainedUnitRepository.findByUnit(unit)).willReturn(List.of(ou));

        unitListener.onDeleteClearCacheIfRequired(unit);

        verify(objectRelationBo, times(1)).delete(or);
        verify(obtainedUnitRepository, times(1)).deleteByUnit(unit);
        verify(improvementBo, times(1)).clearCacheEntriesIfRequired(eq(unit), any(ObtainedUnitImprovementCalculationService.class));
        verify(obtainedUnitEventEmitter, times(1)).emitObtainedUnits(user);
        verify(improvementBo, times(expectedCalls)).emitUserImprovement(user);
        assertThat(UnitListener.class.getMethod("onDeleteClearCacheIfRequired", Unit.class).getAnnotation(PreRemove.class)).isNotNull();
    }

    private static Stream<Arguments> onDeleteClearCacheIfRequired_should_work_arguments() {
        return Stream.of(
                Arguments.of(givenImprovement(), 1),
                Arguments.of(null, 0)
        );
    }
}
