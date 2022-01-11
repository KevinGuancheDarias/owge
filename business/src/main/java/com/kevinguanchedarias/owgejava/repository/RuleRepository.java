package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleRepository extends JpaRepository<Rule, Integer> {

    List<Rule> findByType(String type);

    List<Rule> findByOriginTypeAndOriginId(String sourceType, long id);
}
