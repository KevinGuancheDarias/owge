package com.kevinguanchedarias.owgejava.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.Translatable;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface TranslatableRepository extends JpaRepository<Translatable, Long> {

}
