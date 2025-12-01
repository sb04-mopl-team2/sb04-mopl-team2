package com.codeit.mopl.domain.message.conversation.entity;

public enum SortBy {
    CREATED_AT("createdAt");

    private String value;
    SortBy(String value) {this.value = value;}
    public String getValue() {return value;}
}
