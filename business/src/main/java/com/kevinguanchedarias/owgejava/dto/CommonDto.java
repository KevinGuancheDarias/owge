/**
 *
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.CommonEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class CommonDto<K extends Serializable, E extends CommonEntity<K>> implements DtoFromEntity<E> {
    @EqualsAndHashCode.Include
    private K id;
    private String name;
    private String description;

    @Override
    public void dtoFromEntity(E entity) {
        id = entity.getId();
        name = entity.getName();
        description = entity.getDescription();
    }
}
