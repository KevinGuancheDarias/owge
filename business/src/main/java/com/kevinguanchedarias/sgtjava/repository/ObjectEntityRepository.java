package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.ObjectEntity;

public interface ObjectEntityRepository extends JpaRepository<ObjectEntity, String>, Serializable {

}
