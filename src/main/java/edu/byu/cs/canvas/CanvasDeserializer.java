package edu.byu.cs.canvas;

import com.google.gson.*;
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
            for (var entry : map.entrySet()) {
                String key = entry.getKey();
                Map<String, Object> value = entry.getValue();

                String comments = (String) value.get("comments");

                Double points = ((Double) value.get("points"));

                float score = (points == null) ? 0 : points.floatValue();

                items.put(key, new CanvasIntegration.RubricItem(comments, score));
            }
            return new CanvasIntegration.RubricAssessment(items);
        }
    }
}
