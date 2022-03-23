package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RuleRepository extends JpaRepository<Rule, Integer> {

    List<Rule> findByType(String type);

    List<Rule> findByOriginTypeAndOriginId(String sourceType, long id);

    boolean existsByOriginTypeAndOriginIdAndDestinationTypeIn(
            String originType, Long originId, List<String> destinationType
    );

    Optional<Rule> findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
            String type, String originType, Long originId, String destinationType, Long destinationId
    );
}
