package edu.byu.cs.analytics;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.model.User;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 * Analyzes the commit history of every student with a GitHub repo URL submission
 */
public class CommitAnalytics {

    /**
     * Instantiate data structures and compile analytics
     */
    public CommitAnalytics() {
        invalidRepos = new HashSet<>();
        allStudents = new HashMap<>();
        commitInfo = compile();
    }

    /**
     * A set of users whose repos are unable to be cloned
     */
    private final Set<User> invalidRepos;

    /**
     * A map of netID to User for quick reference
     */
    private final Map<String, User> allStudents;

    /**
     * A map of netID to map of day (represented by a string yyyy-mm-dd) to integer
     */
    private final Map<String, Map<String, Integer>> commitInfo;

    public Set<User> getInvalidRepos() {
        return invalidRepos;
    }

    public Map<String, Map<String, Integer>> getCommitInfo() {
        return commitInfo;
    }

    /**
     * Compiles git commit analytics for every student
     *
     * @return A map of netID to map of day (represented by a string yyyy-mm-dd) to integer
     */
    private Map<String, Map<String, Integer>> compile() {

        Set<User> students;
        Map<String, Map<String, Integer>> commitMap = new TreeMap<>();

        try {
            students = CanvasIntegration.getAllStudents();
        } catch (CanvasException e) {
            throw new RuntimeException("Canvas Exception: " + e.getMessage());
        }

        for (User student : students) {
            allStudents.put(student.netId(), student);
            File repoPath = new File("./tmp-" + student.repoUrl().hashCode());

            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(student.repoUrl())
                    .setDirectory(repoPath);

            try (Git git = cloneCommand.call()) {
                Iterable<RevCommit> commits = git.log().all().call();
                Map<String, Integer> count = handleCommits(commits, 0);
                commitMap.put(student.netId(), count);
            } catch (GitAPIException | IOException e) {
                invalidRepos.add(student);
            }

            removeTemp(repoPath);
        }

        return commitMap;
    }

    /**
     * Given an iterable of commits and a timestamp, compiles stats since that timestamp
     *
     * @param commits the collection of commits
     * @param seconds the lower bound timestamp in Unix seconds
     * @return the map
     */
    public static Map<String, Integer> handleCommits(Iterable<RevCommit> commits, long seconds) {
        Map<String, Integer> days = new TreeMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (RevCommit rc : commits) {
            if (rc.getCommitTime() < seconds) continue;
            Date date = new Date(rc.getCommitTime() * 1000L);
            String dayKey = dateFormat.format(date);
            days.put(dayKey, days.getOrDefault(dayKey, 0) + 1);
        }
        return days;
    }

    /**
     * Counts the total commits in the given map
     *
     * @param days a map of day represented as "yyyy-mm-dd" to integer
     * @return the total of the map's values
     */
    public static int getTotalCommits(Map<String, Integer> days) {
        int total = 0;
        for (Map.Entry<String, Integer> entry : days.entrySet()) {
            total += entry.getValue();
        }
        return total;
    }

    /**
     * Generates a report as a String based on all data in this class, almost like a custom toString()
     *
     * @param simplified whether this output is more robust or not
     * @return the report as a String
     */
    public String generateReport(boolean simplified) {
        if (simplified) return generateSimpleReport();
        String indent = "  ";
        StringBuilder sb = new StringBuilder();
        sb.append("Students with invalid repo URLs:\n");
        for (User u : invalidRepos) {
            sb.append(indent).append(printUserDetails(u)).append("\n");
        }
        sb.append("\n");
        sb.append("Average: ").append(getAverageCommits()).append(" commits across ").append(getAverageDays()).append(" days\n\n");
        sb.append("COMMIT DATA:\n");
        for (Map.Entry<String, Map<String, Integer>> entry : commitInfo.entrySet()) {
            sb.append(printUserDetails(allStudents.get(entry.getKey()))).append("\n");
            sb.append(indent).append("Total commits: ").append(getTotalCommits(entry.getValue())).append(" in ")
                    .append(entry.getValue().size()).append(" day(s). Breakdown:\n");
            for (Map.Entry<String, Integer> days : entry.getValue().entrySet()) {
                sb.append(indent).append(indent).append(days.getKey()).append(" -> ").append(days.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    private String generateSimpleReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Average: ").append(getAverageCommits()).append(" commits across ").append(getAverageDays()).append(" days\n\n");
        sb.append("COMMIT DATA:\n\n");
        for (Map.Entry<String, Map<String, Integer>> entry : commitInfo.entrySet()) {
            sb.append(printUserDetails(allStudents.get(entry.getKey())));
            sb.append("\n  Total commits: ").append(getTotalCommits(entry.getValue())).append(" in ")
                    .append(entry.getValue().size()).append(" day(s).\n\n");
        }
        return sb.toString();
    }

    private void removeTemp(File dir) {
        if (!dir.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete directory: " + e.getMessage());
        }
    }

    private String printUserDetails(User user) {
        return user.netId() + " (" + user.firstName() + " " + user.lastName() + ") " + user.repoUrl();
    }

    private double getAverageCommits() {
        int total = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : commitInfo.entrySet()) {
            for (Map.Entry<String, Integer> days1 : entry.getValue().entrySet()) {
                total += days1.getValue();
            }
        }
        return roundToDecimal((double) total / commitInfo.size(), 1);
    }

    private double getAverageDays() {
        int total = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : commitInfo.entrySet()) {
            total += entry.getValue().size();
        }
        return roundToDecimal((double) total / commitInfo.size(), 1);
    }

    private double roundToDecimal(double value, int decimalPlaces) {
        String pattern = "#." + "0".repeat(decimalPlaces);
        DecimalFormat decimalFormat = new DecimalFormat(pattern);
        String formattedValue = decimalFormat.format(value);
        return Double.parseDouble(formattedValue);
    }
}
