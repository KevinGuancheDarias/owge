/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import com.kevinguanchedarias.owgejava.util.ImprovementDtoUtil;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface DtoWithImprovements {
    ImprovementDto getImprovement();

    void setImprovement(ImprovementDto improvementDto);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    default <K> void dtoFromEntity(EntityWithImprovements<K> entity) {
        setImprovement(ImprovementDtoUtil.dtoFromEntity(entity));
    }
}
