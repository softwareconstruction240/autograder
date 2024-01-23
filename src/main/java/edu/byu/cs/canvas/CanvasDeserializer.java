package edu.byu.cs.canvas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Reader;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CanvasDeserializer<T> {
    public T deserialize(Reader reader, Class<T> deserializeClass) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
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
}
