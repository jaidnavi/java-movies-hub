package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.io.IOException;
import ru.practicum.moviehub.api.ErrorResponse;
import com.google.gson.JsonObject;

abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange ex, ErrorResponse errorResponse, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(errorResponse.getStatusCode(), 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex, ErrorResponse errorResponse) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(errorResponse.getStatusCode(), -1);
    }

    protected void sendErrorJson(HttpExchange ex, ErrorResponse errorResponse) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        Gson gson = new Gson();
        jsonResponse.addProperty("error", errorResponse.getErrorMessage());
        jsonResponse.add("details", gson.toJsonTree(errorResponse.getDetails()));
        byte[] bytes = gson.toJson(jsonResponse).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(errorResponse.getStatusCode(), 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}