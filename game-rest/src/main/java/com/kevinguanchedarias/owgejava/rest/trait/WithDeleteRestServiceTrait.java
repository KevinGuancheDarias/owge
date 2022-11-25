package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.pojo.RestCrudConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface WithDeleteRestServiceTrait<N extends Number, E extends EntityWithId<N>, R extends JpaRepository<E, N>, D extends DtoFromEntity<E>>
        extends CrudRestServiceNoOpEventsTrait<D, E> {
    RestCrudConfigBuilder<N, E, R, D> getRestCrudConfigBuilder();

    /**
     * DELETE mapping to delete an entity
     *
     * @param id The path param id
     * @throws NotFoundException If there is not an entity with the specified
     *                           <i>id</i>
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    @DeleteMapping("{id}")
    default ResponseEntity<Void> delete(@PathVariable() N id) {
        RestCrudConfig<N, E, R, D> config = getRestCrudConfigBuilder().build();
        if (!config.getSupportedOperationsBuilder().build().canDeleteAny()) {
            throw AccessDeniedException.fromUnsupportedOperation();
        }
        config.getRepository().deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
