package com.kevinguanchedarias.owgejava.business.unit.loader;

import com.kevinguanchedarias.owgejava.business.unit.StoredUnitBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnitBasicInfoProjection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = StoredUnitDataLoader.class
)
@MockBean(StoredUnitBo.class)
class StoredUnitDataLoaderTest {
    private final StoredUnitDataLoader storedUnitDataLoader;
    private final StoredUnitBo storedUnitBo;

    @Autowired
    StoredUnitDataLoaderTest(StoredUnitDataLoader storedUnitDataLoader, StoredUnitBo storedUnitBo) {
        this.storedUnitDataLoader = storedUnitDataLoader;
        this.storedUnitBo = storedUnitBo;
    }

    @Test
    void addInformationToDto_should_work() {
        var ou = givenObtainedUnit1();
        var ouDto = new ObtainedUnitDto();
        var ouBasic = givenObtainedUnitBasicInfoProjection();
        given(storedUnitBo.findStoredUnits(ou)).willReturn(List.of(ouBasic));

        storedUnitDataLoader.addInformationToDto(ou, ouDto);

        assertThat(ouDto.getStoredUnits()).contains(ouBasic);
    }
}
