/**
 *
 */
package com.kevinguanchedarias.owgejava.rest.game;

import com.kevinguanchedarias.owgejava.builder.RestCrudConfigBuilder;
import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo;
import com.kevinguanchedarias.owgejava.business.SupportedOperationsBuilder;
import com.kevinguanchedarias.owgejava.dto.ActiveTimeSpecialDto;
import com.kevinguanchedarias.owgejava.dto.TimeSpecialDto;
import com.kevinguanchedarias.owgejava.entity.TimeSpecial;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import com.kevinguanchedarias.owgejava.repository.TimeSpecialRepository;
import com.kevinguanchedarias.owgejava.rest.trait.WithReadRestServiceTrait;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@RestController
@ApplicationScope
@RequestMapping("game/time_special")
@AllArgsConstructor
public class TimeSpecialRestService implements
        WithReadRestServiceTrait<Integer, TimeSpecial, TimeSpecialRepository, TimeSpecialDto>, SyncSource {

    private final TimeSpecialRepository timeSpecialRepository;
    private final ActiveTimeSpecialBo activeTimeSpecialBo;
    private final AutowireCapableBeanFactory beanFactory;

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
        return WithReadRestServiceTrait.super.beforeRequestEnd(dto, savedEntity);
    }

    @Override
    public RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialRepository, TimeSpecialDto> getRestCrudConfigBuilder() {
        RestCrudConfigBuilder<Integer, TimeSpecial, TimeSpecialRepository, TimeSpecialDto> builder = RestCrudConfigBuilder
                .create();
        return builder.withBeanFactory(beanFactory).withRepository(timeSpecialRepository).withDtoClass(TimeSpecialDto.class)
                .withEntityClass(TimeSpecial.class)
                .withSupportedOperationsBuilder(SupportedOperationsBuilder.create().withFullPrivilege());
    }

    @Override
    public Map<String, Function<UserStorage, Object>> findSyncHandlers() {
        return SyncHandlerBuilder.create()
                .withHandler("time_special_change", user -> {
                    var retVal = activeTimeSpecialBo.findByUserWithCurrentStatus(user);
                    recomputeDates(retVal);
                    return retVal;
                })
                .build();
    }

    private void recomputeDates(List<TimeSpecialDto> timeSpecialDtoList) {
        timeSpecialDtoList.stream()
                .map(TimeSpecialDto::getActiveTimeSpecialDto)
                .filter(Objects::nonNull)
                .forEach(ActiveTimeSpecialDto::calculatePendingMillis);
    }

}
