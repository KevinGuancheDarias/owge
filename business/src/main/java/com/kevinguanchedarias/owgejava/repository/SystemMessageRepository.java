package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.SystemMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 */
public interface SystemMessageRepository extends JpaRepository<SystemMessage, Integer> {

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.16
     */
    List<SystemMessage> findByCreationDateLessThan(LocalDateTime date);

}
