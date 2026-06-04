package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;


public class MovieHubApp {
    public static void main(String[] args) {
        MoviesStore store = new MoviesStore();
        MoviesServer server = null;

        server = new MoviesServer(store, 8080);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println("MovieHub API запущен на http://localhost:8080");
        System.out.println("Доступные эндпоинты:");
        System.out.println("  GET  /movies — все фильмы");
        System.out.println("  GET  /movies?year=YYYY — фильмы за год");
        System.out.println("  GET  /movies/{id} — фильм по ID");
        System.out.println("  POST /movies — добавить фильм");
        System.out.println("  DELETE /movies/{id} — удалить фильм");

    }
}
