/**
 *
 */
package com.kevinguanchedarias.owgejava.enumerations;

import com.kevinguanchedarias.owgejava.entity.ObjectEntity;

/**
 * Code representation of the items in the {@link ObjectEntity} table
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public enum ObjectEnum {
    UPGRADE, UNIT, TIME_SPECIAL, SPEED_IMPACT_GROUP, REQUIREMENT_GROUP;

    public boolean isObject(ObjectEntity objectEntity) {
        return objectEntity != null && name().equals(objectEntity.getCode());
    }
}
