package com.kevinguanchedarias.owgejava.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.kevinguanchedarias.owgejava.pojo.MysqlEngineInformation;

/**
 *
 * @since 0.9.18
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Repository
public class MysqlInformationRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 *
	 * @return
	 * @since 0.9.18
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MysqlEngineInformation findInnoDbStatus() {
		return jdbcTemplate.queryForObject("SHOW ENGINE INNODB STATUS",
				(rs, rowNum) -> new MysqlEngineInformation(rs.getString("Type"), rs.getString("Status")));
	}
}
