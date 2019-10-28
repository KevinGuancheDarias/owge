/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.Optional;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.ImageStoreBo;
import com.kevinguanchedarias.owgejava.dto.CommonDtoWithImageStore;
import com.kevinguanchedarias.owgejava.entity.CommonEntityWithImageStore;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface WithImageRestServiceTrait<N extends Number, E extends CommonEntityWithImageStore<N>, D extends CommonDtoWithImageStore<N, E>, S extends BaseBo<N, E, D>>
		extends CrudRestServiceNoOpEventsTrait<D, E> {

	/**
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RestCrudConfigBuilder<N, E, S, D> getRestCrudConfigBuilder();

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public default Optional<E> beforeSave(D parsedDto, E entity) {
		if (parsedDto.getImage() != null) {
			entity.setImage(getRestCrudConfigBuilder().build().getBeanFactory().getBean(ImageStoreBo.class)
					.findByIdOrDie(parsedDto.getImage()));
		}
		return CrudRestServiceNoOpEventsTrait.super.beforeSave(parsedDto, entity);
	}

}
