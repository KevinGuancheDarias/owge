package com.kevinguanchedarias.sgtjava.test.helper;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.kevinguanchedarias.sgtjava.business.MissionReportBo;
import com.kevinguanchedarias.sgtjava.entity.MissionReport;

public class MissionReportMockitoHelper {

	private MissionReportBo missionReportBoMock;

	public MissionReportMockitoHelper() {
		this(null);
	}

	public MissionReportMockitoHelper(MissionReportBo existingMock) {
		if (existingMock == null) {
			missionReportBoMock = Mockito.mock(MissionReportBo.class);
		} else {
			missionReportBoMock = existingMock;
		}
	}

	public ArgumentCaptor<MissionReport> captureMissionReportSave() {
		ArgumentCaptor<MissionReport> captor = ArgumentCaptor.forClass(MissionReport.class);
		Mockito.doAnswer(invocation -> {
			return invocation.getArgumentAt(0, MissionReport.class);

		}).when(missionReportBoMock).save(captor.capture());
		return captor;
	}

}
