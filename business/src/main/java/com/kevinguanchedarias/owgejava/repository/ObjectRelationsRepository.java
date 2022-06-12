package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

public interface ObjectRelationsRepository extends JpaRepository<ObjectRelation, Integer>, Serializable {
    ObjectRelation findOneByObjectCodeAndReferenceId(String objectEntityCode, Integer refId);

    List<ObjectRelation> findByRequirementsRequirementCode(String code);

    List<ObjectRelation> findByRequirementsRequirementCodeAndRequirementsSecondValue(String code,
                                                                                     Long secondValue);

    List<ObjectRelation> findByRequirementsRequirementCodeAndRequirementsSecondValueAndRequirementsThirdValueGreaterThanEqual(
            String code, long secondValue, long thirdValue);

    List<ObjectRelation> findByObjectCodeAndRequirementsRequirementCode(String name, String name2);
}