package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface WithNameRepository<E,I extends Serializable> extends JpaRepository<E,I> {
	public E findOneByName(String name);
}
