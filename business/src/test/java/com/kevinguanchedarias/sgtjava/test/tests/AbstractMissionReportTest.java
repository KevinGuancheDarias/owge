package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.kevinguanchedarias.sgtjava.exception.ProgrammingException;
import com.kevinguanchedarias.sgtjava.test.fake.FakeFailingMissionReport;
import com.kevinguanchedarias.sgtjava.test.fake.FakeWorkingMissionReport;

@RunWith(BlockJUnit4ClassRunner.class)
public class AbstractMissionReportTest {

	@Test(expected = ProgrammingException.class)
	public void shouldThrowWhenInvalidEventWasSpecified() {
		new FakeFailingMissionReport();
	}

	@Test
	public void shouldProperlyConstructWhenEventIsValid() {
		assertNotNull(new FakeWorkingMissionReport());
	}
}
