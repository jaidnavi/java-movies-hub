package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private String errorMessage;
    private final List<String> details;
    private int statusCode;

    public ErrorResponse(String errorMessage, List<String> details, int statusCode) {
        this.errorMessage = errorMessage;
        this.details = details;
        this.statusCode = statusCode;
    }

    public void addDetails(String errorMessage, String detail, int statusCode) {
        this.errorMessage = errorMessage;
        this.details.add(detail);
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }


    public List<String> getDetails() {
        return details;
    }


    public int getStatusCode() {
        return statusCode;
    }

}