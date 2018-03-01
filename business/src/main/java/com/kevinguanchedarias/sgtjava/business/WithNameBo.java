package com.kevinguanchedarias.sgtjava.business;

import com.kevinguanchedarias.kevinsuite.commons.entity.SimpleIdEntity;
import com.kevinguanchedarias.sgtjava.repository.WithNameRepository;

public interface WithNameBo<E extends SimpleIdEntity> extends BaseBo<E> {
	public default Object findOneByName(String name) {
		WithNameRepository<E, Number> repository = (WithNameRepository<E, Number>) getRepository();
		return repository.findOneByName(name);
	}
}
