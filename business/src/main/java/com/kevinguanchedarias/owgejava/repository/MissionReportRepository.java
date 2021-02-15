package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.kevinguanchedarias.owgejava.entity.MissionReport;

public interface MissionReportRepository extends JpaRepository<MissionReport, Long>, Serializable {

	List<MissionReport> findByUserIdOrderByIdDesc(Integer userId, Pageable pageRequest);

	/**
	 *
	 * @param userId
	 * @param isEnemy
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	Long countByUserIdAndIsEnemyAndUserReadDateIsNull(Integer userId, Boolean isEnemy);

	/**
	 * Marks the messages as read if the user is the owner
	 *
	 * @param reportsIds
	 * @param userId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Query("UPDATE MissionReport rp SET rp.userReadDate = CURRENT_DATE WHERE  rp.user.id = :userId AND rp.id IN :reportsIds")
	@Modifying
	void markAsReadIfUserIsOwner(List<Long> reportsIds, Integer userId);

	List<MissionReport> findByReportDateLessThan(Date date, Pageable pageable);
}
