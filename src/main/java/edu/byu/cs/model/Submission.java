package edu.byu.cs.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

/**
 * Note that the `numCommits` field has been deprecated and should be removed from tables.
 * It is no longer being populated with values.
 * */
public record Submission(
        String netId,
        String repoUrl,
        String headHash,
        Instant timestamp,
        Phase phase,
        Boolean passed,
        Float score,
        @Deprecated Integer numCommits,
        String notes,
        Rubric rubric
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
