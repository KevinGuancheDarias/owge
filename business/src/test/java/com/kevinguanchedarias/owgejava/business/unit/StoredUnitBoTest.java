package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.StoredUnitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnitBasicInfoProjection;
import static com.kevinguanchedarias.owgejava.mock.unit.StoredUnitMock.STORED_UNIT_TARGET_OU;
import static com.kevinguanchedarias.owgejava.mock.unit.StoredUnitMock.givenStoredUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = StoredUnitBo.class
)
@MockBean({
        StoredUnitRepository.class,
        ObtainedUnitRepository.class
})
class StoredUnitBoTest {
    private final StoredUnitBo storedUnitBo;
    private final StoredUnitRepository storedUnitRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    public StoredUnitBoTest(
            StoredUnitBo storedUnitBo,
            StoredUnitRepository storedUnitRepository,
            ObtainedUnitRepository obtainedUnitRepository
    ) {
        this.storedUnitBo = storedUnitBo;
        this.storedUnitRepository = storedUnitRepository;
        this.obtainedUnitRepository = obtainedUnitRepository;
    }

    @Test
    void findStoredUnits_should_work() {
        var ou = givenObtainedUnit1();
        var su = givenStoredUnit();
        var ouBasic = givenObtainedUnitBasicInfoProjection();
        given(storedUnitRepository.findByOwnerObtainedUnitId(ou)).willReturn(List.of(su));
        given(obtainedUnitRepository.findBaseInfo(STORED_UNIT_TARGET_OU)).willReturn(ouBasic);

        var result = storedUnitBo.findStoredUnits(ou);

        assertThat(result).contains(ouBasic);
    }
}
