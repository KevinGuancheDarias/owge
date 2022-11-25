package com.kevinguanchedarias.owgejava.repository.jdbc;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.jdbc.StoredUnit;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StoredUnitRepository extends CrudRepository<StoredUnit, Long> {
    @Query("SELECT SUM(u.storedWeight * ou.count) FROM stored_unit su " +
            "INNER JOIN obtained_unit ou ON ou.id = su.target_obtained_unit_id " +
            "INNER JOIN unit u ON u.id = ou.unit_id " +
            "WHERE su.owner_obtained_unit_id = ?1")
    long findUsedWeight(long obtainedUnitId);

    List<StoredUnit> findByOwnerObtainedUnitId(ObtainedUnit obtainedUnit);
}
