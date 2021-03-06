package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.projection.AuditDataProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    List<Audit> findByRelatedUser(UserStorage relatedUser);

    List<Audit> findByUser(UserStorage user);

    @Query("SELECT a FROM Audit a WHERE a.creationDate < ?1 AND a.user.id = ?2 AND a.ip IS NOT NULL ORDER BY a.creationDate DESC")
    List<Audit> findNearesRequestAction(LocalDateTime now, Integer user, Pageable pageable);

    List<AuditDataProjection> findDistinctByUserIdAndCreationDateBetween(Integer userId, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
