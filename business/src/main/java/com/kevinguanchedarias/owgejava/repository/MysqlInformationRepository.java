package com.kevinguanchedarias.owgejava.repository;

import com.kevinguanchedarias.owgejava.pojo.MysqlEngineInformation;
import com.kevinguanchedarias.owgejava.pojo.MysqlProcessInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.18
 */
@Repository
public class MysqlInformationRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Find inno db status mysql engine information.
	 *
	 * @return the mysql engine information
	 * @since 0.9.18
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public MysqlEngineInformation findInnoDbStatus() {
		return jdbcTemplate.queryForObject("SHOW ENGINE INNODB STATUS",
				(rs, rowNum) -> new MysqlEngineInformation(rs.getString("Type"), rs.getString("Status")));
	}

	/**
	 * Find full process information list.
	 *
	 * @return the list
	 * @since 0.9.20
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<MysqlProcessInformation> findFullProcessInformation() {
		// @formatter:off
		return jdbcTemplate.query("SHOW FULL PROCESSLIST", (rs, rowNum) ->
				new MysqlProcessInformation(
						rs.getLong("Id"),
						rs.getString("User"),
						rs.getString("Host"),
						rs.getString("db"),
						rs.getString("Command"),
						rs.getLong("Time"),
						rs.getString("State"),
						rs.getString("Info")
				)
		);
		// @formatter:on
	}
}
