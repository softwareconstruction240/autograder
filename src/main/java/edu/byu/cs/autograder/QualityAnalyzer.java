package edu.byu.cs.autograder;

import com.google.gson.Gson;
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


    public QualityAnalysis runQualityChecks(File stageRepo) {
        ProcessBuilder processBuilder = new ProcessBuilder().directory(stageRepo)
                .command("java", "-jar", checkStyleJarPath, "-c", "cs240_checks.xml", "./");

        String output = ProcessUtils.runProcess(processBuilder);
        return parseOutput(output);
    }


    private QualityAnalysis parseOutput(String output) {
        Map<String, List<String>> errors = new HashMap<>();
        List<String> warnings = new ArrayList<>();
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
        return new QualityAnalysis(errors, warnings);
    }


    public float getScore(QualityAnalysis analysis) {
        float totalPoints = 0;
        float earnedPoints = 0;
        for (QualityRubricCategory category : qualityRubricItems.categories()) {
            totalPoints += category.value();
            earnedPoints += getCategoryPoints(analysis, category);
        }
        return earnedPoints / totalPoints;
    }


    public String getResults(QualityAnalysis analysis) {
        StringBuilder builder = new StringBuilder();
        analysis.errors().values().forEach(errorList -> errorList.forEach(s -> builder.append(s).append("\n")));
        analysis.warnings().forEach(s -> builder.append(s).append("\n"));
        if (!builder.isEmpty()) builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }


    public String getNotes(QualityAnalysis analysis) {
        StringBuilder builder = new StringBuilder();
        for (QualityRubricCategory category : qualityRubricItems.categories()) {
            float categoryPoints = getCategoryPoints(analysis, category);
            builder.append(category.name()).append(": ").append(categoryPoints).append("/").append(category.value())
                    .append("\n");
        }
        return builder.toString();
    }


    private float getCategoryPoints(QualityAnalysis analysis, QualityRubricCategory category) {
        float deductions = 0;
        float itemWeight = category.value() / category.items().size();
        for (QualityRubricItem item : category.items()) {
            float reporterWeight = itemWeight / item.reporters().size();
            for (String reporter : item.reporters()) {
                if (analysis.errors().containsKey(reporter)) {
                    deductions += reporterWeight;
                }
            }
        }
        return category.value() - deductions;
    }


    public record QualityAnalysis(Map<String, List<String>> errors, List<String> warnings) {}

    private record QualityRubricItem(Set<String> reporters) {}

    private record QualityRubricCategory(String name, float value, Set<QualityRubricItem> items) {}

    private record QualityRubric(Set<QualityRubricCategory> categories) {}

}
