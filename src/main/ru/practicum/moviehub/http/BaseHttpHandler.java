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
    protected static final int RESPONSE_LENGTH_NO_CONTENT = -1;
    protected static final int OK_STATUS_CODE = 200;                        // 200 OK
    protected static final int CREATE_OK_STATUS_CODE = 201;                 // 201 Created
    protected static final int DELETE_OK_STATUS_CODE = 204;                 //204 No Content
    protected static final int REQUEST_ERROR_STATUS_CODE = 400;             // 400 Bad Request
    protected static final int NOT_FOUND_ERROR_STATUS_CODE = 404;           // 404 Not Found
    protected static final int INCORRECT_METHOD_ERROR_STATUS_CODE = 405;    //405 Method Not Allowed
    protected static final int CREATE_MEDIA_TYPE_ERROR_STATUS_CODE = 415;   // 415 Unsupported Media Type
    protected static final int CREATE_VALIDATE_ERROR_STATUS_CODE = 422;     // 422 Unprocessable Entity
    protected static final String METHOD_GET_MOVIE = "GET";
    protected static final String METHOD_POST_MOVIE = "POST";
    protected static final String METHOD_DELETE_MOVIE = "DELETE";

    protected void sendJson(HttpExchange ex, ErrorResponse errorResponse, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(errorResponse.getStatusCode(), bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex, ErrorResponse errorResponse) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(errorResponse.getStatusCode(), RESPONSE_LENGTH_NO_CONTENT);
    }

    protected void sendErrorJson(HttpExchange ex, ErrorResponse errorResponse) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        Gson gson = new Gson();
        jsonResponse.addProperty("error", errorResponse.getErrorMessage());
        jsonResponse.add("details", gson.toJsonTree(errorResponse.getDetails()));
        byte[] bytes = gson.toJson(jsonResponse).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(errorResponse.getStatusCode(), bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}