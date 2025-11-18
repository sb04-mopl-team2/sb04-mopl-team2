package com.codeit.mopl.domain.playlist.entity;

public enum SortBy {
    UPDATED_AT("updatedAt"),
    SUBSCRIBER_COUNT("subscribeCount");

    private String value;

    SortBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
