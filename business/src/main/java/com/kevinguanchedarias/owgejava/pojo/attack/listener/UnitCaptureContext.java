package com.kevinguanchedarias.owgejava.pojo.attack.listener;

import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UnitCaptureContext {
    AttackObtainedUnit captorUnit;
    AttackObtainedUnit victimUnit;
    long capturedUnits;
    
}
