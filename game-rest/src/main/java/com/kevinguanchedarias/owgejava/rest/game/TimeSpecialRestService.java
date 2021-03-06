/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.game;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.business.TimeSpecialBo;
import com.kevinguanchedarias.owgejava.dto.ActiveTimeSpecialDto;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;
import com.kevinguanchedarias.owgejava.rest.trait.WithUnlockedRestServiceTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@RestController
@ApplicationScope
@RequestMapping("game/time_special")
public class TimeSpecialRestService
		implements WithUnlockedRestServiceTrait<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto>,
		WithReadRestServiceTrait<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto>, SyncSource {

	@Autowired
	private TimeSpecialBo timeSpecialBo;

	@Autowired
	private ActiveTimeSpecialBo activeTimeSpecialBo;

	@Autowired
	private AutowireCapableBeanFactory beanFactory;

	@PostMapping("/activate")
	public ActiveTimeSpecialDto activate(@RequestBody Integer timeSpecialId) {
		return activeTimeSpecialBo.toDto(activeTimeSpecialBo.activate(timeSpecialId));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.kevinguanchedarias.owgejava.rest.trait.CrudRestServiceNoOpEventsTrait#
	 * beforeRequestEnd(java.lang.Object, java.lang.Object)
	 */
	@Override
	public Optional<TimeSpecialDto> beforeRequestEnd(TimeSpecialDto dto, TimeSpecial savedEntity) {
		dto = alterDto(dto);
		return WithReadRestServiceTrait.super.beforeRequestEnd(dto, savedEntity);
	}

	@Override
	public RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> getRestCrudConfigBuilder() {
		RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialBo, TimeSpecialDto> builder = RestCrudConfigBuilder
				.create();
		return builder.withBeanFactory(beanFactory).withBoService(timeSpecialBo).withDtoClass(TimeSpecialDto.class)
				.withEntityClass(TimeSpecial.class)
				.withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
	}

	@Override
	public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
		return SyncHandlerBuilder.create()
				.withHandler("time_special_change", user -> activeTimeSpecialBo.findByUserWithCurrentStatus(user))
				.build();
	}

}
