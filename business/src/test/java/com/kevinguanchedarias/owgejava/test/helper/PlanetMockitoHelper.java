package com.kevinguanchedarias.owgejava.test.helper;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

/**
 * This class contains methods useful to fake Planet operations
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class PlanetMockitoHelper {

	private PlanetBo planetBoMock;

	public PlanetMockitoHelper(Object target) {
		planetBoMock = Mockito.mock(PlanetBo.class);
		Whitebox.setInternalState(target, "planetBo", planetBoMock);
	}

	public void fakePlanetExists(Long id, Planet planet) {
		Mockito.when(planetBoMock.findById(id)).thenReturn(planet);
		Mockito.when(planetBoMock.exists(id)).thenReturn(true);
	}

	public void fakeExplored(UserStorage user, Planet planet) {
		Mockito.when(planetBoMock.isExplored(user, planet)).thenReturn(true);
	}

	public PlanetBo getPlanetBoMock() {
		return planetBoMock;
	}

}
