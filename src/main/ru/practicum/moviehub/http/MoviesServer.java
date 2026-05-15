package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private static final String MOVIES_CONTEXT = "/movies";
    private static final int SERVER_BACKLOG = 0;
    private static final int DELAY_SECOND_FOR_STOP = 0;

    public MoviesServer(MoviesStore moviesStore, int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), SERVER_BACKLOG);
            server.createContext(MOVIES_CONTEXT, new MoviesHandler());

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(DELAY_SECOND_FOR_STOP);
        System.out.println("Сервер остановлен");
    }
}