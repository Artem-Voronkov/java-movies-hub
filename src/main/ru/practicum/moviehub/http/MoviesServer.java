package ru.practicum.moviehub.http;


import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final int port;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        this.port = port;
        try {
            // Создаём сервер на указанном порту
            server = HttpServer.create(new InetSocketAddress(port), 0);

            // Добавляем контекст для /movies и передаём хранилище в хендлер
            server.createContext("/movies", new MoviesHandler(store));

            server.setExecutor(null);

        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP‑сервер на порту " + port, e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен на порту " + port);
    }

    public void stop(int delay) {
        server.stop(delay);
        System.out.println("Сервер остановлен");
    }

    public void stop() {
        stop(0);
    }

    public int getPort() {
        return port;
    }
}
