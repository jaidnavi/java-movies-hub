package ru.practicum.moviehub.exception;

import java.util.List;

public class ValidateException extends Exception {
    private final List<String> messages;

    public ValidateException(List<String> messages, String message) {
        super(message);
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }
}
