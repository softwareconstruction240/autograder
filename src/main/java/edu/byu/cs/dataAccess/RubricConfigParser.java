package edu.byu.cs.dataAccess;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class RubricConfigParser {

    public static Collection<RubricConfigObject> loadRubricConfigFile() throws FileNotFoundException {
        JsonReader reader;
        reader = new JsonReader(new FileReader("src/main/resources/rubric-config.json"));

        RubricConfigFileData fileData = new Gson().fromJson(reader, RubricConfigFileData.class);
        return fileData.items();
    }

    record RubricConfigFileData(ArrayList<RubricConfigObject> items) {
    }

    public record RubricConfigObject(Phase phase, Rubric.RubricType type, String category, String criteria, int points) {
    }
}
