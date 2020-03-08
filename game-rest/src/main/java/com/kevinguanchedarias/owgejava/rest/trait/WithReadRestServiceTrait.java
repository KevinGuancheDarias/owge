/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.pojo.RestCrudConfig;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface WithReadRestServiceTrait<N extends Number, E extends EntityWithId<N>, S extends BaseBo<N, E, D>, D extends DtoFromEntity<E>>
		extends CrudRestServiceNoOpEventsTrait<D, E> {
	public RestCrudConfigBuilder<N, E, S, D> getRestCrudConfigBuilder();

	/**
	 * GET mapping that returns all entities (converted to the Dto)
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping
	public default List<D> findAll() {
		RestCrudConfig<N, E, S, D> config = getRestCrudConfigBuilder().build();
		if (!config.getSupportedOperationsBuilder().build().canReadAll()) {
			AccessDeniedException.fromUnsupportedOperation();
		}
		List<D> retVal = new ArrayList<>();
		config.getBoService().findAll().forEach(currentEntity -> {
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
	 * @param id
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("{id}")
	public default D findOneById(@PathVariable N id) {
		RestCrudConfig<N, E, S, D> config = getRestCrudConfigBuilder().build();
		if (!config.getSupportedOperationsBuilder().build().canReadAll()) {
			AccessDeniedException.fromUnsupportedOperation();
		}
		E entity = config.getBoService().findByIdOrDie(id);
		D dto = getDtoUtilService().dtoFromEntity(config.getDtoClass(), entity);
		dto = beforeRequestEnd(dto, entity).orElse(dto);
		return dto;
	}

	private DtoUtilService getDtoUtilService() {
		return getRestCrudConfigBuilder().build().getBeanFactory().getBean(DtoUtilService.class);
	}
}
