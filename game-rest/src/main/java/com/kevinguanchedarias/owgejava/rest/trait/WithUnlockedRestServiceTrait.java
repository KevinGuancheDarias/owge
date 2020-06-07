/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.bind.annotation.GetMapping;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.business.WithUnlockableBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;

/**
 *
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface WithUnlockedRestServiceTrait<N extends Number, E extends EntityWithId<N>, S extends WithUnlockableBo<N, E, D>, D extends DtoFromEntity<E>> {
	public RestCrudConfigBuilder<N, E, S, D> getRestCrudConfigBuilder();

	/**
	 * Finds out the unlocked by the current logged in user
	 *
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@GetMapping("/findUnlocked")
	public default List<D> findUnlocked() {
		BeanFactory beanFactory = getRestCrudConfigBuilder().build().getBeanFactory();
		UserStorageBo userStorageBo = beanFactory.getBean(UserStorageBo.class);

		S bo = getRestCrudConfigBuilder().build().getBoService();
		List<D> retVal = bo.toDto(bo.findUnlocked(userStorageBo.findLoggedIn()));
		retVal.forEach(this::alterDto);
		return retVal;
	}

	/**
	 * Override, to run an action on the unlocked item DTO, to transform the DTO
	 *
	 * @param input
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public default D alterDto(D input) {
		return input;
	}
}
