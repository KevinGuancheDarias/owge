package com.kevinguanchedarias.owgejava.entity.util;

import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructEntityRefreshUtilService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.support.Repositories;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.givenUnit1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SuppressWarnings("rawtypes")
@SpringBootTest(
        classes = NonPostConstructEntityRefreshUtilService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        EntityManager.class,
        JpaRepository.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class EntityRefreshUtilServiceTest {
    private static final String KNOWN_EXCEPTION_MESSAGE_START = "No repository for ";


    private final NonPostConstructEntityRefreshUtilService entityRefreshUtilService;
    private final EntityManager entityManager;
    private final JpaRepository jpaRepository;

    private Repositories repositoriesMock;

    @BeforeEach
    void setup_init() {
        try (var mockedConstructor = mockConstruction(Repositories.class)) {
            entityRefreshUtilService.realInit();
            repositoriesMock = mockedConstructor.constructed().getFirst();
        }
    }

    @Test
    void refresh_should_use_entity_manager_if_entity_is_managed() {
        var unit = givenUnit1();
        given(entityManager.contains(unit)).willReturn(true);

        var retVal = entityRefreshUtilService.refresh(unit);

        assertThat(retVal).isSameAs(unit);
        verify(entityManager, times(1)).refresh(unit);
    }

    @SuppressWarnings("unchecked")
    @Test
    void refresh_should_use_repository_when_entity_is_not_managed() {
        var unit = givenUnit1();
        var unitFromRepository = givenUnit1().toBuilder().build();
        unitFromRepository.setId(4);
        given(repositoriesMock.getRepositoryFor(Unit.class)).willReturn(Optional.of(jpaRepository));
        given(jpaRepository.getReferenceById(UNIT_ID_1)).willReturn(unitFromRepository);

        var retVal = entityRefreshUtilService.refresh(unit);

        assertThat(retVal).isEqualTo(unitFromRepository);
    }

    @Test
    void refresh_should_throw_when_repository_is_missing() {
        var unit = givenUnit1();
        assertThatThrownBy(() -> entityRefreshUtilService.refresh(unit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith(KNOWN_EXCEPTION_MESSAGE_START);
    }

    @Test
    void refresh_should_throw_when_repository_is_not_a_jpa_repository() {
        var unit = givenUnit1();
        given(repositoriesMock.getRepositoryFor(Unit.class)).willReturn(Optional.of(mock(CrudRepository.class)));
        assertThatThrownBy(() -> entityRefreshUtilService.refresh(unit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith(KNOWN_EXCEPTION_MESSAGE_START);
    }

    @SuppressWarnings("unchecked")
    @Test
    void refresh_should_throw_when_repository_exists_but_entity_not() {
        var unit = givenUnit1();
        given(repositoriesMock.getRepositoryFor(Unit.class)).willReturn(Optional.of(jpaRepository));
        assertThatThrownBy(() -> entityRefreshUtilService.refresh(unit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith(KNOWN_EXCEPTION_MESSAGE_START);

        verify(jpaRepository, times(1)).getReferenceById(UNIT_ID_1);
    }
}
