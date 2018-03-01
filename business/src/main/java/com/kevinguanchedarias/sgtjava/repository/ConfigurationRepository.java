package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.Configuration;

public interface ConfigurationRepository extends JpaRepository<Configuration, String>, Serializable {
	public List<Configuration> findByNameIn(List<String> names);

	public List<Configuration> findByPrivilegedFalse();
}
