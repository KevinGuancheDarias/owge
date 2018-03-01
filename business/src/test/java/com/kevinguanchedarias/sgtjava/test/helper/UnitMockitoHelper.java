package com.kevinguanchedarias.sgtjava.test.helper;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.kevinguanchedarias.sgtjava.business.ObtainedUnitBo;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;

public class UnitMockitoHelper {

	private ObtainedUnitBo obtainedUnitBoMock;

	public UnitMockitoHelper(Object target) {
		obtainedUnitBoMock = Mockito.mock(ObtainedUnitBo.class);
		Whitebox.setInternalState(target, "obtainedUnitBo", obtainedUnitBoMock);
	}

	public void fakeObtainedUnitExists(Long id, ObtainedUnit obtainedUnit) {
		Mockito.when(obtainedUnitBoMock.findById(id)).thenReturn(obtainedUnit);
	}

	public ObtainedUnitBo getObtainedUnitBoMock() {
		return obtainedUnitBoMock;
	}

}
