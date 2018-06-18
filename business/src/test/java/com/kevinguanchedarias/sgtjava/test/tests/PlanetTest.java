package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.sgtjava.entity.Planet;

@RunWith(MockitoJUnitRunner.class)
public class PlanetTest {

	private Planet instance;

	@Before
	public void init() {
		instance = new Planet();
		instance.setRichness(60);
	}

	@Test
	public void shouldProperlyReturnRational() {
		assertEquals(0.6D, instance.findRationalRichness(), 0.1D);
		instance.setRichness(90);
		assertEquals(0.9D, instance.findRationalRichness(), 0.1D);
	}
}
