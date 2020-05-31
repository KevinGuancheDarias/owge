package com.kevinguanchedarias.owgejava.responses;

import java.util.List;

import com.kevinguanchedarias.owgejava.dto.MissionReportDto;

/**
 * Represents the response to fetch the page
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class MissionReportResponse {
	private int page;
	private long userUnread;
	private long enemyUnread;
	private List<MissionReportDto> reports;

	/**
	 * @return the page
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public int getPage() {
		return page;
	}

	/**
	 * @param page the page to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setPage(int page) {
		this.page = page;
	}

	/**
	 * @return the userUnread
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public long getUserUnread() {
		return userUnread;
	}

	/**
	 * @param userUnread the userUnread to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUserUnread(long userUnread) {
		this.userUnread = userUnread;
	}

	/**
	 * @return the enemyUnread
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public long getEnemyUnread() {
		return enemyUnread;
	}

	/**
	 * @param enemyUnread the enemyUnread to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEnemyUnread(long enemyUnread) {
		this.enemyUnread = enemyUnread;
	}

	/**
	 * @return the reports
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<MissionReportDto> getReports() {
		return reports;
	}

	/**
	 * @param reports the reports to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setReports(List<MissionReportDto> reports) {
		this.reports = reports;
	}

}
