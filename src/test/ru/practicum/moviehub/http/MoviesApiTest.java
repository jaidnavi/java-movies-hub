package ru.practicum.moviehub.http;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.store.MoviesStore;
import ru.practicum.moviehub.model.Movie;

import java.util.List;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;


public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void clearMovies() {
        MoviesStore.clearMovies();
    }

    @Test
    void getMovies_whenNotEmpty_returnsArray() throws Exception {

        MoviesStore.addMovie("Тестовый фильм 1", 1994);
        MoviesStore.addMovie("Тестовый фильм 2", 1995);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());

        assertEquals(2, movies.size(), "Должно вернуться ровно 2 фильма");
        assertEquals("Тестовый фильм 1", movies.get(0).getTitle());
        assertEquals("Тестовый фильм 2", movies.get(1).getTitle());

    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void postMovies_whenCorrectData_addMovies() throws Exception {

        String jsonBody = "{\"title\":\"Тестовый фильм 1\",\"year\":1994}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size(), "Должно вернуться ровно 1 фильм");
        assertEquals("Тестовый фильм 1", movies.get(0).getTitle());
        assertTrue(movies.get(0).getId() > 0, "Идентификатор фильма должен быть больше 0");
    }

    @Test
    void postMovies_whenEmptyTitle_getError() throws Exception {

        String jsonBody = "{\"title\":\"\",\"year\":2026}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Ошибка валидации", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Ошибка валидации'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");

        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Название не должно быть пустым"),
                "Массив должен содержать ошибку валидации названия");
    }

    @Test
    void postMovies_whenOverflowTitle_getError() throws Exception {

        String jsonBody = "{\"title\":\"12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901\",\"year\":2026}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Ошибка валидации", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Ошибка валидации'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");

        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Название не должно превышать 100 символов"),
                "Массив должен содержать ошибку валидации названия");
    }

    @Test
    void postMovies_whenIncorrectYear_getError() throws Exception {

        String jsonBody = "{\"title\":\"Тестовый фильм 1\",\"year\":1887}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Ошибка валидации", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Ошибка валидации'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");

        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Год должен быть между 1888 и 2027"),
                "Массив должен содержать ошибку валидации года");
    }

    @Test
    void postMovies_whenIncorrectContentType_getError() throws Exception {

        String jsonBody = "{\"title\":\"Тестовый фильм 1\",\"year\":1887}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "xxxx")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");
    }

    @Test
    void postMovies_whenIncorrectJson_getError() throws Exception {

        String jsonBody = "{\"title\":\"Тестовый фильм 1\",\"year\":1887";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Ошибка JSON", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Ошибка JSON'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");

        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Некорректный формат JSON"),
                "Массив должен содержать ошибку формата JSON");
    }

    @Test
    void postMovies_whenNoYear_getError() throws Exception {

        String jsonBody = "{\"title\":\"Тестовый фильм 1\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Ошибка JSON", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Ошибка JSON'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");

        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("JSON должен содержать год фильма (year)"),
                "Массив должен содержать ошибку наличия поля year");
    }

    @Test
    void postMovies_whenNoTitle_getError() throws Exception {

        String jsonBody = "{\"year\":1887}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Ошибка JSON", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Ошибка JSON'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");


        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("JSON должен содержать название фильма (title)"),
                "Массив должен содержать ошибку наличия поля title");
    }

    @Test
    void postMovies_whenIncorrectData_getManyError() throws Exception {

        String jsonBody = "{\"title\":\"\",\"year\":1700}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Ошибка валидации", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Ошибка валидации'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");

        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(2, detailsArray.size(), "В массиве 'details' должно быть ровно 2 ошибки");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Название не должно быть пустым"),
                "Массив должен содержать ошибку валидации названия");
        assertTrue(detailsList.contains("Год должен быть между 1888 и 2027"),
                "Массив должен содержать ошибку валидации года");
    }

    @Test
    void getMoviesId_whenMovieExists_getMovie() throws Exception {
        long id1 = MoviesStore.addMovie("Тестовый фильм 1", 1994);
        long id2 = MoviesStore.addMovie("Тестовый фильм 2", 1993);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id1))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());

        assertEquals(1, movies.size(), "Должно вернуться ровно 1 фильм");
        assertEquals("Тестовый фильм 1", movies.get(0).getTitle());
    }

    @Test
    void getMoviesId_whenMovieNotExists_getError() throws Exception {
        long id1 = MoviesStore.addMovie("Тестовый фильм 1", 1994);
        long id2 = MoviesStore.addMovie("Тестовый фильм 2", 1993);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + 666))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Фильм не найден", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Фильм не найден'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");


        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Фильм не найден"),
                "Массив должен содержать ошибку Фильм не найден");
    }

    @Test
    void getMoviesId_whenIdNotNumber_getError() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/пара-пам-па-пам"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/пара-пам-па-пам должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Некорректный ID", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Некорректный ID'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");


        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Некорректный ID"),
                "Массив должен содержать ошибку о некорректном ID");

    }

    @Test
    void deleteMoviesId_whenMovieExists_deleteMovie() throws Exception {
        long id1 = MoviesStore.addMovie("Тестовый фильм 1", 1994);
        long id2 = MoviesStore.addMovie("Тестовый фильм 2", 1993);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + id1))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        assertFalse(MoviesStore.movieIdIsExists(id1), "Первый фильм должен быть удален");
        assertTrue(MoviesStore.movieIdIsExists(id2), "Второй фильм должен оставаться в кинотеатре");
    }

    @Test
    void deleteMoviesId_whenMovieNotExists_getError() throws Exception {
        long id1 = MoviesStore.addMovie("Тестовый фильм 1", 1994);
        long id2 = MoviesStore.addMovie("Тестовый фильм 2", 1993);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + 666))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Фильм не найден", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Фильм не найден'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");


        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Фильм не найден"),
                "Массив должен содержать ошибку Фильм не найден");
    }

    @Test
    void deleteMoviesId_whenIdNotNumber_getError() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/пара-пам-па-пам"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/пара-пам-па-пам должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Некорректный ID", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Некорректный ID'");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");


        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Некорректный ID"),
                "Массив должен содержать ошибку о некорректном ID");

    }

    @Test
    void getMoviesFilter_whenMoviesExists_getMovies() throws Exception {
        MoviesStore.addMovie("Тестовый фильм 1", 1991);
        MoviesStore.addMovie("Тестовый фильм 2", 1992);
        MoviesStore.addMovie("Тестовый фильм 3", 1992);
        MoviesStore.addMovie("Тестовый фильм 4", 1993);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1992"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=1992 должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());

        assertEquals(2, movies.size(), "Должно вернуться ровно 2 фильма");

        assertEquals("Тестовый фильм 2", movies.get(0).getTitle());
        assertEquals("Тестовый фильм 3", movies.get(1).getTitle());

    }

    @Test
    void getMoviesFilter_whenMoviesNotExists_EmptyMovies() throws Exception {
        MoviesStore.addMovie("Тестовый фильм 1", 1991);
        MoviesStore.addMovie("Тестовый фильм 2", 1992);
        MoviesStore.addMovie("Тестовый фильм 3", 1992);
        MoviesStore.addMovie("Тестовый фильм 4", 1993);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1990"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=1990 должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(body, new ListOfMoviesTypeToken().getType());

        assertEquals(0, movies.size(), "Должно вернуться ровно 0 фильмов");

    }

    @Test
    void getMoviesFilter_whenErrorFilter_getError() throws Exception {
        MoviesStore.addMovie("Тестовый фильм 1", 1991);
        MoviesStore.addMovie("Тестовый фильм 2", 1992);
        MoviesStore.addMovie("Тестовый фильм 3", 1992);
        MoviesStore.addMovie("Тестовый фильм 4", 1993);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=парам-пам-парам"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/пара-пам-па-пам должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        Gson gson = new Gson();

        JsonObject errorResponse = gson.fromJson(body, JsonObject.class);

        assertTrue(errorResponse.has("error"), "Ответ должен содержать поле 'error'");
        assertEquals("Некорректный параметр запроса — 'year'", errorResponse.get("error").getAsString(),
                "Поле 'error' должно содержать текст 'Некорректный параметр запроса — 'year''");

        assertTrue(errorResponse.has("details"), "Ответ должен содержать поле 'details'");
        assertTrue(errorResponse.get("details").isJsonArray(), "Поле 'details' должно быть JSON-массивом");


        JsonArray detailsArray = errorResponse.getAsJsonArray("details");
        assertEquals(1, detailsArray.size(), "В массиве 'details' должно быть ровно 1 ошибка");

        List<String> detailsList = new ArrayList<>();
        detailsArray.forEach(element -> detailsList.add(element.getAsString()));

        assertTrue(detailsList.contains("Некорректный параметр запроса — 'year'"),
                "Некорректный параметр запроса — 'year'");
    }
}