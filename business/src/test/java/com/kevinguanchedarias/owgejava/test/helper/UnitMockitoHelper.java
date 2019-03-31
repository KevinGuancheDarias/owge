package com.kevinguanchedarias.owgejava.test.helper;

import java.util.ArrayList;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.UnitBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Unit;

public class UnitMockitoHelper {

	private ObtainedUnitBo obtainedUnitBoMock;
	private UnitBo unitBoMock;

	public UnitMockitoHelper(Object target) {
		obtainedUnitBoMock = Mockito.mock(ObtainedUnitBo.class);
		unitBoMock = Mockito.mock(UnitBo.class);
		Whitebox.setInternalState(target, "obtainedUnitBo", obtainedUnitBoMock);
		Whitebox.setInternalState(target, "unitBo", unitBoMock);
	}

	public void fakeFindUnit(Integer unitId, Unit retVal) {
		Mockito.when(unitBoMock.findById(unitId)).thenReturn(retVal);
	}

	public void fakeObtainedUnitExists(Long id, ObtainedUnit obtainedUnit) {
		Mockito.when(obtainedUnitBoMock.findById(id)).thenReturn(obtainedUnit);
	}

	public void fakeObtainedUnitExistsForGivenUnitAndPlanet(Integer userId, Integer unitId, Long planetId,
			ObtainedUnit retVal) {
		Mockito.when(
				obtainedUnitBoMock.findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNullOrDeployed(userId, unitId, planetId))
				.thenReturn(retVal);
	}

	public void fakeFindByMissionId(Long id, ObtainedUnit obtainedUnit) {
		List<ObtainedUnit> retVal = new ArrayList<>();
		retVal.add(obtainedUnit);
		fakeFindByMissionId(id, retVal);
	}

	public void fakeFindByMissionId(Long id, List<ObtainedUnit> retVal) {
		Mockito.when(obtainedUnitBoMock.findByMissionId(id)).thenReturn(retVal);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArgumentCaptor<List<ObtainedUnit>> captureObtainedUnitListSave() {
		ArgumentCaptor<List<ObtainedUnit>> captor = ArgumentCaptor.forClass((Class) List.class);
		Mockito.doAnswer(invocation -> {
			return invocation.getArgumentAt(0, List.class);

		}).when(obtainedUnitBoMock).save(captor.capture());
		return captor;
	}

	public ObtainedUnitBo getObtainedUnitBoMock() {
		return obtainedUnitBoMock;
	}

}
