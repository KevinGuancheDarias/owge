package com.kevinguanchedarias.owgejava.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a system message with the is read property for the given user
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.16
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMessageUser {
    private Integer id;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime creationDate;
    private boolean isRead;
}
