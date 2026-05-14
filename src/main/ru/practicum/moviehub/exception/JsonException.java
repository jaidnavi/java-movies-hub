package ru.practicum.moviehub.exception;

import java.util.List;

public class JsonException extends RuntimeException {
    private final List<String> messages;

    public JsonException(List<String> messages, String message) {

        super(message);
        this.messages = messages;
    }

    public List<String> getMessages() {
        return messages;
    }

}
