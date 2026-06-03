package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class MoviesHandler extends BaseHttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        if (method.equalsIgnoreCase("GET")) {
            // Используем метод sendJson для отправки пустого JSON‑массива с кодом 200
            sendJson(ex, 200, "[]");
        } else {
            // Для других методов отправляем 405 Method Not Allowed
            ex.sendResponseHeaders(405, -1);
            ex.close();
        }
    }
}
