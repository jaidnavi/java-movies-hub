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

    private static final String SPLIT_PAIRS = "&";
    private static final String SPLIT_VALUES = "=";
    private static final String SPLIT_PATH_PARTS = "/";
    private static final String FILTER_FIELD = "year";
    private static final int PATH_PARTS_COUNT_GET_ONE_MOVIE = 3;
    private static final int PATH_PARTS_COUNT_DELETE_ONE_MOVIE = 3;
    private static final int PATH_PARTS_COUNT_GET_MANY_MOVIES = 2;
    private static final String JSON_YEAR_FIELD = "year";
    private static final String JSON_TITLE_FIELD = "title";
    private static final String DEFAULT_TITLE = "";
    private static final int DEFAULT_YEAR = -1;
    private final Gson gson = new Gson();

    private void handleGetAll(HttpExchange ex) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), OK_STATUS_CODE);
        sendJson(ex, errorResponse, MoviesStore.getAllMoviesAsJson());
    }

    private void handleGetFilter(HttpExchange ex, String rawQuery) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), OK_STATUS_CODE);

        String[] pairs = rawQuery.split(SPLIT_PAIRS);

        int year = DEFAULT_YEAR;

        for (String pair : pairs) {
            int idx = pair.indexOf(SPLIT_VALUES);
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                try {
                    if (key.equalsIgnoreCase(FILTER_FIELD)) {
                        year = Integer.parseInt(value);
                    }
                } catch (NumberFormatException e) {
                    errorResponse.addDetails("Некорректный параметр запроса — '" + FILTER_FIELD + "'", "Некорректный параметр запроса — '" + FILTER_FIELD + "'", REQUEST_ERROR_STATUS_CODE);
                    sendErrorJson(ex, errorResponse);
                }
            } else {
                errorResponse.addDetails("Некорректный параметр запроса — '" + FILTER_FIELD + "'", "Некорректный параметр запроса — '" + FILTER_FIELD + "'", REQUEST_ERROR_STATUS_CODE);
                sendErrorJson(ex, errorResponse);
            }
        }

        if (year == DEFAULT_YEAR) {
            errorResponse.addDetails("Некорректный параметр запроса — '" + FILTER_FIELD + "'", "Некорректный параметр запроса — '" + FILTER_FIELD + "'", REQUEST_ERROR_STATUS_CODE);
            sendErrorJson(ex, errorResponse);
        }
        sendJson(ex, errorResponse, MoviesStore.getYearMoviesAsJson(year));
    }


    private void handleGet(HttpExchange ex, String id) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), OK_STATUS_CODE);
        try {
            long idAsLong = Long.parseLong(id);
            if (MoviesStore.movieIdIsExists(idAsLong)) {
                sendJson(ex, errorResponse, MoviesStore.getMovieAsJson(idAsLong));
            } else {
                errorResponse.addDetails("Фильм не найден", "Фильм не найден", NOT_FOUND_ERROR_STATUS_CODE);
                sendErrorJson(ex, errorResponse);
            }
        } catch (NumberFormatException exception) {
            errorResponse.addDetails("Некорректный ID", "Некорректный ID", REQUEST_ERROR_STATUS_CODE);
            sendErrorJson(ex, errorResponse);
        }
    }

    private void handleDelete(HttpExchange ex, String id) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), DELETE_OK_STATUS_CODE);
        try {
            long idAsLong = Long.parseLong(id);
            if (MoviesStore.movieIdIsExists(idAsLong)) {
                MoviesStore.deleteMovie(idAsLong);
                sendNoContent(ex, errorResponse);
            } else {
                errorResponse.addDetails("Фильм не найден", "Фильм не найден", NOT_FOUND_ERROR_STATUS_CODE);
                sendErrorJson(ex, errorResponse);
            }
        } catch (NumberFormatException exception) {
            errorResponse.addDetails("Некорректный ID", "Некорректный ID", REQUEST_ERROR_STATUS_CODE);
            sendErrorJson(ex, errorResponse);
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        Headers requestHeaders = ex.getRequestHeaders();
        List<String> contentTypeValues = requestHeaders.get("Content-type");
        if (contentTypeValues == null || !contentTypeValues.contains("application/json")) {
            ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), CREATE_MEDIA_TYPE_ERROR_STATUS_CODE);
            sendNoContent(ex, errorResponse);
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (body.trim().isBlank()) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка JSON", List.of("JSON пуст"), CREATE_VALIDATE_ERROR_STATUS_CODE);
            sendErrorJson(ex, errorResponse);
        }
        try {
            JsonObject jsonObject = gson.fromJson(body, JsonObject.class);

            String title = DEFAULT_TITLE;
            int year = DEFAULT_YEAR;

            ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), CREATE_OK_STATUS_CODE);

            if (jsonObject.has(JSON_TITLE_FIELD)) {
                title = jsonObject.get(JSON_TITLE_FIELD).getAsString();
            } else {
                errorResponse.addDetails("Ошибка JSON", "JSON должен содержать название фильма (" + JSON_TITLE_FIELD + ")", CREATE_VALIDATE_ERROR_STATUS_CODE);
            }

            if (jsonObject.has(JSON_YEAR_FIELD)) {
                try {
                    year = jsonObject.get(JSON_YEAR_FIELD).getAsInt();
                } catch (NumberFormatException exception) {
                    errorResponse.addDetails("Ошибка JSON", "Параметр " + JSON_YEAR_FIELD + " должен быть числом", CREATE_VALIDATE_ERROR_STATUS_CODE);
                }
            } else {
                errorResponse.addDetails("Ошибка JSON", "JSON должен содержать год фильма (" + JSON_YEAR_FIELD + ")", CREATE_VALIDATE_ERROR_STATUS_CODE);
            }

            if (errorResponse.getStatusCode() == CREATE_OK_STATUS_CODE) {
                long id = MoviesStore.addMovie(title, year);
                sendJson(ex, errorResponse, MoviesStore.getMovieAsJson(id));
            } else {
                sendErrorJson(ex, errorResponse);
            }
        } catch (JsonSyntaxException exception) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка JSON", List.of("Некорректный формат JSON"), CREATE_VALIDATE_ERROR_STATUS_CODE);
            sendErrorJson(ex, errorResponse);
        } catch (ValidateException exception) {
            ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), exception.getMessages(), CREATE_VALIDATE_ERROR_STATUS_CODE);
            sendErrorJson(ex, errorResponse);
        }
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] pathParts = path.split(SPLIT_PATH_PARTS);

        if (method.equalsIgnoreCase(METHOD_GET_MOVIE) && pathParts.length == PATH_PARTS_COUNT_GET_MANY_MOVIES) {
            String rawQuery = ex.getRequestURI().getRawQuery();
            if (rawQuery == null) {
                handleGetAll(ex);
            } else {
                handleGetFilter(ex, rawQuery);
            }
        } else if (method.equalsIgnoreCase(METHOD_POST_MOVIE)) {
            handlePost(ex);
        } else if (method.equalsIgnoreCase(METHOD_GET_MOVIE) && pathParts.length == PATH_PARTS_COUNT_GET_ONE_MOVIE) {
            handleGet(ex, pathParts[PATH_PARTS_COUNT_GET_ONE_MOVIE - 1]);
        } else if (method.equalsIgnoreCase(METHOD_DELETE_MOVIE) && pathParts.length == PATH_PARTS_COUNT_DELETE_ONE_MOVIE) {
            handleDelete(ex, pathParts[PATH_PARTS_COUNT_DELETE_ONE_MOVIE - 1]);
        } else {
            ErrorResponse errorResponse = new ErrorResponse(null, new ArrayList<>(), INCORRECT_METHOD_ERROR_STATUS_CODE);
            sendNoContent(ex, errorResponse);
        }
    }
}
