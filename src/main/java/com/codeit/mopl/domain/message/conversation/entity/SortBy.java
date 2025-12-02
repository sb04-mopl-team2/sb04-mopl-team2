package com.codeit.mopl.domain.message.conversation.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SortBy {
    CREATED_AT("createdAt");

    private String value;
    SortBy(String value) {this.value = value;}
    public String getValue() {return value;}

    @JsonValue
    public String toJson() {
        return value;
    }

    @JsonCreator
    public static SortBy from(String value) {
        for (SortBy v : values()) {
            if (v.value.equalsIgnoreCase(value)) return v;
        }
        return SortBy.valueOf(value.toUpperCase());
    }
}
