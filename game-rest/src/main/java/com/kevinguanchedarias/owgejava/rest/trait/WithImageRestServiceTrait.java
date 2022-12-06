package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.dto.CommonDtoWithImageStore;
import com.kevinguanchedarias.owgejava.entity.CommonEntityWithImageStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public interface WithImageRestServiceTrait<
        N extends Number,
        E extends CommonEntityWithImageStore<N>,
        D extends CommonDtoWithImageStore<N, E>,
        R extends JpaRepository<E, N>
        >
        extends CrudRestServiceNoOpEventsTrait<D, E> {

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    RestCrudConfigBuilder<N, E, R, D> getRestCrudConfigBuilder();

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    default Optional<E> beforeSave(D parsedDto, E entity) {
        if (parsedDto.getImage() != null) {
            entity.setImage(getRestCrudConfigBuilder().build().getBeanFactory().getBean(ImageStoreBo.class)
                    .findByIdOrDie(parsedDto.getImage()));
        }
        return CrudRestServiceNoOpEventsTrait.super.beforeSave(parsedDto, entity);
    }

}
