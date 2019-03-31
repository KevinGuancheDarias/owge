package com.kevinguanchedarias.owgejava.test.helper;

import org.mockito.Mockito;

import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;

public class MissionTypeMockitoHelper {

	private MissionTypeRepository missionTypeRepositoryMock;

	public MissionTypeMockitoHelper(MissionTypeRepository missionTypeRepositoryMock) {
		if (missionTypeRepositoryMock != null) {
			this.missionTypeRepositoryMock = missionTypeRepositoryMock;
		} else {
			this.missionTypeRepositoryMock = Mockito.mock(MissionTypeRepository.class);
		}
	}

	public com.kevinguanchedarias.owgejava.entity.MissionType fakeFindByCode(MissionType type) {
		com.kevinguanchedarias.owgejava.entity.MissionType fakeType = new com.kevinguanchedarias.owgejava.entity.MissionType();
		fakeType.setCode(type.name());
		fakeType.setId(1);
		Mockito.when(missionTypeRepositoryMock.findOneByCode(type.name())).thenReturn(fakeType);
		return fakeType;
	}
}
