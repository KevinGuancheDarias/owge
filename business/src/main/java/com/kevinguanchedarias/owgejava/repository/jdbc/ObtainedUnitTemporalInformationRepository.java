package com.kevinguanchedarias.owgejava.repository.jdbc;

import com.kevinguanchedarias.owgejava.entity.jdbc.ObtainedUnitTemporalInformation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObtainedUnitTemporalInformationRepository extends CrudRepository<ObtainedUnitTemporalInformation, Long> {
    List<ObtainedUnitTemporalInformation> findByRelationId(Integer relationId);
}