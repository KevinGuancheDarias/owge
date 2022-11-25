package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.AsyncRunnerBo;
import com.kevinguanchedarias.owgejava.business.SocketIoService;
import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ObtainedUnitEventEmitter {
    public static final String UNIT_OBTAINED_CHANGE = "unit_obtained_change";

    private final TransactionUtilService transactionUtilService;
    private final SocketIoService socketIoService;
    private final ObtainedUnitFinderBo obtainedUnitFinderBo;
    private final AsyncRunnerBo asyncRunnerBo;
    private final UnitTypeBo unitTypeBo;
    private final UserEventEmitterBo userEventEmitterBo;

    public void emitObtainedUnitsAfterCommit(UserStorage user) {
        transactionUtilService.doAfterCommit(() -> emitObtainedUnits(user));
    }

    public void emitObtainedUnits(UserStorage user) {
        socketIoService.sendMessage(user, UNIT_OBTAINED_CHANGE,
                () -> obtainedUnitFinderBo.findCompletedAsDto(user));
    }

    /**
     * Emits changes to elements affected by a unit alteration, for example, energy, or type's count
     */
    public void emitSideChanges(List<ObtainedUnit> obtainedUnits) {
        if (!obtainedUnits.isEmpty()) {
            UserStorage user = obtainedUnits.get(0).getUser();
            Integer userId = user.getId();

            if (obtainedUnits.stream().anyMatch(unit -> unit.getUnit().getEnergy() > 0)) {
                asyncRunnerBo.runAssyncWithoutContextDelayed(() -> userEventEmitterBo.emitUserData(user));
            }
            asyncRunnerBo.runAssyncWithoutContextDelayed(() ->
                    socketIoService.sendMessage(userId, "unit_type_change", () -> unitTypeBo.findUnitTypesWithUserInfo(userId))
            );
            emitObtainedUnits(user);
        }
    }
}
