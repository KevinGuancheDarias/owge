package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.projection.AuditDataProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditRepository extends JpaRepository<Audit, Long> {

    List<Audit> findByRelatedUser(UserStorage relatedUser);

    @Query("SELECT a FROM Audit a WHERE a.creationDate < ?1 AND a.user.id = ?2 AND (a.ipv4 IS NOT NULL OR a.ipv6 IS NOT NULL) ORDER BY a.creationDate DESC")
    List<Audit> findNearestRequestAction(LocalDateTime now, Integer user, Pageable pageable);

    List<AuditDataProjection> findDistinctByUserIdAndCreationDateBetween(Integer userId, LocalDateTime from, LocalDateTime to, Pageable pageable);


    @Query("SELECT a FROM Audit a WHERE " +
            "a.creationDate > :maxBackLookupDate " +
            "AND a.user <> :user " +
            "AND (a.cookie = :cookie OR a.ipv4 = :ipv4 OR a.ipv6 = :ipv6)")
    List<Audit> findSuspicions(LocalDateTime maxBackLookupDate, UserStorage user, String cookie, String ipv4, String ipv6);

    @Query("UPDATE Audit a SET a.isTor = ?2 WHERE a = ?1")
    @Modifying
    void updateIsTor(Audit audit, boolean isTor);

    void deleteByUserOrRelatedUser(UserStorage user, UserStorage user1);
}
