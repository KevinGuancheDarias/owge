package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;

import java.io.Serializable;
import java.util.List;


public interface UnitTypeRepository extends WithNameRepository<UnitType, Integer>, Serializable {

    @Override
    @TaggableCacheable(
            tags = UnitType.UNIT_TYPE_CACHE_TAG
    )
    List<UnitType> findAll();

    /**
     * Exists by units type id boolean.
     *
     * @param id the id
     * @return the boolean
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    @TaggableCacheable(tags = {
            UnitType.UNIT_TYPE_CACHE_TAG + ":#id",
            Unit.UNIT_CACHE_TAG
    })
    boolean existsByUnitsTypeId(Integer id);
}