package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

public interface RequirementInformationRepository extends JpaRepository<RequirementInformation, Integer>, Serializable {
    List<RequirementInformation> findByRelationId(Integer relationId);

    List<RequirementInformation> findByRelationIdAndRequirementId(Integer relationId, Integer requirementId);

    /**
     * Deletes all requirement information by relation
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    void deleteByRelation(ObjectRelation relation);
}
