package com.kevinguanchedarias.sgtjava.test.fake;

import com.kevinguanchedarias.sgtjava.pojo.AbstractMissionReport;

public class FakeFailingMissionReport extends AbstractMissionReport {

	@Override
	public String getEventName() {
		return "shit";
	}
}
