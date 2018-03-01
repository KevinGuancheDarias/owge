package com.kevinguanchedarias.sgtjava.business;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.dao.RequirementInformationDao;
import com.kevinguanchedarias.sgtjava.entity.ObjectEntity;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendRequirementException;
import com.kevinguanchedarias.sgtjava.repository.ObjectEntityRepository;
import com.kevinguanchedarias.sgtjava.repository.WithNameRepository;

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
	public WithNameRepository findRepository(ObjectEntity object) {
		Class entityRepositoryClass;
		WithNameRepository repository;
		try {
			entityRepositoryClass = Class.forName(object.getRepository());
			repository = (WithNameRepository) beanFactory.getBean(entityRepositoryClass);
		} catch (ClassNotFoundException e) {
			LOGGER.fatal(e);
			throw new SgtBackendRequirementException("No existe el repositorio " + object.getRepository());
		}
		return repository;
	}

	public List<ObjectEntity> getAll() {
		return objectEntityRepository.findAll();
	}

	public ObjectEntity getByDescription(RequirementTargetObject target) {
		return objectEntityRepository.findOne(target.name());
	}

}
