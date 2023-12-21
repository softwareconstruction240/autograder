package edu.byu.cs.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.byu.cs.autograder.TestAnalyzer;

import java.io.IOException;
import java.time.Instant;

public record Submission(
        String netId,
        String repoUrl,
        String headHash,
        Instant timestamp,
        Phase phase,
        Float score,
        TestAnalyzer.TestNode testResults
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
