package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;

@RunWith(MockitoJUnitRunner.class)
public class ObtainedUnitTest {

	private ObtainedUnit instance;

	@Before
	public void init() {
		instance = new ObtainedUnit();
	}

	@Test
	public void testAddCount() {
		instance.setCount(4L);
		instance.addCount(8L);
		assertEquals(12L, (long) instance.getCount());
	}

	@Test
	public void testAddCountShouldHandleNullValueAsZero() {
		instance.addCount(2L);
		assertEquals(2L, (long) instance.getCount());
	}
}
