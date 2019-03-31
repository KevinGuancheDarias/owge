package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.business.ObjectEntityBo;
import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;

@RunWith(MockitoJUnitRunner.class)
public class ObjectRelationBoTest {

	@Mock
	private ObjectEntityBo objectEntityBoMock;

	@Mock
	private ObjectRelationsRepository objectRelationsRepositoryMock;

	@Mock
	private UnitRepository unitRepositoryMock;

	@InjectMocks
	private ObjectRelationBo objectRelationBo;

	@Before
	public void init() {
		Mockito.when(objectEntityBoMock.findRepository(Mockito.any())).thenReturn(unitRepositoryMock);
	}

	@Test
	public void shouldProperlyUnboxSingleEntity() {
		int refId = 10;
		ObjectRelation relation = new ObjectRelation();
		relation.setReferenceId(refId);
		relation.setObject(new ObjectEntity());

		Unit unit = new Unit();
		unit.setId(refId);

		Mockito.when(unitRepositoryMock.findOne(refId)).thenReturn(unit);
		assertEquals(unit, objectRelationBo.unboxObjectRelation(relation));

	}

	@Test
	public void shouldProperlyUnboxMultipleEntities() {
		int refId1 = 10;
		int refId2 = 11;
		List<ObjectRelation> relations = new ArrayList<>();

		ObjectRelation relation1 = new ObjectRelation();
		relation1.setReferenceId(refId1);
		relation1.setObject(new ObjectEntity());

		ObjectRelation relation2 = new ObjectRelation();
		relation2.setReferenceId(refId2);
		relation2.setObject(new ObjectEntity());

		relations.add(relation1);
		relations.add(relation2);

		Unit unit1 = new Unit();
		unit1.setId(refId1);
		Unit unit2 = new Unit();
		unit2.setId(refId2);

		Mockito.when(unitRepositoryMock.findOne(refId1)).thenReturn(unit1);
		Mockito.when(unitRepositoryMock.findOne(refId2)).thenReturn(unit2);

		List<Unit> results = objectRelationBo.unboxObjectRelation(relations);
		assertFalse(results.isEmpty());
		assertEquals(2, results.size());
		assertEquals(unit1, results.get(0));
		assertEquals(unit2, results.get(1));

	}
}
