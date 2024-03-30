package edu.byu.cs.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public record Submission(
        String netId,
        String repoUrl,
        String headHash,
        Instant timestamp,
        Phase phase,
        Boolean passed,
        Float score,
        String notes,
        Rubric rubric,
        Boolean admin
) {
    public static class InstantAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter jsonWriter, Instant instant) throws IOException {
            jsonWriter.value(instant.toString());
        }

        @Override
        public Instant read(JsonReader jsonReader) throws IOException {
            return Instant.parse(jsonReader.nextString());
        }
    }
}
