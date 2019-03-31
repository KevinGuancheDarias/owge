package com.kevinguanchedarias.owgejava.repository;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.owgejava.entity.MissionReport;

public interface MissionReportRepository extends JpaRepository<MissionReport, Number>, Serializable {

	List<MissionReport> findByUserIdOrderByIdDesc(Integer userId, Pageable pageRequest);

}
