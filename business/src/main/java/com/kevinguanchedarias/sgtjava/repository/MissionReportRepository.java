package com.kevinguanchedarias.sgtjava.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kevinguanchedarias.sgtjava.entity.MissionReport;

public interface MissionReportRepository extends JpaRepository<MissionReport, Number>, Serializable {

}
