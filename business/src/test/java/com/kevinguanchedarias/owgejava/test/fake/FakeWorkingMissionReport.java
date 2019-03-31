package com.kevinguanchedarias.owgejava.test.fake;

import com.kevinguanchedarias.owgejava.pojo.AbstractMissionReport;

public class FakeWorkingMissionReport extends AbstractMissionReport {

	@Override
	public String getEventName() {
		return "mission_explore";
	}

}
