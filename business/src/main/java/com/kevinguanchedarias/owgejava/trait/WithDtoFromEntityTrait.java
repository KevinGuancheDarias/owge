package com.kevinguanchedarias.owgejava.trait;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import org.springframework.beans.BeanUtils;

/**
 * Copies common properties by default when invoking <i>dtoFromEntity</i>
 *
 * @param <E> Target entity
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 */
public interface WithDtoFromEntityTrait<E> extends DtoFromEntity<E> {

    @Override
    default void dtoFromEntity(E entity) {
        BeanUtils.copyProperties(entity, this);
    }
}
