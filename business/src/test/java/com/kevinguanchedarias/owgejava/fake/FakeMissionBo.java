package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.AbstractMissionBo;
import org.springframework.stereotype.Service;

@Service
public class FakeMissionBo extends AbstractMissionBo {
    @Override
    public String getGroupName() {
        return "FAKE";
    }
}
