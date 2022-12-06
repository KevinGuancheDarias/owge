package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.SystemMessage;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemMessageDto implements WithDtoFromEntityTrait<SystemMessage> {
    private Integer id;
    private String content;

    @Builder.Default
    private LocalDateTime creationDate = LocalDateTime.now(ZoneOffset.UTC);
}
