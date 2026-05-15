package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.api.ErrorResponse;
import java.util.ArrayList;
import java.util.List;
import com.sun.net.httpserver.Headers;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;
import ru.practicum.moviehub.exception.ValidateException;

public class MoviesHandler extends BaseHttpHandler {
    private final Gson gson = new Gson();

    private void handleGetAll(HttpExchange ex) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), 200);
        sendJson(ex, errorResponse, MoviesStore.getAllMoviesAsJson());
    }

    private void handleGetFilter(HttpExchange ex, String rawQuery) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), 200);

        String[] pairs = rawQuery.split("&");
        int year = -1;

        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                try {
                    if (key.equalsIgnoreCase("year")) {
                        year = Integer.parseInt(value);
                    }
                } catch (NumberFormatException e) {
                    errorResponse.addDetails("Некорректный параметр запроса — 'year'", "Некорректный параметр запроса — 'year'", 400);
                    sendErrorJson(ex, errorResponse);
                }
            } else {
                errorResponse.addDetails("Некорректный параметр запроса — 'year'", "Некорректный параметр запроса — 'year'", 400);
                sendErrorJson(ex, errorResponse);
            }
        }

        if (year < 0) {
            errorResponse.addDetails("Некорректный параметр запроса — 'year'", "Некорректный параметр запроса — 'year'", 400);
            sendErrorJson(ex, errorResponse);
        }
        sendJson(ex, errorResponse, MoviesStore.getYearMoviesAsJson(year));
    }


    private void handleGet(HttpExchange ex, String id) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), 200);
        try {
            long idAsLong = Long.parseLong(id);
            if (MoviesStore.movieIdIsExists(idAsLong)) {
                sendJson(ex, errorResponse, MoviesStore.getMovieAsJson(idAsLong));
            } else {
                errorResponse.addDetails("Фильм не найден", "Фильм не найден", 404);
                sendErrorJson(ex, errorResponse);
            }
        } catch (NumberFormatException exception) {
            errorResponse.addDetails("Некорректный ID", "Некорректный ID", 400);
            sendErrorJson(ex, errorResponse);
        }
    }

    private void handleDelete(HttpExchange ex, String id) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), 204);
        try {
            long idAsLong = Long.parseLong(id);
            if (MoviesStore.movieIdIsExists(idAsLong)) {
                MoviesStore.deleteMovie(idAsLong);
                sendNoContent(ex, errorResponse);
            } else {
                errorResponse.addDetails("Фильм не найден", "Фильм не найден", 404);
                sendErrorJson(ex, errorResponse);
            }
        } catch (NumberFormatException exception) {
            errorResponse.addDetails("Некорректный ID", "Некорректный ID", 400);
            sendErrorJson(ex, errorResponse);
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        Headers requestHeaders = ex.getRequestHeaders();
        List<String> contentTypeValues = requestHeaders.get("Content-type");
        if (contentTypeValues == null || !contentTypeValues.contains("application/json")) {
            ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), 415);
            sendNoContent(ex, errorResponse);
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.trim().isBlank()) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка JSON", List.of("JSON пуст"), 422);
            sendErrorJson(ex, errorResponse);
        }
        try {
            JsonObject jsonObject = gson.fromJson(body, JsonObject.class);

            String title = "";
            int year = -1;

            ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), 201);

            if (jsonObject.has("title")) {
                title = jsonObject.get("title").getAsString();
            } else {
                errorResponse.addDetails("Ошибка JSON", "JSON должен содержать название фильма (title)", 422);
            }

            if (jsonObject.has("year")) {
                try {
                    year = jsonObject.get("year").getAsInt();
                } catch (NumberFormatException exception) {
                    errorResponse.addDetails("Ошибка JSON", "Параметр year должен быть числом", 422);
                }
            } else {
                errorResponse.addDetails("Ошибка JSON", "JSON должен содержать год фильма (year)", 422);
            }

            if (errorResponse.getStatusCode() == 201) {
                long id = MoviesStore.addMovie(title, year);
                sendJson(ex, errorResponse, MoviesStore.getMovieAsJson(id));
            } else {
                sendErrorJson(ex, errorResponse);
            }
        } catch (JsonSyntaxException exception) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка JSON", List.of("Некорректный формат JSON"), 422);
            sendErrorJson(ex, errorResponse);
        } catch (ValidateException exception) {
            ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), exception.getMessages(), 422);
            sendErrorJson(ex, errorResponse);
        }
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        if (method.equalsIgnoreCase("GET") && pathParts.length == 2) {
            String rawQuery = ex.getRequestURI().getRawQuery();
            if (rawQuery == null) {
                handleGetAll(ex);
            } else {
                handleGetFilter(ex, rawQuery);
            }
        } else if (method.equalsIgnoreCase("POST")) {
            handlePost(ex);
        } else if (method.equalsIgnoreCase("GET") && pathParts.length == 3) {
            handleGet(ex, pathParts[2]);
        } else if (method.equalsIgnoreCase("DELETE") && pathParts.length == 3) {
            handleDelete(ex, pathParts[2]);
        } else {
            ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), 405);
            sendNoContent(ex, errorResponse);
        }
    }
}
