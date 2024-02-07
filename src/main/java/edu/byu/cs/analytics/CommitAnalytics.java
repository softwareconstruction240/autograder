package edu.byu.cs.analytics;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import edu.byu.cs.util.DateTimeUtils;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.util.*;

/**
 * Analyzes the commit history of every student with a GitHub repo URL submission
 */
public class CommitAnalytics {

    /**
     * Given an iterable of commits and two timestamps, creates a map of day to number of commits on that day
     *
     * @param commits the collection of commits
     * @param lowerBound the lower bound timestamp in Unix seconds
     * @param upperBound the upper bound timestamp in Unix seconds
     * @return the map
     */
    public static Map<String, Integer> handleCommits(Iterable<RevCommit> commits, long lowerBound, long upperBound) {
        Map<String, Integer> days = new TreeMap<>();
        for (RevCommit rc : commits) {
            if (rc.getCommitTime() < lowerBound || rc.getCommitTime() > upperBound) continue;
            String dayKey = DateTimeUtils.getDateString(rc.getCommitTime(), false);
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

    private record CommitDatum(
            String netId,
            Phase phase,
            int commits,
            int days,
            int section,
            String timestamp
    ) {}

    /**
     * generates a CSV-formatted string of all commit data
     * takes around 5 minutes to run for 300+ students
     *
     * @return a serialized version of the data
     */
    public static String generateCSV() {

        SubmissionDao submissionDao = DaoService.getSubmissionDao();

        Map<Integer, Map<String, ArrayList<Integer>>> commitInfo = compile();

        ArrayList<CommitDatum> csvData = new ArrayList<>();
        ArrayList<Phase> phases = new ArrayList<>();
        phases.add(Phase.Phase0);
        phases.add(Phase.Phase1);
        phases.add(Phase.Phase3);
        phases.add(Phase.Phase4);
        phases.add(Phase.Phase6);

        for (Map.Entry<Integer, Map<String, ArrayList<Integer>>> e : commitInfo.entrySet()) {
            for (Map.Entry<String, ArrayList<Integer>> entry : e.getValue().entrySet()) {
                String netID = entry.getKey();
                for (Phase phase : phases) {
                    Submission submission = submissionDao.getFirstPassingSubmission(entry.getKey(), phase);
                    if (submission == null) break;
                    Phase prevPhase = PhaseUtils.getPreviousPhase(phase);

                    long lowerBound = 0;
                    if (prevPhase != null) {
                        Submission prevSubmission = submissionDao.getFirstPassingSubmission(netID, prevPhase);
                        if (prevSubmission != null) { // it should never be null due to passoff order enforcement
                            lowerBound = prevSubmission.timestamp().getEpochSecond();
                        }
                    }
                    long upperBound = submission.timestamp().getEpochSecond();
                    ArrayList<Integer> chunk = getChunkOfTimestamps(entry.getValue(), lowerBound, upperBound);

                    CommitDatum row = new CommitDatum(netID, phase, chunk.size(), getNumDaysFromChunk(chunk),
                            e.getKey(), DateTimeUtils.getDateString(submission.timestamp().getEpochSecond(), true));

                    csvData.add(row);
                }

            }
        }
        return serializeDataToCSV(csvData);
    }

    /**
     * Compiles git commit analytics for every student
     *
     * @return A map section to map of netID to list of timestamp
     */
    private static Map<Integer, Map<String, ArrayList<Integer>>> compile() {

        Map<Integer, Map<String, ArrayList<Integer>>> commitsBySection = new TreeMap<>();

        Map<Integer, Integer> sectionIDs = CanvasIntegration.sectionIDs;
        for (Map.Entry<Integer, Integer> i : sectionIDs.entrySet()) {

            Collection<User> students;
            Map<String, ArrayList<Integer>> commitMap = new TreeMap<>();

            try {
                students = CanvasIntegration.getAllStudentsBySection(i.getValue());
            } catch (CanvasException e) {
                throw new RuntimeException("Canvas Exception: " + e.getMessage());
            }

            for (User student : students) {
                File repoPath = new File("./tmp-" + student.repoUrl().hashCode());

                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(student.repoUrl())
                        .setDirectory(repoPath);

                try (Git git = cloneCommand.call()) {
                    Iterable<RevCommit> commits = git.log().all().call();
                    ArrayList<Integer> timestamps = getAllTimestamps(commits);
                    commitMap.put(student.netId(), timestamps);
                } catch (Exception e) {
                    FileUtils.removeDirectory(repoPath);
                    continue;
                }

                FileUtils.removeDirectory(repoPath);
            }

            commitsBySection.put(i.getKey(), commitMap);
        }

        return commitsBySection;
    }

    private static String serializeDataToCSV(ArrayList<CommitDatum> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("netID,phase,numCommits,numDays,section,timestamp\n");
        for (CommitDatum cd : data) {
            sb.append(cd.netId).append(",").append(PhaseUtils.getPhaseAsString(cd.phase)).append(",")
                    .append(cd.commits).append(",").append(cd.days).append(",")
                    .append(cd.section).append(",").append(cd.timestamp).append("\n");
        }
        return sb.toString();
    }

    private static ArrayList<Integer> getChunkOfTimestamps(ArrayList<Integer> timestamps, long lowerBound, long upperBound) {
        ArrayList<Integer> chunk = new ArrayList<>();
        for (Integer ts : timestamps) {
            if (ts > lowerBound && ts <= upperBound) chunk.add(ts);
        }
        return chunk;
    }

    private static int getNumDaysFromChunk(ArrayList<Integer> timestamps) {
        Set<String> days = new HashSet<>();
        for (Integer ts : timestamps) {
            days.add(DateTimeUtils.getDateString(ts, false));
        }
        return days.size();
    }

    private static ArrayList<Integer> getAllTimestamps(Iterable<RevCommit> commits) {
        ArrayList<Integer> timestamps = new ArrayList<>();
        for (RevCommit rc : commits) timestamps.add(rc.getCommitTime());
        return timestamps;
    }
}
