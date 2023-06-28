package com.kevinguanchedarias.owgejava.business;


import com.kevinguanchedarias.owgejava.entity.CommonEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

import static com.kevinguanchedarias.owgejava.mock.UnitMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommonEntityServiceTest {

    @Mock
    private JpaRepository<CommonEntity<Integer>, Integer> repository;

    @InjectMocks
    private CommonEntityService commonEntityService;

    @Test
    void entitiesIdsToDto_should_work() {
        var unit = givenUnit1();
        given(repository.findAllById(Set.of(UNIT_ID_1))).willReturn(List.of(unit));

        var retVal = commonEntityService.entitiesByIdToDto(repository, Set.of(UNIT_ID_1));

        assertThat(retVal).containsKey(UNIT_ID_1);
        var dto = retVal.get(UNIT_ID_1);
        assertThat(dto.getId()).isEqualTo(UNIT_ID_1);
        assertThat(dto.getName()).isEqualTo(UNIT_NAME);
        assertThat(dto.getDescription()).isEqualTo(UNIT_DESCRIPTION);
    }
}
