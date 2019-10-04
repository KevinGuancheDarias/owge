/**
 * 
 */
package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class GameJpaRepository<T, I extends Serializable> extends SimpleJpaRepository<T, I> {

	private Class<T> entityClass;

	/**
	 * @param domainClass
	 * @param em
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GameJpaRepository(Class<T> domainClass, EntityManager em) {
		super(domainClass, em);
		entityClass = domainClass;
	}

	/**
	 * @param entityInformation
	 * @param entityManager
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GameJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		entityClass = entityInformation.getJavaType();
	}

	/**
	 * Finds the class associated with the repository
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Class<T> findEntityClass() {
		return entityClass;
	}

}
