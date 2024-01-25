package edu.byu.cs.canvas;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class CanvasDeserializer<T> {
    public T deserialize(Reader reader, Class<T> deserializeClass) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
                .registerTypeAdapter(CanvasIntegration.RubricAssessment.class, new RubricAssessmentAdapter())
                .create();
        return gson.fromJson(reader, deserializeClass);
    }
    private static class ZonedDateTimeAdapter extends TypeAdapter<ZonedDateTime> {
        @Override
        public void write(JsonWriter jsonWriter, ZonedDateTime zonedDateTime) throws IOException {
        }

        @Override
        public ZonedDateTime read(JsonReader jsonReader) throws IOException {
            ZonedDateTime utc = ZonedDateTime.parse(jsonReader.nextString());
            return utc.withZoneSameInstant(ZoneId.of("America/Denver"));
        }
    }

    private static class RubricAssessmentAdapter implements JsonDeserializer<CanvasIntegration.RubricAssessment> {
        @Override
        public CanvasIntegration.RubricAssessment deserialize(JsonElement jsonElement, Type type,
                                                              JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            Map<String, Map<String, Object>> map = jsonDeserializationContext.deserialize(jsonElement, Map.class);
            Map<String, CanvasIntegration.RubricItem> items = new HashMap<>();
            for(Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                items.put(entry.getKey(), new CanvasIntegration.RubricItem((String) entry.getValue().get("comments"),
                        ((Double) entry.getValue().get("points")).floatValue()));
            }
            return new CanvasIntegration.RubricAssessment(items);
        }
    }
}
