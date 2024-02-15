package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface MissionReportRepository extends JpaRepository<MissionReport, Long>, Serializable {

    List<MissionReport> findByUserIdOrderByIdDesc(Integer userId, Pageable pageRequest);

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    Long countByUserIdAndIsEnemyAndUserReadDateIsNull(Integer userId, Boolean isEnemy);

    /**
     * Marks the messages as read if the user is the owner
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Query("UPDATE MissionReport rp SET rp.userReadDate = CURRENT_TIMESTAMP WHERE  rp.user.id = :userId AND rp.id IN :reportsIds")
    @Modifying
    void markAsReadIfUserIsOwner(List<Long> reportsIds, Integer userId);

    @Query("UPDATE MissionReport  rp SET rp.userReadDate = CURRENT_TIMESTAMP WHERE rp.user.id = :userId AND rp.userReadDate is NULL AND rp.reportDate < :date")
    @Modifying
    void markAsReadBeforeDate(Integer userId, Date date);

    List<MissionReport> findByReportDateLessThan(Date date, Pageable pageable);

    void deleteByUser(UserStorage user);
}
