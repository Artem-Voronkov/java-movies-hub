package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private MoviesServer server;
    private HttpClient client;
    private MoviesStore store;
    private Gson gson = new Gson();

    @BeforeEach
    void setUp() throws Exception {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
        client = HttpClient.newHttpClient();
        Thread.sleep(500); // Ждём запуска сервера
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body().trim());
    }

    @Test
    void postMovie_withValidData_returnsCreated() throws Exception {
        Movie movie = new Movie("Интерстеллар", 2014);
        String json = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, response.statusCode());
        Movie savedMovie = gson.fromJson(response.body(), Movie.class);
        assertNotNull(savedMovie.getId());
        assertEquals("Интерстеллар", savedMovie.getTitle());
        assertEquals(2014, savedMovie.getYear());
    }

    @Test
    void getMovies_whenNotEmpty_returnsAllMovies() throws Exception {
        // Добавляем фильмы
        store.add(new Movie("Матрица", 1999));
        store.add(new Movie("Начало", 2010));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        List<Movie> movies = gson.fromJson(
                response.body(),
                new com.google.gson.reflect.TypeToken<List<Movie>>() {}.getType()
        );
        assertEquals(2, movies.size());
    }

    @Test
    void getMoviesByYear_returnsFilteredList() throws Exception {
        store.add(new Movie("Годзилла", 1954));
        store.add(new Movie("Кинг-Конг", 1933));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=1954"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, response.statusCode());

        List<Movie> movies = gson.fromJson(
                response.body(),
                new com.google.gson.reflect.TypeToken<List<Movie>>() {}.getType()
        );
        assertEquals(1, movies.size());
        assertEquals("Годзилла", movies.get(0).getTitle());
    }

    @Test
    void deleteMovie_whenExists_returnsNoContent() throws Exception {
        Movie saved = store.add(new Movie("Удали меня", 2020));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + saved.getId()))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertNull(store.getById(saved.getId()));
    }

    @Test
    void getMovieById_whenNotExists_returnsNotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, response.statusCode());

        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void postMovie_withEmptyTitle_returnsUnprocessableEntity() throws Exception {
        Movie movie = new Movie("", 2020);
        String json = gson.toJson(movie);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, response.statusCode());

        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertTrue(error.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_withUnsupportedContentType_returnsUnsupportedMediaType() throws Exception {
        String json = "{\"title\": \"Тест\", \"year\": 2020}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "text/plain")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, response.statusCode());

        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Неподдерживаемый тип контента. Требуется application/json", error.getError());
    }
}
