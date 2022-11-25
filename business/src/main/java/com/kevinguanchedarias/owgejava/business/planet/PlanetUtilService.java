package com.kevinguanchedarias.owgejava.business.planet;

import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import org.springframework.stereotype.Service;

@Service
public class PlanetUtilService {

    public boolean isEnemyPlanet(UserStorage invokerUser, Planet askedPlanet) {
        return askedPlanet.getOwner() != null && !invokerUser.equals(askedPlanet.getOwner());
    }
}
