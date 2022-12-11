package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImprovementMock {
    public static final float GROUPED_IMPROVEMENT_MORE_MISSIONS = 3;

    public static Improvement givenImprovement() {
        Improvement improvement = new Improvement();
        improvement.setId(1);
        return improvement;
    }

    public static GroupedImprovement givenUserImprovement() {
        var instance = new GroupedImprovement();
        instance.setMoreMissions(GROUPED_IMPROVEMENT_MORE_MISSIONS);
        return instance;
    }
}
