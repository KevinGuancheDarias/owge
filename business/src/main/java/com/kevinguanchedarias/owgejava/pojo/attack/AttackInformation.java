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

    /**
     * Seeded {@link Random} used only when the {@code ATTACK_DETERMINISTIC_RNG} configuration flag is on.
     * When {@code null}, the engine falls back to its default (time-random) behaviour and emits no RNG trace.
     */
    private Random deterministicRandom;

    /**
     * Seed used to build {@link #deterministicRandom} (the attack mission id). Emitted on every trace line.
     */
    private long rngSeed;

    /**
     * Per-attack RNG trace sequence counter; starts at 0 and increments once per primitive draw.
     */
    private long rngSeq;

    public AttackInformation(Mission attackMission, Planet targetPlanet) {
        this.attackMission = attackMission;
        this.targetPlanet = targetPlanet;
    }

    /**
     * @return {@code true} when deterministic RNG mode is active (a seeded {@link Random} has been set).
     */
    public boolean isDeterministicRng() {
        return deterministicRandom != null;
    }

    /**
     * Emits one {@code @@RNG@@} trace line to stderr (deterministic mode only) and advances the sequence counter.
     * The JSON is compact (no internal spaces) with fixed field names so both backends can be diffed by {@code seq}.
     */
    public void traceRng(String site, Integer bound, Long attacker, Long victim, Long killed, Number result) {
        var sb = new StringBuilder();
        sb.append("{\"seq\":").append(rngSeq)
                .append(",\"site\":\"").append(site).append('"')
                .append(",\"seed\":").append(rngSeed)
                .append(",\"bound\":").append(bound == null ? "null" : bound.toString())
                .append(",\"attacker\":").append(attacker == null ? "null" : attacker.toString())
                .append(",\"victim\":").append(victim == null ? "null" : victim.toString())
                .append(",\"killed\":").append(killed == null ? "null" : killed.toString())
                .append(",\"result\":").append(result)
                .append('}');
        System.err.println("@@RNG@@ " + sb);
        rngSeq++;
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
