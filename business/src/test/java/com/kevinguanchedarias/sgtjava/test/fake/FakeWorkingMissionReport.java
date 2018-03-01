package com.kevinguanchedarias.sgtjava.test.fake;

import com.kevinguanchedarias.sgtjava.pojo.AbstractMissionReport;

public class FakeWorkingMissionReport extends AbstractMissionReport {

	@Override
	public String getEventName() {
		return "mission_explore";
	}

}
