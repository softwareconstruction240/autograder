package edu.byu.cs.autograder;

import com.google.gson.Gson;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.util.ProcessUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class QualityAnalyzer {

    /**
     * The path to the checkstyle jar
     */
    protected static final String checkStyleJarPath;
    private static final QualityRubric qualityRubricItems;

    static {
        Path libsPath = new File("phases", "libs").toPath();
        try {
            checkStyleJarPath = new File(libsPath.toFile(), "checkstyle-1.0.jar").getCanonicalPath();
            File qualityRubric = new File(libsPath.toFile(), "qualityRubric.json");
            qualityRubricItems = new Gson().fromJson(new FileReader(qualityRubric), QualityRubric.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        String out = new Gson().toJson(qualityRubricItems);
        System.out.println(out);
    }

    private Map<String, List<String>> errors;

    private List<String> warnings;


    public Rubric.Results runQualityChecks(File stageRepo) {
        ProcessBuilder processBuilder = new ProcessBuilder().directory(stageRepo)
                .command("java", "-jar", checkStyleJarPath, "-c", "cs240_checks.xml", "*");

        String output = ProcessUtils.runProcess(processBuilder);
        parseOutput(output);

        float score = getScore();
        String results = getResults();

        return new Rubric.Results("", score, null, results);
    }


    private void parseOutput(String output) {
        errors = new HashMap<>();
        warnings = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[ERROR]")) {
                String cause = trimmed.substring(trimmed.lastIndexOf('['));
                if (!errors.containsKey(cause)) {
                    errors.put(cause, new ArrayList<>());
                }
                errors.get(cause).add(trimmed);
            }
            if (trimmed.startsWith("[WARN]")) {
                warnings.add(trimmed);
            }
        }
    }


    private float getScore() {
        float points = 0;
        float deductions = 0;
        for(QualityRubricCategory category : qualityRubricItems.categories()) {
            points += category.value();
            float itemWeight = category.value() / category.items().size();
            for(QualityRubricItem item : category.items()) {
                float reporterWeight = itemWeight / item.reporters().size();
                for(String reporter : item.reporters()) {
                    if(errors.containsKey(reporter)) {
                        deductions += reporterWeight;
                    }
                }
            }
        }
        return 1 - (deductions / points);
    }


    private String getResults() {
        StringBuilder builder = new StringBuilder();
        errors.values().forEach(errorList -> errorList.forEach(s -> builder.append(s).append("\n")));
        warnings.forEach(s -> builder.append(s).append("\n"));
        if(!builder.isEmpty()) builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    private record QualityRubricItem(Set<String> reporters) {}
    private record QualityRubricCategory(String name, float value, Set<QualityRubricItem> items) {}
    private record QualityRubric(Set<QualityRubricCategory> categories) {}

}
