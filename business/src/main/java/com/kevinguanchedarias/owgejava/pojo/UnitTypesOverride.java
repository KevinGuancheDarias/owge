package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;
import lombok.ToString;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.10.0
 */
@ToString
public class UnitTypesOverride extends UnitTypeDto {
    private Long overrideMaxCount;

    /**
     * @return the overrideMaxCount
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.10.0
     */
    public Long getOverrideMaxCount() {
        return overrideMaxCount;
    }

    /**
     * @param overrideMaxCount the overrideMaxCount to set
     * @author Kevin Guanche Darias
     * @since 0.10.0
     */
    public void setOverrideMaxCount(Long overrideMaxCount) {
        this.overrideMaxCount = overrideMaxCount;
    }

}
