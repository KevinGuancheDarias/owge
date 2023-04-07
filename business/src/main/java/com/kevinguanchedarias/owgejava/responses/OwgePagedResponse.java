package com.kevinguanchedarias.owgejava.responses;


import java.util.List;

public record OwgePagedResponse<E>(long count, List<E> elements) {
}
