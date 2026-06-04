package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private static final Gson gson = new Gson();
    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        try {
            if (method.equalsIgnoreCase("GET")) {
                if (query != null && query.startsWith("year=")) {
                    handleGetByYear(ex, query);
                } else if (path.matches("/movies/\\d+")) {
                    handleGetById(ex, path);
                } else {
                    handleGetAll(ex);
                }
            } else if (method.equalsIgnoreCase("POST")) {
                handlePost(ex);
            } else if (method.equalsIgnoreCase("DELETE")) {
                if (path.matches("/movies/\\d+")) {
                    handleDelete(ex, path);
                } else {
                    sendJson(ex, 400,
                            gson.toJson(new ErrorResponse("Некорректный ID", null)));
                }
            } else {
                ex.sendResponseHeaders(405, -1);
                ex.close();
            }
        } catch (Exception e) {
            sendJson(ex, 500,
                    gson.toJson(new ErrorResponse("Внутренняя ошибка сервера", null)));
        }
    }

    private void handleGetAll(HttpExchange ex) throws IOException {
        List<Movie> movies = store.getAll();
        sendJson(ex, 200, gson.toJson(movies));
    }

    private void handleGetByYear(HttpExchange ex, String query) throws IOException {
        try {
            int year = Integer.parseInt(query.substring(5));
            List<Movie> movies = store.getByYear(year);
            sendJson(ex, 200, gson.toJson(movies));
        } catch (NumberFormatException e) {
            sendJson(ex, 400,
                    gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'", null)));
        }
    }

    private void handleGetById(HttpExchange ex, String path) throws IOException {
        try {
            Long id = Long.parseLong(path.substring(8));
            Movie movie = store.getById(id);
            if (movie != null) {
                sendJson(ex, 200, gson.toJson(movie));
            } else {
                sendJson(ex, 404,
                        gson.toJson(new ErrorResponse("Фильм не найден", null)));
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400,
                    gson.toJson(new ErrorResponse("Некорректный ID", null)));
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            sendJson(ex, 415,
                    gson.toJson(new ErrorResponse("Unsupported Media Type", null)));
            return;
        }

        String requestBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (requestBody.isBlank()) {
            sendJson(ex, 422,
                    gson.toJson(new ErrorResponse("Ошибка валидации",
                            List.of("тело запроса не может быть пустым"))));
            return;
        }

        Movie movie;
        try {
            movie = gson.fromJson(requestBody, Movie.class);
        } catch (Exception e) {
            sendJson(ex, 422,
                    gson.toJson(new ErrorResponse("Ошибка валидации",
                            List.of("некорректный JSON"))));
            return;
        }

        // Валидация данных
        List<String> validationErrors = validateMovie(movie);
        if (!validationErrors.isEmpty()) {
            sendJson(ex, 422,
                    gson.toJson(new ErrorResponse("Ошибка валидации", validationErrors)));
            return;
        }

        // Добавляем фильм в хранилище
        Movie savedMovie = store.add(movie);
        sendJson(ex, 201, gson.toJson(savedMovie));
    }

    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        int currentYear = Year.now().getValue();

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("название не может превышать 100 символов");
        }

        if (movie.getYear() == null) {
            errors.add("год выпуска обязателен");
        } else if (movie.getYear() < 1888 || movie.getYear() > currentYear + 1) {
            errors.add("год должен быть между 1888 и " + (currentYear + 1));
        }

        return errors;
    }

    private void handleDelete(HttpExchange ex, String path) throws IOException {
        try {
            Long id = Long.parseLong(path.substring(8));
            if (store.delete(id)) {
                sendNoContent(ex);
            } else {
                sendJson(ex, 404,
                        gson.toJson(new ErrorResponse("Фильм не найден", null)));
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400,
                    gson.toJson(new ErrorResponse("Некорректный ID", null)));
        }
    }
}