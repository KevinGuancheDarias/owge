/**
 *
 */
package com.kevinguanchedarias.owgejava.builder;

import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.pojo.RestCrudConfig;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;

/**
 * Used to avoid having to implement multiple methods
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class RestCrudConfigBuilder<K extends Serializable, E extends EntityWithId<K>, R extends JpaRepository<E, K>, D extends DtoFromEntity<E>> {
    private final RestCrudConfig<K, E, R, D> restCrudConfig = new RestCrudConfig<>();

    /**
     * Creates an instance of the builder
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public static <K extends Serializable, E extends EntityWithId<K>, R extends JpaRepository<E, K>, D extends DtoFromEntity<E>> RestCrudConfigBuilder<K, E, R, D> create() {
        return new RestCrudConfigBuilder<>();
    }

    private RestCrudConfigBuilder() {
        // Do not use constructor, as it's less readable use, create() method
    }

    /**
     * Returns the config
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public RestCrudConfig<K, E, R, D> build() {
        return restCrudConfig;
    }

    /**
     * Sets the Entity class
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public RestCrudConfigBuilder<K, E, R, D> withEntityClass(Class<E> entityClass) {
        restCrudConfig.setEntityClass(entityClass);
        return this;
    }

    /**
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public RestCrudConfigBuilder<K, E, R, D> withRepository(R repository) {
        restCrudConfig.setRepository(repository);
        return this;
    }

    /**
     * Sets the DTO class
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public RestCrudConfigBuilder<K, E, R, D> withDtoClass(Class<D> dtoClass) {
        restCrudConfig.setDtoClass(dtoClass);
        return this;
    }

    /**
     * Sets the bean factory
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public RestCrudConfigBuilder<K, E, R, D> withBeanFactory(AutowireCapableBeanFactory beanFactory) {
        restCrudConfig.setBeanFactory(beanFactory);
        return this;
    }

    /**
     * Sets the {@link SupportedOperationsBuilder}
     *
     * @since 0.8.0
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public RestCrudConfigBuilder<K, E, R, D> withSupportedOperationsBuilder(
            SupportedOperationsBuilder supportedOperationsBuilder) {
        restCrudConfig.setSupportedOperationsBuilder(supportedOperationsBuilder);
        return this;
    }
}
