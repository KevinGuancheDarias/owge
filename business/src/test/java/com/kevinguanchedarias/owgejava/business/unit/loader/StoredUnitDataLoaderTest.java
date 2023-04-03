package com.kevinguanchedarias.owgejava.business.unit.loader;

import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.fake.FakeUnitDataLoader;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = StoredUnitDataLoader.class
)
@MockBean({
        FakeUnitDataLoader.class,
        DtoUtilService.class,
        ObtainedUnitRepository.class
})
class StoredUnitDataLoaderTest {
    private final StoredUnitDataLoader storedUnitDataLoader;
    private final FakeUnitDataLoader unitDataLoader;
    private final DtoUtilService dtoUtilService;
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    StoredUnitDataLoaderTest(
            StoredUnitDataLoader storedUnitDataLoader,
            FakeUnitDataLoader unitDataLoader,
            DtoUtilService dtoUtilService,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.storedUnitDataLoader = storedUnitDataLoader;
        this.unitDataLoader = unitDataLoader;
        this.dtoUtilService = dtoUtilService;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @Test
    void addInformationToDto_should_work() {
        var ou1 = givenObtainedUnit1();
        var storedOu = givenObtainedUnit2();
        ou1.setStoredUnits(List.of(storedOu));
        var ouDto = new ObtainedUnitDto();
        var storedOuDto = mock(ObtainedUnitDto.class);
        given(dtoUtilService.dtoFromEntity(ObtainedUnitDto.class, storedOu)).willReturn(storedOuDto);
        given(obtainedUnitRepository.findByOwnerUnitId(OBTAINED_UNIT_1_ID)).willReturn(List.of(storedOu));

        storedUnitDataLoader.addInformationToDto(ou1, ouDto);

        verify(unitDataLoader, times(1)).addInformationToDto(storedOu, storedOuDto);
        assertThat(ouDto.getStoredUnits())
                .hasSize(1)
                .containsExactly(storedOuDto);

    }
}
