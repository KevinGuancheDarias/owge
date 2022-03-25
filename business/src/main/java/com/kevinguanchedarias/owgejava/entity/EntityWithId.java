package com.kevinguanchedarias.owgejava.entity;

import java.io.Serializable;

public interface EntityWithId<K> extends Serializable {
    K getId();

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    void setId(K id);
}
