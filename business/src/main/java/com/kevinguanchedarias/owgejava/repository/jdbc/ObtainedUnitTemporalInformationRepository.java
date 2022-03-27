package com.kevinguanchedarias.owgejava.repository.jdbc;

import com.kevinguanchedarias.owgejava.entity.jdbc.ObtainedUnitTemporalInformation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ObtainedUnitTemporalInformationRepository extends CrudRepository<ObtainedUnitTemporalInformation, Long> {
    long countById(long id);
}