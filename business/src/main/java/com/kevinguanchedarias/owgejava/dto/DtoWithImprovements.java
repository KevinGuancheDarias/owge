/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import org.hibernate.Hibernate;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface DtoWithImprovements {
    ImprovementDto getImprovement();

    void setImprovement(ImprovementDto improvementDto);

    /**
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    default <K> void dtoFromEntity(EntityWithImprovements<K> entity) {
        if (Hibernate.isInitialized(entity.getImprovement()) && entity.getImprovement() != null) {
            setImprovement(new ImprovementDto());
            getImprovement().dtoFromEntity(entity.getImprovement());
        }
    }
}
