package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

public interface ConfigurationRepository extends JpaRepository<Configuration, String>, Serializable {
    List<Configuration> findByNameIn(List<String> names);

    List<Configuration> findByPrivilegedFalse();
}
