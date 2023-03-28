package com.kevinguanchedarias.owgejava.pojo.attack;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Planet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
public class AttackInformation {
    private final Mission attackMission;
    private final Map<Integer, AttackUserInformation> users = new HashMap<>();
    private final List<AttackObtainedUnit> units = new ArrayList<>();
    private final Set<Integer> usersWithDeletedMissions = new HashSet<>();
    private final Set<Integer> usersWithChangedCounts = new HashSet<>();
    private final Set<Long> unitsStoringUnits = new HashSet<>();
    private final Planet targetPlanet;
    private final Map<String, List<Object>> contextData = new HashMap<>();

    private boolean isRemoved;
    private UnitMissionReportBuilder reportBuilder;

    public AttackInformation(Mission attackMission, Planet targetPlanet) {
        this.attackMission = attackMission;
        this.targetPlanet = targetPlanet;
    }

    public <T> List<T> getContextData(String key, Class<T> clazz) {
        contextData.computeIfAbsent(key, k -> new ArrayList<>());
        return doCast(contextData.get(key), clazz);
    }

    public void addToContext(String key, Object object) {
        contextData.computeIfAbsent(key, k -> new ArrayList<>());
        contextData.get(key).add(object);
    }

    private <T> List<T> doCast(List<Object> val, Class<T> clazz) {
        return val.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .toList();
    }
}
