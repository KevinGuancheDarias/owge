package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.hotfix.ObtainedUnitHotFixRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.OBTAINED_UNIT_1_COUNT;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.PlanetMock.SOURCE_PLANET_ID;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
        classes = ObtainedUnitHotFixRepository.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        ObtainedUnitRepository.class,
        EntityManager.class
})
@AllArgsConstructor(onConstructor_ = @Autowired)
class ObtainedUnitHotFixRepositoryTest {

    private final ObtainedUnitHotFixRepository obtainedUnitHotFixRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final EntityManager entityManager;

    @ParameterizedTest
    @MethodSource("methodsToTest")
    void hotfix_methods_should_work(Supplier<ObtainedUnit> methodToTest) {
        var ou = givenObtainedUnit1();
        givenRepoInvoked(List.of(ou));

        var retVal = methodToTest.get();

        assertThat(retVal).isSameAs(ou);

        verify(obtainedUnitRepository, never()).deleteAll(anyList());
    }

    @ParameterizedTest
    @MethodSource("methodsToTest")
    void hotfix_methods_should_handle_empty_list(Supplier<ObtainedUnit> methodToTest) {
        givenRepoInvoked(List.of());

        var retVal = methodToTest.get();

        assertThat(retVal).isNull();
    }

    @ParameterizedTest
    @MethodSource("methodsToTest")
    void hotfix_methods_should_handle_duplicates(Supplier<ObtainedUnit> methodToTest) {
        var ou = givenObtainedUnit1();
        givenRepoInvoked(List.of(ou, ou));
        given(obtainedUnitRepository.save(ou)).willAnswer(AdditionalAnswers.returnsFirstArg());

        var retVal = methodToTest.get();

        verify(obtainedUnitRepository, times(1)).deleteAll(List.of(ou, ou));
        verify(entityManager, times(1)).detach(ou);
        var captor = ArgumentCaptor.forClass(ObtainedUnit.class);
        verify(obtainedUnitRepository, times(1)).save(captor.capture());
        assertThat(retVal).isSameAs(captor.getValue());
        assertThat(retVal.getCount()).isEqualTo(OBTAINED_UNIT_1_COUNT + OBTAINED_UNIT_1_COUNT);
        assertThat(retVal.getId()).isNull();
    }

    private void givenRepoInvoked(List<ObtainedUnit> ouList) {
        given(obtainedUnitRepository.findByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
                USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID
        )).willReturn(ouList);
        given(obtainedUnitRepository.findByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(
                USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID, MissionType.EXPLORE.name()
        )).willReturn(ouList);
    }

    private Stream<Supplier<ObtainedUnit>> methodsToTest() {
        return Stream.of(
                () -> obtainedUnitHotFixRepository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
                        USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID),
                () -> obtainedUnitHotFixRepository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(
                        USER_ID_1, UNIT_ID_1, SOURCE_PLANET_ID, MissionType.EXPLORE.name())
        );
    }

}
