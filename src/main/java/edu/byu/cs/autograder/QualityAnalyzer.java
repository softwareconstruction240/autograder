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
        ProcessBuilder processBuilder = new ProcessBuilder().directory(stageRepo.getParentFile())
                .command("java", "-jar", checkStyleJarPath, "-c", "cs240_checks.xml", "repo");

        String output = ProcessUtils.runProcess(processBuilder);

        output = output.replaceAll(stageRepo.getAbsolutePath(), "");
        output = output.replaceAll(stageRepo.getPath(), "");

        QualityOutput qualityOutput = parseOutput(output);
        float score = getScore(qualityOutput);
        String results = getResults(qualityOutput);
        String notes = getNotes(qualityOutput);
        return new QualityAnalysis(score, results, notes);
    }


    private QualityOutput parseOutput(String output) {
        Map<String, List<String>> errors = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[ERROR]")) {
                String cause = trimmed.substring(trimmed.lastIndexOf('[') + 1, trimmed.lastIndexOf(']'));
                if (!errors.containsKey(cause)) {
                    errors.put(cause, new ArrayList<>());
                }
                errors.get(cause).add(trimmed);
            }
            if (trimmed.startsWith("[WARN]")) {
                warnings.add(trimmed);
            }
        }
        return new QualityOutput(errors, warnings);
    }

    private float getScore(QualityOutput output) {
        float totalPoints = 0;
        float earnedPoints = 0;
        for (QualityRubricCategory category : qualityRubricItems.categories()) {
            totalPoints += category.value();
            if(categoryPasses(category, output)) earnedPoints += category.value();
        }
        return earnedPoints / totalPoints;
    }

    private String getResults(QualityOutput output) {
        StringBuilder resultsBuilder = new StringBuilder();
        for (QualityRubricCategory category : qualityRubricItems.categories()) {
            StringBuilder categoryResultsBuilder = new StringBuilder();
            for(String reporter : category.reporters()) {
                StringBuilder reporterResultsBuilder = new StringBuilder();
                if (output.errors().containsKey(reporter)) {
                    output.errors().get(reporter).forEach(s -> reporterResultsBuilder.append("\t\t").append(s).append("\n"));
                }
                if (!reporterResultsBuilder.isEmpty()) {
                    categoryResultsBuilder.append("\t").append(reporter).append(":\n")
                            .append(reporterResultsBuilder);
                }
            }
            if (!categoryResultsBuilder.isEmpty()) {
                resultsBuilder.append(category.name()).append(":\n").append(categoryResultsBuilder);
            }
        }

        if(!output.warnings().isEmpty()) {
            resultsBuilder.append("Warnings:\n");
            output.warnings().forEach(s -> resultsBuilder.append("\t").append(s).append("\n"));
        }

        if (resultsBuilder.isEmpty()) resultsBuilder.append("Good job!");
        else resultsBuilder.deleteCharAt(resultsBuilder.length() - 1);
        return resultsBuilder.toString();
    }

    private String getNotes(QualityOutput output) {
        StringBuilder builder = new StringBuilder();
        for (QualityRubricCategory category : qualityRubricItems.categories()) {
            boolean categoryPasses = categoryPasses(category, output);
            builder.append((categoryPasses) ? "✓" : "✗").append(" ").append(category.name()).append("\n");
        }
        return builder.toString();
    }


    private boolean categoryPasses(QualityRubricCategory category, QualityOutput output) {
        for(String reporter : category.reporters()) {
            if(output.errors().containsKey(reporter)) return false;
        }
        return true;
    }


    public record QualityAnalysis(float score, String results, String notes) {}

    private record QualityOutput(Map<String, List<String>> errors, List<String> warnings) {}

    private record QualityRubricCategory(String name, float value, Set<String> reporters) {}

    private record QualityRubric(Set<QualityRubricCategory> categories) {}

}
