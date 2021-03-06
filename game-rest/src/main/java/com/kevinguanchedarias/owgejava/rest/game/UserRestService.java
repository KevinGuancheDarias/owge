package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.AuditBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dto.UserStorageDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("game/user")
@ApplicationScope
@AllArgsConstructor
public class UserRestService implements SyncSource {

	private final UserStorageBo userStorageBo;

	private final AuditBo auditBo;

	@GetMapping("exists")
	public boolean exists(HttpServletRequest request, HttpServletResponse response) {
		auditBo.creteCookieIfMissing(request, response);
		return userStorageBo.exists(userStorageBo.findLoggedIn().getId());
	}

	/**
	 * Will subscribe the user to this universe
	 *
	 * @return If everything well ok, returns true
	 * @author Kevin Guanche Darias
	 */
	@GetMapping("subscribe")
	public Object subscribe(@RequestParam("factionId") Integer factionId) {
		return userStorageBo.subscribe(factionId);
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create().withHandler("user_data_change", this::findData).build();
	}

	private UserStorageDto findData(UserStorage user) {
		UserStorage withDetails = userStorageBo.findById(user.getId());
		UserStorageDto retVal = userStorageBo.findData(withDetails);
		retVal.getImprovements().getUnitTypesUpgrades().forEach(current -> {
			current.getUnitType().setSpeedImpactGroup(null);
		});
		return retVal;
	}
}
