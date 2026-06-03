package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8"; // Заголовок Content-Type для JSON

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        // Устанавливаем заголовок Content-Type
        ex.getResponseHeaders().set("Content-Type", CT_JSON);

        // Преобразуем JSON‑строку в массив байтов с кодировкой UTF-8
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        // Отправляем код ответа и длину тела ответа
        ex.sendResponseHeaders(status, responseBytes.length);

        // Получаем поток для записи тела ответа
        OutputStream outputStream = ex.getResponseBody();

        // Записываем тело ответа
        outputStream.write(responseBytes);

        // Закрываем поток и обмен
        outputStream.close();
        ex.close();
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        // Устанавливаем заголовок Content-Type (для согласованности, хотя тела нет)
        ex.getResponseHeaders().set("Content-Type", CT_JSON);

        // Отправляем код ответа 204 No Content без тела (длина -1)
        ex.sendResponseHeaders(204, -1);

        // Закрываем обмен — тело не отправляется
        ex.close();
    }
}
