package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

import ru.practicum.moviehub.store.MoviesStore;

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

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            sendJson(ex, 200, MoviesStore.getAllMoviesAsJson());
        } else if (method.equalsIgnoreCase("POST")) {
            Headers requestHeaders = ex.getRequestHeaders();
            List<String> contentTypeValues = requestHeaders.get("Content-type");
            if (contentTypeValues == null || !contentTypeValues.contains("application/json")) {
                sendNoContent(ex, 415);
                return;
            }
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (body.trim().isBlank()) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Ошибка валидации");
                errorResponse.add("details", gson.toJsonTree(List.of("JSON пуст")));
                sendJson(ex, 422, gson.toJson(errorResponse));
            }
            try {
                JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
                String title = "";
                int year = -1;

                List<String> validateExceptions = new ArrayList<>();

                if (jsonObject.has("title")) {
                    title = jsonObject.get("title").getAsString();
                } else {
                    validateExceptions.add("JSON должен содержать название фильма (title)");
                }

                if (jsonObject.has("year")) {
                    try {
                        year = jsonObject.get("year").getAsInt();
                    } catch (NumberFormatException exception) {
                        validateExceptions.add("Параметр year должен быть числом");
                    }
                } else {
                    validateExceptions.add("JSON должен содержать год фильма (year)");
                }

                if (validateExceptions.size() > 0) {
                    throw new ValidateException(validateExceptions, "Ошибка валидации");
                }

                long id = MoviesStore.addMovie(title, year);

                sendJson(ex, 201, MoviesStore.getMovieAsJson(id));

            } catch (JsonSyntaxException exception) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", "Ошибка валидации");
                errorResponse.add("details", gson.toJsonTree(List.of("Некорректный формат JSON")));
                sendJson(ex, 422, gson.toJson(errorResponse));

            } catch (ValidateException exception) {
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("error", exception.getMessage());
                errorResponse.add("details", gson.toJsonTree(exception.getMessages()));
                sendJson(ex, 422, gson.toJson(errorResponse));

            }
        }
    }
}
