package com.kevinguanchedarias.sgtjava.test.helper;

import org.mockito.Mockito;

import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.repository.MissionTypeRepository;

public class MissionTypeMockitoHelper {

	private MissionTypeRepository missionTypeRepositoryMock;

	public MissionTypeMockitoHelper(MissionTypeRepository missionTypeRepositoryMock) {
		if (missionTypeRepositoryMock != null) {
			this.missionTypeRepositoryMock = missionTypeRepositoryMock;
		} else {
			this.missionTypeRepositoryMock = Mockito.mock(MissionTypeRepository.class);
		}
	}

	public com.kevinguanchedarias.sgtjava.entity.MissionType fakeFindByCode(MissionType type) {
		com.kevinguanchedarias.sgtjava.entity.MissionType fakeType = new com.kevinguanchedarias.sgtjava.entity.MissionType();
		fakeType.setCode(type.name());
		fakeType.setId(1);
		Mockito.when(missionTypeRepositoryMock.findOneByCode(type.name())).thenReturn(fakeType);
		return fakeType;
	}
}
