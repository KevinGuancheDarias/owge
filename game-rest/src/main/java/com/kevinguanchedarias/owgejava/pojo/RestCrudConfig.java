/**
 *
 */
package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import lombok.Data;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;

/**
 * Configuration for Rest CRUD traits
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
public class RestCrudConfig<K extends Serializable, E extends EntityWithId<K>, R extends JpaRepository<E, K>, D extends DtoFromEntity<E>> {
    private Class<E> entityClass;
    private R repository;
    private Class<D> dtoClass;
    private AutowireCapableBeanFactory beanFactory;
    private SupportedOperationsBuilder supportedOperationsBuilder;

}
