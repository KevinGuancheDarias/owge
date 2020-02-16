package com.kevinguanchedarias.owgejava.test.mockito;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class ImprovementBoTest {
	private static final double EPSILON = 0.01;

	@Mock
	private ConfigurationBo configurationBo;

	@InjectMocks
	private ImprovementBo improvementBo;

	@Test
	public void computeImprovementValueShouldWork() {
		Configuration configuration = new Configuration();
		configuration.setValue("10");
		when(configurationBo.findOrSetDefault(Mockito.anyString(), Mockito.anyString())).thenReturn(configuration);
		assertThat(improvementBo.computeImprovementValue(5000, 20), closeTo(6050, EPSILON));
		assertThat(improvementBo.computeImprovementValue(5000, 40), closeTo(7320.5, EPSILON));
		assertThat(improvementBo.computeImprovementValue(5000, 60), closeTo(8857.805, EPSILON));
		assertThat(improvementBo.computeImprovementValue(5000, 300), closeTo(87247.011, EPSILON));
	}
}
