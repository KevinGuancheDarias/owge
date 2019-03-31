package com.kevinguanchedarias.owgejava.test.fake;

import com.kevinguanchedarias.owgejava.pojo.AbstractMissionReport;

public class FakeFailingMissionReport extends AbstractMissionReport {

	@Override
	public String getEventName() {
		return "shit";
	}
}
