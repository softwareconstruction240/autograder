package edu.byu.cs.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.byu.cs.model.Submission;

import java.io.Reader;
import java.time.Instant;

public class Serializer {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
            .create();

    public static String serialize(Object obj) {
        return GSON.toJson(obj);
    }

    public static <T> T deserialize(String jsonStr, Class<T> classOfT) {
        return GSON.fromJson(jsonStr, classOfT);
    }

    public static <T> T deserialize(Reader reader, Class<T> classOfT) {
        return GSON.fromJson(reader, classOfT);
    }
}
