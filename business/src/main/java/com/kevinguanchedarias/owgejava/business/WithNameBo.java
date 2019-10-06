package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;
import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.repository.WithNameRepository;

public interface WithNameBo<K extends Serializable, E extends EntityWithId<K>, D extends DtoFromEntity<E>>
		extends BaseBo<K, E, D> {
	public default Object findOneByName(String name) {
		WithNameRepository<E, K> repository = (WithNameRepository<E, K>) getRepository();
		return repository.findOneByName(name);
	}
}
