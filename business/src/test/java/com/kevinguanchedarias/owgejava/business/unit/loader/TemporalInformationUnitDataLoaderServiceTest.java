package com.kevinguanchedarias.owgejava.business.unit.loader;

import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.repository.jdbc.ObtainedUnitTemporalInformationRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitTemporalInformationMock.givenObtainedUnitTemporalInformation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = TemporalInformationUnitDataLoaderService.class
)
@MockBean(ObtainedUnitTemporalInformationRepository.class)
class TemporalInformationUnitDataLoaderServiceTest {
    private final ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository;
    private final TemporalInformationUnitDataLoaderService temporalInformationUnitDataLoaderService;

    @Autowired
    public TemporalInformationUnitDataLoaderServiceTest(
            ObtainedUnitTemporalInformationRepository obtainedUnitTemporalInformationRepository,
            TemporalInformationUnitDataLoaderService temporalInformationUnitDataLoaderService
    ) {
        this.obtainedUnitTemporalInformationRepository = obtainedUnitTemporalInformationRepository;
        this.temporalInformationUnitDataLoaderService = temporalInformationUnitDataLoaderService;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addInformationToDto_should_work(boolean hasExpirationId) {
        var ou = givenObtainedUnit1();
        var ouDto = new ObtainedUnitDto();
        var expirationId = 19L;
        var temporalInformation = givenObtainedUnitTemporalInformation();
        if (hasExpirationId) {
            ou.setExpirationId(expirationId);
        }
        given(obtainedUnitTemporalInformationRepository.findById(expirationId)).willReturn(Optional.of(temporalInformation));

        temporalInformationUnitDataLoaderService.addInformationToDto(ou, ouDto);

        if (hasExpirationId) {
            assertThat(ouDto.getTemporalInformation()).isEqualTo(temporalInformation);
            assertThat(ouDto.getTemporalInformation().getPendingMillis()).isNotNull();
        } else {
            assertThat(ouDto.getTemporalInformation()).isNull();
        }
    }
}
