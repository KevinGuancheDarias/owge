package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class InterceptedUnitsInformation {
    UserStorage interceptorUser;
    ObtainedUnit interceptorUnit;
    Set<ObtainedUnit> interceptedUnits;
}
