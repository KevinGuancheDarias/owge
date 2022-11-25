package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithImprovements;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface CrudWithFullRestService<
        N extends Number,
        E extends EntityWithImprovements<N>,
        R extends JpaRepository<E, N>,
        D extends DtoFromEntity<E>
        > extends CrudWithRequirementsRestServiceTrait<N, E, R, D>, CrudWithImprovementsRestServiceTrait<N, E, R, D>,
        WithReadRestServiceTrait<N, E, R, D>, CrudRestServiceTrait<N, E, R, D>, WithDeleteRestServiceTrait<N, E, R, D> {

    @Override
    default Optional<D> beforeRequestEnd(D dto, E savedEntity) {
        return CrudWithImprovementsRestServiceTrait.super.beforeRequestEnd(dto, savedEntity);
    }

}
