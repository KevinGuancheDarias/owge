/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

import java.io.Serializable;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import com.kevinguanchedarias.owgejava.business.BaseBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;

/**
 * Configuration for Rest CRUD traits
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class RestCrudConfig<K extends Serializable, E extends EntityWithId<K>, S extends BaseBo<K, E, D>, D extends DtoFromEntity<E>> {
	private Class<E> entityClass;
	private S boService;
	private Class<D> dtoClass;
	private AutowireCapableBeanFactory beanFactory;
	private SupportedOperationsBuilder supportedOperationsBuilder;

	/**
	 * @since 0.8.0
	 * @return the entityClass
	 */
	public Class<E> getEntityClass() {
		return entityClass;
	}

	/**
	 * @since 0.8.0
	 * @param entityClass the entityClass to set
	 */
	public void setEntityClass(Class<E> entityClass) {
		this.entityClass = entityClass;
	}

	/**
	 * @since 0.8.0
	 * @return the boService
	 */
	public S getBoService() {
		return boService;
	}

	/**
	 * @since 0.8.0
	 * @param boService the boService to set
	 */
	public void setBoService(S boService) {
		this.boService = boService;
	}

	/**
	 * @since 0.8.0
	 * @return the dtoClass
	 */
	public Class<D> getDtoClass() {
		return dtoClass;
	}

	/**
	 * @since 0.8.0
	 * @param dtoClass the dtoClass to set
	 */
	public void setDtoClass(Class<D> dtoClass) {
		this.dtoClass = dtoClass;
	}

	/**
	 * @since 0.8.0
	 * @return the beanFactory
	 */
	public AutowireCapableBeanFactory getBeanFactory() {
		return beanFactory;
	}

	/**
	 * @since 0.8.0
	 * @param beanFactory the beanFactory to set
	 */
	public void setBeanFactory(AutowireCapableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * @since 0.8.0
	 * @return the supportedOperationsBuilder
	 */
	public SupportedOperationsBuilder getSupportedOperationsBuilder() {
		return supportedOperationsBuilder;
	}

	/**
	 * @since 0.8.0
	 * @param supportedOperationsBuilder the supportedOperationsBuilder to set
	 */
	public void setSupportedOperationsBuilder(SupportedOperationsBuilder supportedOperationsBuilder) {
		this.supportedOperationsBuilder = supportedOperationsBuilder;
	}

}
