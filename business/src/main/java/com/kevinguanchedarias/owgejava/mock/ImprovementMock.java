package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Improvement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ImprovementMock {
    public static Improvement givenEntity() {
        Improvement improvement = new Improvement();
        improvement.setId(1);
        return improvement;
    }
}
