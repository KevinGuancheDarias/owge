package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendRequirementException;
import com.kevinguanchedarias.owgejava.repository.ObjectEntityRepository;
import com.kevinguanchedarias.owgejava.util.ProxyUtil;

@Service
public class ObjectEntityBo implements Serializable {
	private static final long serialVersionUID = -8249042125676687286L;

	private static final Logger LOGGER = Logger.getLogger(RequirementInformationDao.class);

	@Autowired
	private ObjectEntityRepository objectEntityRepository;

	@Autowired
	private transient AutowireCapableBeanFactory beanFactory;

	/**
	 * gets the repository for given {@link ObjectEntity}
	 *
	 * @param object
	 * @return
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JpaRepository findRepository(ObjectEntity object) {
		Class entityRepositoryClass;
		JpaRepository repository;
		try {
			entityRepositoryClass = Class.forName(object.getRepository());
			repository = (JpaRepository) beanFactory.getBean(entityRepositoryClass);
		} catch (ClassNotFoundException e) {
			LOGGER.fatal(e);
			throw new SgtBackendRequirementException("No existe el repositorio " + object.getRepository());
		}
		return repository;
	}

	/**
	 * Can be used to find the BO associated with a repository
	 *
	 * @param <E>
	 * @param object
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@SuppressWarnings("unchecked")
	public <K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>> WithNameBo<K, E, D> findBo(
			ObjectEntity object) {
		String repositoryName = ProxyUtil.resolveProxy(findRepository(object)).getName();
		String[] repositoryNameParts = repositoryName.split("\\.");
		repositoryNameParts[3] = "business";
		repositoryNameParts[4] = repositoryNameParts[4].replaceFirst("Repository", "Bo");
		String boName = String.join(".", repositoryNameParts);
		try {
			Class<? extends WithNameBo<K, E, D>> clazz = (Class<? extends WithNameBo<K, E, D>>) Class.forName(boName);
			return beanFactory.getBean(clazz);
		} catch (ClassNotFoundException e) {
			throw new ProgrammingException(
					"While the repository " + repositoryName + " exists, there is no a Bo with name " + boName
							+ " which means you are not following the package or class name convention");
		}
	}

	public List<ObjectEntity> getAll() {
		return objectEntityRepository.findAll();
	}

	/**
	 *
	 * @param objectEnum
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ObjectEntity findByDescription(ObjectEnum objectEnum) {
		return objectEntityRepository.findById(objectEnum.name()).get();
	}

	/**
	 *
	 * @throws ProgrammingException If doesn't exists in the table, but it exists in
	 *                              the enum
	 * @param target
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void existsByDescriptionOrDie(ObjectEnum target) {
		if (!objectEntityRepository.existsById(target.name())) {
			throw new ProgrammingException("You are requesting Object of type " + target.name()
					+ " but, while the object exists in the enum, it doesn't exists in the objects table, insert it into the table, or remove from the ENUM and the used location");
		}
	}

	/**
	 *
	 * @deprecated Use {@link ObjectEntityBo#findByDescription(ObjectEnum)} instead
	 * @param target
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Deprecated(since = "0.8.0")
	public ObjectEntity getByDescription(RequirementTargetObject target) {
		return objectEntityRepository.findById(target.name()).get();
	}

	/**
	 * Checks if the object is valid
	 *
	 * @param object
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void checkValid(ObjectEntity object) {
		if (object == null) {
			throw new SgtBackendRequirementException("No se ha especificado un tipo de objeto");
		}

		if (object.getRepository() == null) {
			throw new SgtBackendRequirementException("No hay ninguna entidad asociada");
		}

		if (!objectEntityRepository.existsById(object.getCode())) {
			throw new SgtBackendRequirementException("La entidad objeto " + object.getCode() + " no existe");
		}
	}
}
