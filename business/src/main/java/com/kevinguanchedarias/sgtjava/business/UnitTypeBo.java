package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.repository.UnitTypeRepository;

@Component
public class UnitTypeBo implements WithNameBo<UnitType> {
	private static final long serialVersionUID = 1064115662505668879L;

	@Autowired
	private UnitTypeRepository unitTypeRepository;

	@Override
	public JpaRepository<UnitType, Number> getRepository() {
		return unitTypeRepository;
	}

}
