package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.ImprovementUnitTypeBo;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.ImprovementUnitType;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;

@ActiveProfiles("dev")
@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class ImprovementsTest {

	private static final Integer EXISTING_UNIT_TYPE_ID = 74;
	private static final Integer NOT_EXISTING_UNIT_TYPE_ID = 28;

	private List<ImprovementUnitType> testList;

	@Mock
	private UnitTypeRepository unitTypeRepository;

	@InjectMocks
	private ImprovementUnitTypeBo improvementsBo;

	@PostConstruct
	public void init() {
		MockitoAnnotations.initMocks(this);

		Mockito.when(unitTypeRepository.exists(EXISTING_UNIT_TYPE_ID)).thenReturn(true);
		Mockito.when(unitTypeRepository.exists(NOT_EXISTING_UNIT_TYPE_ID)).thenReturn(false);
	}

	@Test
	public void shouldRemoveNotExisting() {
		testList = new ArrayList<>();

		ImprovementUnitType existingOne = prepareValid(EXISTING_UNIT_TYPE_ID);
		ImprovementUnitType notExistingOne = prepareValid(NOT_EXISTING_UNIT_TYPE_ID);

		testList.add(existingOne);
		testList.add(existingOne);

		testList.add(notExistingOne);
		testList.add(notExistingOne);
		testList.add(notExistingOne);

		improvementsBo.removeInvalidFromList(testList);
		assertEquals(2, testList.size());
	}

	@Test
	public void shouldRemoveInvalidValue() {
		testList = new ArrayList<>();
		testList.add(prepareValid());
		testList.add(prepareValid());

		ImprovementUnitType nullValue = prepareValid();
		nullValue.setValue(null);

		testList.add(nullValue);

		ImprovementUnitType zeroValue = prepareValid();
		zeroValue.setValue(0L);

		testList.add(zeroValue);

		improvementsBo.removeInvalidFromList(testList);
		assertEquals(2, testList.size());
	}

	@Test
	public void shouldRemoveInvalidType() {
		testList = new ArrayList<>();
		testList.add(prepareValid());
		testList.add(prepareValid());

		ImprovementUnitType nullType = prepareValid();
		nullType.setType(null);

		testList.add(nullType);

		ImprovementUnitType emptyValue = prepareValid();
		emptyValue.setType("");

		testList.add(emptyValue);

		ImprovementUnitType invalidType = prepareValid();
		invalidType.setType("DIFJDIOJFJISDJKJIKD3423!kdFJ");

		testList.add(invalidType);

		improvementsBo.removeInvalidFromList(testList);
		assertEquals(2, testList.size());
	}

	@Test
	public void shouldReturnDuplicated() {
		testList = new ArrayList<>();
		testList.add(prepareValid());

		assertTrue(improvementsBo.isDuplicated(testList, prepareValid()));
	}

	@Test
	public void shoulReturnDuplicatedEvenWithDifferentValues() {
		testList = new ArrayList<>();
		testList.add(prepareValid());

		ImprovementUnitType compared = prepareValid();
		compared.setValue(927L);

		assertTrue(improvementsBo.isDuplicated(testList, compared));
	}

	@Test
	public void shouldReturnNotDuplicatedBecauseTypeIsDifferent() {
		testList = new ArrayList<>();
		testList.add(prepareValid());

		ImprovementUnitType compared = prepareValid();
		compared.setType("DEFENSE");

		assertFalse(improvementsBo.isDuplicated(testList, compared));
	}

	@Test
	public void shouldReturnNotDuplicatedBecauseUnitTypeIsDifferent() {
		testList = new ArrayList<>();
		testList.add(prepareValid());

		ImprovementUnitType compared = prepareValid();
		compared.setUnitType(new UnitType());
		compared.getUnitType().setId(NOT_EXISTING_UNIT_TYPE_ID);
		;

		assertFalse(improvementsBo.isDuplicated(testList, compared));
	}

	@Test
	public void shouldProperlyHandleRationalFinders() {
		Improvement instance = new Improvement();
		instance.setMoreChargeCapacity(80F);
		assertEquals(0.8D, instance.findRationalChargeCapacity(), 0.1D);
	}

	private ImprovementUnitType prepareValid(Integer id) {
		ImprovementUnitType retVal = new ImprovementUnitType();
		retVal.setType("ATTACK");
		retVal.setValue(4L);
		retVal.setUnitType(new UnitType());
		retVal.getUnitType().setId(id);

		return retVal;
	}

	private ImprovementUnitType prepareValid() {
		return prepareValid(EXISTING_UNIT_TYPE_ID);
	}
}
