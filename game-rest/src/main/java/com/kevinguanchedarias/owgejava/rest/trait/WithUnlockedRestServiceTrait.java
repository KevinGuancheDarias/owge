/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.bind.annotation.GetMapping;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

/**
 * 
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface WithUnlockedRestServiceTrait<N extends Number, E extends EntityWithId<N>, S extends BaseBo<N, E, D>, D extends DtoFromEntity<E>> {
	public RestCrudConfigBuilder<N, E, S, D> getRestCrudConfigBuilder();

	/**
	 * Returns the object the entity <i>E</i> represents in the {@link ObjectEntity}
	 * table
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectEnum getObject();

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
		DtoUtilService dtoUtilService = beanFactory.getBean(DtoUtilService.class);
		UnlockedRelationBo unlockedRelationBo = beanFactory.getBean(UnlockedRelationBo.class);
		UserStorageBo userStorageBo = beanFactory.getBean(UserStorageBo.class);

		List<D> retVal = dtoUtilService.convertEntireArray(getRestCrudConfigBuilder().build().getDtoClass(),
				unlockedRelationBo.unboxToTargetEntity(unlockedRelationBo
						.findByUserIdAndObjectType(userStorageBo.findLoggedIn().getId(), getObject())));
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
