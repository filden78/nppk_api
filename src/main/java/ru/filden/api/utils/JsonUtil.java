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

    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static String successResponse(Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "success");
        response.add("data", gson.toJsonTree(data));
        return gson.toJson(response);
    }

    public static String errorResponse(String message, int code) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "error");
        response.addProperty("code", code);
        response.addProperty("message", message);
        return gson.toJson(response);
    }

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

    public static void setJsonResponse(Response response) {
        response.type("application/json");
    }
}
