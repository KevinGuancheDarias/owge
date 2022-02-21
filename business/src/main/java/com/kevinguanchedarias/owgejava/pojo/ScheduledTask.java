package com.kevinguanchedarias.owgejava.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a scheduled task
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTask implements Serializable {
    @Serial
    private static final long serialVersionUID = -6405371104931890932L;

    private String id;
    private String type;
    private Object content;

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    public ScheduledTask(String type, Object content) {
        super();
        this.type = type;
        this.content = content;
    }
}
