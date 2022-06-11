package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

public interface UnlockedRelationRepository extends JpaRepository<UnlockedRelation, Long>, Serializable {

    UnlockedRelation findOneByUserIdAndRelationId(Integer userId, Integer relationId);

    List<UnlockedRelation> findByUserIdAndRelationObjectCode(Integer userId, String objectType);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    void deleteByRelation(ObjectRelation objectRelation);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    boolean existsByUserAndRelation(UserStorage user, ObjectRelation relation);
}
