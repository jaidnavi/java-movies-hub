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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.ArrayList;


public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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
                .uri(URI.create(BASE + "/movies")) // !!! Добавьте правильный URI
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

    //GET /movies/{id}
    //возвращает фильм по существующему id;
    //возвращает ошибку, если фильм не найден;
    //возвращает ошибку, если id не число.
    //DELETE /movies/{id}
    //удаляет фильм по существующему id;
    //возвращает ошибку, если фильм не найден;
    //возвращает ошибку, если id не число.
    //GET /movies?year=YYYY
    //возвращает фильмы указанного года;
    //возвращает пустой список, если фильмов с таким годом нет;
    //возвращает ошибку, если параметр year не число.
}