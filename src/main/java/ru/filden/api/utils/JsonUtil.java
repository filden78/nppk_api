package ru.filden.api.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import spark.Response;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    // Преобразование объекта в JSON строку
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    // Преобразование JSON строки в объект
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    // Формирование успешного ответа
    public static String successResponse(Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", gson.toJsonTree(data));
        return gson.toJson(response);
    }

    // Формирование ответа с ошибкой
    public static String errorResponse(String message, int code) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("code", code);
        response.addProperty("message", message);
        return gson.toJson(response);
    }

    // Формирование ответа с пагинацией
    public static String paginatedResponse(Object data, int page, int size, long total) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", gson.toJsonTree(data));

        JsonObject pagination = new JsonObject();
        pagination.addProperty("page", page);
        pagination.addProperty("size", size);
        pagination.addProperty("total", total);
        pagination.addProperty("pages", (int) Math.ceil((double) total / size));
        response.add("pagination", pagination);

        return gson.toJson(response);
    }

    // Утилитный метод для установки JSON типа ответа
    public static void setJsonResponse(Response response) {
        response.type("application/json");
    }
}
