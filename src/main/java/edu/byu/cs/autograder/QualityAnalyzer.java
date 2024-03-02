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


    public QualityOutput runQualityChecks(File stageRepo) {
        ProcessBuilder processBuilder = new ProcessBuilder().directory(stageRepo.getParentFile())
                .command("java", "-jar", checkStyleJarPath, "-c", "cs240_checks.xml", "repo");

        String output = ProcessUtils.runProcess(processBuilder);

        output = output.replaceAll(stageRepo.getAbsolutePath(), "");
        output = output.replaceAll(stageRepo.getPath(), "");

        QualityAnalysis analysis = parseAnalysis(output);
        float score = getScore(analysis);
        String results = getResults(analysis);
        String notes = getNotes(analysis);
        return new QualityOutput(score, results, notes);
    }


    private QualityAnalysis parseAnalysis(String output) {
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

    private float getScore(QualityAnalysis analysis) {
        float totalPoints = 0;
        float earnedPoints = 0;
        for (QualityRubricCategory category : qualityRubricItems.categories()) {
            totalPoints += category.value();
            earnedPoints += getCategoryPoints(analysis, category);
        }
        return earnedPoints / totalPoints;
    }

    private String getResults(QualityAnalysis analysis) {
        StringBuilder resultsBuilder = new StringBuilder();
        for (QualityRubricCategory category : qualityRubricItems.categories()) {
            StringBuilder categoryResultsBuilder = new StringBuilder();
            for (QualityRubricItem item : category.items()) {
                for (QualityRubricReporter reporter : item.reporters()) {
                    StringBuilder reporterResultsBuilder = new StringBuilder();
                    if (analysis.errors().containsKey(reporter.name())) {
                        analysis.errors().get(reporter.name())
                                .forEach(s -> reporterResultsBuilder.append("\t\t").append(s).append("\n"));
                    }
                    if (!reporterResultsBuilder.isEmpty()) {
                        categoryResultsBuilder.append("\t").append(reporter.name()).append(":\n")
                                .append(reporterResultsBuilder);
                    }
                }
            }
            if (!categoryResultsBuilder.isEmpty()) {
                resultsBuilder.append(category.name()).append(":\n").append(categoryResultsBuilder);
            }
        }

        if (resultsBuilder.isEmpty()) resultsBuilder.append("Good job!");
        else resultsBuilder.deleteCharAt(resultsBuilder.length() - 1);
        return resultsBuilder.toString();
    }

    private String getNotes(QualityAnalysis analysis) {
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
        float totalWeight = 0;
        for (QualityRubricItem item : category.items()) {
            for (QualityRubricReporter reporter : item.reporters()) {
                float weight = item.weight() * reporter.weight();
                totalWeight += weight;
                if (analysis.errors().containsKey(reporter.name())) {
                    deductions += weight;
                }
            }
        }
        return category.value() * (1 - (deductions / totalWeight));
    }


    public record QualityOutput(float score, String results, String notes) {}

    private record QualityAnalysis(Map<String, List<String>> errors, List<String> warnings) {}

    private record QualityRubricReporter(String name, int weight) {}

    private record QualityRubricItem(Set<QualityRubricReporter> reporters, int weight) {}

    private record QualityRubricCategory(String name, float value, Set<QualityRubricItem> items) {}

    private record QualityRubric(Set<QualityRubricCategory> categories) {}

}
