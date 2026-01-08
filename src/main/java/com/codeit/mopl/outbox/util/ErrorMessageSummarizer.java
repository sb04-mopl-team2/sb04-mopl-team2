package com.codeit.mopl.outbox.util;

public class ErrorMessageSummarizer {

    public static String summarizeErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }

        if (errorMessage.length() > 4000) {
            errorMessage = errorMessage.substring(0, 3997) + "...";
            return errorMessage;
        } else {
            return errorMessage;
        }
    }
}
