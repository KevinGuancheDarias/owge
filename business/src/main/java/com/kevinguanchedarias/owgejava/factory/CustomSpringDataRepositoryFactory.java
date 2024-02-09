/**
 *
 */
package com.kevinguanchedarias.owgejava.factory;

import java.io.Serializable;


import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import com.kevinguanchedarias.owgejava.repository.GameJpaRepository;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
public class CustomSpringDataRepositoryFactory<R extends JpaRepository<T, I>, T, I extends Serializable>
        extends JpaRepositoryFactoryBean<R, T, I> {

    /**
     * @param repositoryInterface
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
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
        /**
         * Simple jpa executor factory constructor
         *
         * @param entityManager entity manager
         */
        public SimpleJpaExecutorFactory(EntityManager entityManager) {
            super(entityManager);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        protected JpaRepositoryImplementation<?, ?> getTargetRepository(RepositoryInformation repositoryInformation,
                                                                        EntityManager em) {
            JpaEntityInformation entityInformation = getEntityInformation(repositoryInformation.getDomainType());
            return new GameJpaRepository<T, I>(entityInformation, em);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        protected Class getRepositoryBaseClass(RepositoryMetadata metadata) {
            return GameJpaRepository.class;
        }
    }
}