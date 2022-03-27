package com.kevinguanchedarias.owgejava.util;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class SetUtils {
    public static <E> E getFirstElement(Set<E> set) {
        if (!set.isEmpty()) {
            for (E entry : set) {
                if (entry != null) {
                    return entry;
                }
            }
        }
        return null;
    }
}
