package com.kevinguanchedarias.owgejava.business.timespecial;

import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.business.WithUnlockableBo;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UnlockableTimeSpecialService implements WithUnlockableBo<Integer, TimeSpecial, TimeSpecialDto> {
    private final UnlockedRelationBo unlockedRelationBo;

    @Override
    public Class<TimeSpecialDto> getDtoClass() {
        return TimeSpecialDto.class;
    }

    @Override
    public ObjectEnum getObject() {
        return ObjectEnum.TIME_SPECIAL;
    }

    @Override
    public UnlockedRelationBo getUnlockedRelationBo() {
        return unlockedRelationBo;
    }
}
