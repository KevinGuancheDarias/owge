package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.business.WithUnlockableBo;
import com.kevinguanchedarias.owgejava.dto.UnitDto;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UnlockableUnitService implements WithUnlockableBo<Integer, Unit, UnitDto> {
    private final UnlockedRelationBo unlockedRelationBo;

    @Override
    public Class<UnitDto> getDtoClass() {
        return UnitDto.class;
    }

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.UNIT;
    }

    @Override
    public UnlockedRelationBo getUnlockedRelationBo() {
        return unlockedRelationBo;
    }
}
