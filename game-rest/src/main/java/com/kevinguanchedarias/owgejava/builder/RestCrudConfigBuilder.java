/**
 * 
 */
package com.kevinguanchedarias.owgejava.builder;

import java.io.Serializable;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.pojo.RestCrudConfig;

/**
 * Used to avoid having to implement multiple methods
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class RestCrudConfigBuilder<K extends Serializable, E extends EntityWithId<K>, S extends BaseBo<K, E, D>, D extends DtoFromEntity<E>> {
	private RestCrudConfig<K, E, S, D> restCrudConfig = new RestCrudConfig<>();

	/**
	 * Creates an instance of the builder
	 *
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static <K extends Serializable, E extends EntityWithId<K>, S extends BaseBo<K, E, D>, D extends DtoFromEntity<E>> RestCrudConfigBuilder<K, E, S, D> create() {
		return new RestCrudConfigBuilder<>();
	}

	private RestCrudConfigBuilder() {
		// Do not use constructor, as it's less readable use, create() method
	}

	/**
	 * Returns the config
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RestCrudConfig<K, E, S, D> build() {
		return restCrudConfig;
	}

	/**
	 * Sets the Entity class
	 * 
	 * @param entityClass
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RestCrudConfigBuilder<K, E, S, D> withEntityClass(Class<E> entityClass) {
		restCrudConfig.setEntityClass(entityClass);
		return this;
	}

	/**
	 * Sets the Bo service
	 * 
	 * @param boService
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RestCrudConfigBuilder<K, E, S, D> withBoService(S boService) {
		restCrudConfig.setBoService(boService);
		return this;
	}

	/**
	 * Sets the DTO class
	 * 
	 * @param dtoClass
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RestCrudConfigBuilder<K, E, S, D> withDtoClass(Class<D> dtoClass) {
		restCrudConfig.setDtoClass(dtoClass);
		return this;
	}

	/**
	 * Sets the bean factory
	 * 
	 * @param beanFactory
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RestCrudConfigBuilder<K, E, S, D> withBeanFactory(AutowireCapableBeanFactory beanFactory) {
		restCrudConfig.setBeanFactory(beanFactory);
		return this;
	}

	/**
	 * Sets the {@link SupportedOperationsBuilder}
	 * 
	 * @param supportedOperationsBuilder
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public RestCrudConfigBuilder<K, E, S, D> withSupportedOperationsBuilder(
			SupportedOperationsBuilder supportedOperationsBuilder) {
		restCrudConfig.setSupportedOperationsBuilder(supportedOperationsBuilder);
		return this;
	}
}
