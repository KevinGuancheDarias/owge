package com.kevinguanchedarias.owgejava.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kevinguanchedarias.owgejava.entity.SystemMessage;
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
public class SystemMessageDto implements DtoFromEntity<SystemMessage> {
    private Integer id;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy")
    @Builder.Default
    private LocalDateTime creationDate = LocalDateTime.now(ZoneOffset.UTC);

    @Override
    public void dtoFromEntity(SystemMessage entity) {
        id = entity.getId();
        content = entity.getContent();
        creationDate = entity.getCreationDate();
    }
}
