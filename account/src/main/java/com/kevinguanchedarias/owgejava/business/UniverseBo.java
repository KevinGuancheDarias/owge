package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Universe;
import com.kevinguanchedarias.owgejava.repository.UniverseRepository;

@Service
public class UniverseBo implements BaseBo<Universe> {
	private static final long serialVersionUID = -582298044994856203L;

	@Autowired
	private UniverseRepository universeRepository;

	@Override
	public JpaRepository<Universe, Number> getRepository() {
		return universeRepository;
	}

	public List<Universe> findOfficialUniverses() {
		return universeRepository.findByOfficial(true);
	}
}
