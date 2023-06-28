package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.dto.CommonDto;
import com.kevinguanchedarias.owgejava.entity.CommonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommonEntityService {
    public <K extends Serializable, E extends CommonEntity<K>> Map<K, CommonDto<K, E>> entitiesByIdToDto(
            JpaRepository<E, K> repository,
            Set<K> ids
    ) {
        return repository.findAllById(ids).stream()
                .map(entity -> {
                    var dto = new CommonDto<K, E>() {
                    };
                    dto.dtoFromEntity(entity);
                    return dto;
                })
                .collect(
                        Collectors.toUnmodifiableMap(CommonDto::getId, dto -> dto)
                );
    }
}
