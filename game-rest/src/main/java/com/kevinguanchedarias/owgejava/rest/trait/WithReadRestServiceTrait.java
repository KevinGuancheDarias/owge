/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.pojo.RestCrudConfig;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface WithReadRestServiceTrait<N extends Number, E extends EntityWithId<N>, R extends JpaRepository<E, N>, D extends DtoFromEntity<E>>
        extends CrudRestServiceNoOpEventsTrait<D, E> {
    public RestCrudConfigBuilder<N, E, R, D> getRestCrudConfigBuilder();

    /**
     * GET mapping that returns all entities (converted to the Dto)
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @GetMapping
    default List<D> findAll() {
        RestCrudConfig<N, E, R, D> config = getRestCrudConfigBuilder().build();
        if (!config.getSupportedOperationsBuilder().build().canReadAll()) {
            throw AccessDeniedException.fromUnsupportedOperation();
        }
        List<D> retVal = new ArrayList<>();
        config.getRepository().findAll().forEach(currentEntity -> {
            D dto = getDtoUtilService().dtoFromEntity(config.getDtoClass(), currentEntity);
            dto = beforeRequestEnd(dto, currentEntity).orElse(dto);
            if (filterGetResult(dto, currentEntity)) {
                retVal.add(dto);
            }
        });
        return retVal;
    }

    /**
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @GetMapping("{id}")
    default D findOneById(@PathVariable N id) {
        RestCrudConfig<N, E, R, D> config = getRestCrudConfigBuilder().build();
        if (!config.getSupportedOperationsBuilder().build().canReadAll()) {
            throw AccessDeniedException.fromUnsupportedOperation();
        }
        E entity = SpringRepositoryUtil.findByIdOrDie(config.getRepository(), id);
        D dto = getDtoUtilService().dtoFromEntity(config.getDtoClass(), entity);
        dto = beforeRequestEnd(dto, entity).orElse(dto);
        return dto;
    }

    private DtoUtilService getDtoUtilService() {
        return getRestCrudConfigBuilder().build().getBeanFactory().getBean(DtoUtilService.class);
    }
}
