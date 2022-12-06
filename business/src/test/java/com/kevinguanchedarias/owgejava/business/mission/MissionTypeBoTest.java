package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionTypeMock.givenMissinType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        classes = MissionTypeBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean(MissionTypeRepository.class)
class MissionTypeBoTest {
    private final MissionTypeBo missionTypeBo;
    private final MissionTypeRepository missionTypeRepository;

    @Autowired
    MissionTypeBoTest(MissionTypeBo missionTypeBo, MissionTypeRepository missionTypeRepository) {
        this.missionTypeBo = missionTypeBo;
        this.missionTypeRepository = missionTypeRepository;
    }

    @Test
    void resolve_should_work() {
        assertThat(missionTypeBo.resolve(givenExploreMission())).isEqualTo(MissionType.EXPLORE);
        assertThat(missionTypeBo.resolve(null)).isNull();
    }

    @Test
    void find_should_work() {
        var missionTypeEntity = givenMissinType(MissionType.EXPLORE);
        given(missionTypeRepository.findOneByCode(MissionType.EXPLORE.name())).willReturn(Optional.of(missionTypeEntity));

        assertThat(missionTypeBo.find(MissionType.EXPLORE)).isEqualTo(missionTypeEntity);
    }

    @Test
    void find_should_throw_on_type_not_in_db() {
        var missionType = MissionType.EXPLORE;

        assertThatThrownBy(() -> missionTypeBo.find(missionType))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining(missionType.name() + " was found in the database");
    }
}
