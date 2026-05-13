package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import ru.practicum.moviehub.store.MoviesStore;

public class MoviesHandler extends BaseHttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            sendJson(ex, 200, MoviesStore.getAllMoviesAsJson());
        }
    }
}
