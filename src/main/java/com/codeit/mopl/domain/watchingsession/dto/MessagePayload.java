package com.codeit.mopl.domain.watchingsession.dto;

import java.io.Serializable;

public record MessagePayload(
    String destination,
    Object content
) implements Serializable {
}
