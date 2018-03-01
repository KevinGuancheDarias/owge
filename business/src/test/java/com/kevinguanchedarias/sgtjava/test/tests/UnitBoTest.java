package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.sgtjava.business.UnitBo;
import com.kevinguanchedarias.sgtjava.business.UnlockedRelationBo;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.sgtjava.repository.UnitRepository;

@RunWith(MockitoJUnitRunner.class)
public class UnitBoTest {

	@Mock
	private UnitRepository unitRepositoryMock;

	@Mock
	private UnlockedRelationBo unlockedRelationBoMock;

	@InjectMocks
	private UnitBo unitBo;

	private int id = 4;
	private Unit unit;

	private Integer primary = 20;
	private Integer secondary = 30;
	private Integer time = 40;

	@Before
	public void init() {
		unit = new Unit();
		unit.setId(id);
		unit.setPrimaryResource(primary);
		unit.setSecondaryResource(secondary);
		unit.setTime(time);
	}

	@Test
	public void shouldCallRepositoryWhenInvokedWithNumber() {
		Mockito.when(unitRepositoryMock.findOne(id)).thenReturn(unit);
		unitBo.calculateRequirements(id, 2L);
		Mockito.verify(unitRepositoryMock).findOne(id);
	}

	@Test(expected = SgtBackendInvalidInputException.class)
	public void shouldThrowWhenInputCountIsZero() {
		unitBo.calculateRequirements(unit, 0L);
	}

	@Test(expected = SgtBackendInvalidInputException.class)
	public void shouldThrowWhenInputCountIsNegative() {
		unitBo.calculateRequirements(unit, -4L);
	}

	@Test
	public void shouldReturnProperValuesWhenCountIsOne() {
		ResourceRequirementsPojo resourceRequirements = unitBo.calculateRequirements(unit, 1L);
		assertEquals(primary, resourceRequirements.getRequiredPrimary(), 0.1);
		assertEquals(secondary, resourceRequirements.getRequiredSecondary(), 0.1);
		assertEquals(time, resourceRequirements.getRequiredTime(), 0.1);
	}

	@Test
	public void shouldReturnProperValuesWhenCountIsTwo() {
		ResourceRequirementsPojo resourceRequirements = unitBo.calculateRequirements(unit, 2L);
		assertEquals(primary * 2, resourceRequirements.getRequiredPrimary(), 0.1);
		assertEquals(secondary * 2, resourceRequirements.getRequiredSecondary(), 0.1);
		assertEquals(time * 2, resourceRequirements.getRequiredTime(), 0.1);
	}
}
