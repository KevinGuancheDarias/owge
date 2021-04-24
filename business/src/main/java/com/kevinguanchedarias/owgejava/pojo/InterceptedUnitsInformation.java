package com.kevinguanchedarias.owgejava.pojo;

import java.util.Set;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class InterceptedUnitsInformation {
	UserStorage interceptorUser;
	ObtainedUnit interceptorUnit;
	Set<ObtainedUnit> interceptedUnits;
}
