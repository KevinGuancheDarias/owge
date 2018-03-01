package com.kevinguanchedarias.sgtjava.entity;

import java.io.Serializable;

@FunctionalInterface
public interface EntityWithId extends Serializable {
	public Number getId();
}
