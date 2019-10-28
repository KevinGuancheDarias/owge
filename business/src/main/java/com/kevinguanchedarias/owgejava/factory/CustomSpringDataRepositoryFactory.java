/**
 * 
 */
package com.kevinguanchedarias.owgejava.factory;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import com.kevinguanchedarias.owgejava.repository.GameJpaRepository;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class CustomSpringDataRepositoryFactory<R extends JpaRepository<T, I>, T, I extends Serializable>
		extends JpaRepositoryFactoryBean<R, T, I> {

	/**
	 * @param repositoryInterface
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CustomSpringDataRepositoryFactory(Class<? extends R> repositoryInterface) {
		super(repositoryInterface);
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new SimpleJpaExecutorFactory<T, I>(entityManager);
	}

	/**
	 * Simple jpa executor factory
	 * 
	 * @param <T>
	 * @param <I>
	 */
	private static class SimpleJpaExecutorFactory<T, I extends Serializable> extends JpaRepositoryFactory {

		private EntityManager entityManager;

		/**
		 * Simple jpa executor factory constructor
		 * 
		 * @param entityManager entity manager
		 */
		public SimpleJpaExecutorFactory(EntityManager entityManager) {
			super(entityManager);
			this.entityManager = entityManager;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected Object getTargetRepository(RepositoryInformation repositoryInformation) {
			JpaEntityInformation entityInformation = getEntityInformation(repositoryInformation.getDomainType());
			return new GameJpaRepository<T, I>(entityInformation, entityManager);
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected Class getRepositoryBaseClass(RepositoryMetadata metadata) {
			return GameJpaRepository.class;
		}
	}
}