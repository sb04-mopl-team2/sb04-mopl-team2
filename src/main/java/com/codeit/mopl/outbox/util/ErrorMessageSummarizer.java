package com.codeit.mopl.outbox.util;

public final class ErrorMessageSummarizer {

    public ErrorMessageSummarizer() {
    }

    private static final int MAX_LENGTH = 4000;
    private static final String ELLIPSIS = "...";

    public static String summarizeErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        return errorMessage.length() > MAX_LENGTH
                ? errorMessage.substring(0, MAX_LENGTH - ELLIPSIS.length()) + ELLIPSIS
                : errorMessage;
    }
}
