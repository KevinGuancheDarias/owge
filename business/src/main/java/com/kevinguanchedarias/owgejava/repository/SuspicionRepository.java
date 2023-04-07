package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.Audit;
import com.kevinguanchedarias.owgejava.entity.Suspicion;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuspicionRepository extends JpaRepository<Suspicion, Long> {
    boolean existsByRelatedUserAndRelatedAudit(UserStorage user, Audit audit);

    List<Suspicion> findByRelatedUser(UserStorage user);

    long countByRelatedUser(UserStorage user);
}
