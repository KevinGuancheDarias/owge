package com.kevinguanchedarias.owgejava.pojo.attack;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class AttackUserInformation {
    Double earnedPoints = 0D;
    List<AttackObtainedUnit> units = new ArrayList<>();
    List<AttackObtainedUnit> attackableUnits;

    private final UserStorage user;
    private final GroupedImprovement userImprovement;

    public AttackUserInformation(UserStorage user, GroupedImprovement userImprovement) {
        this.user = user;
        this.userImprovement = userImprovement;
    }
}
