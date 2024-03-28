package edu.byu.cs.analytics;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import edu.byu.cs.util.DateTimeUtils;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.util.*;

/**
 * Analyzes the commit history of every student with a GitHub repo URL submission
 */
public class CommitAnalytics {

    /**
     * Given an iterable of commits and two timestamps, creates a map of day to number of commits on that day,
     * counting only commits within the bounds presented.
     *
     * @param git An open git object for use
     * @param lowerBound The last commit in this log, exclusive
     * @param upperBound The first commit in this log, inclusive
     * @return A {@link CommitsByDay} record with the results
     */
    public static CommitsByDay countCommitsByDay(Git git, CommitThreshold lowerBound, CommitThreshold upperBound)
            throws GitAPIException, IncorrectObjectTypeException, MissingObjectException {

        // Prepare data for repeated calculation
        Iterable<RevCommit> commits = getCommitsBetweenBounds(git, lowerBound.commitHash(), upperBound.commitHash());
        long lowerTimeBound = lowerBound.timestamp().getEpochSecond();
        long upperTimeBound = upperBound.timestamp().getEpochSecond();

        // Will hold results
        Map<String, Integer> days = new TreeMap<>();
        int totalCommits = 0;
        boolean commitsInOrder = true;
        boolean commitsInFuture = false;

        // Iteration helpers
        int commitTime;
        for (RevCommit rc : commits) {
            commitTime = rc.getCommitTime();
            if (commitTime <= lowerTimeBound) {
                continue;
            }

            if (commitTime > upperTimeBound) {
                commitsInFuture = true;
            }
            if (commitTime < rc.getParent(1).getCommitTime()) {
                commitsInOrder = false;
            }

            // Add the commit to results
            String dayKey = DateTimeUtils.getDateString(commitTime, false);
            days.put(dayKey, days.getOrDefault(dayKey, 0) + 1);
            totalCommits += 1;
        }
        return new CommitsByDay(days, totalCommits, commitsInOrder, commitsInFuture, lowerBound, upperBound);
    }
    private static Iterable<RevCommit> getCommitsBetweenBounds(
            Git git, @NonNull String headHash, @Nullable String tailHash)
            throws IncorrectObjectTypeException, MissingObjectException, GitAPIException {
        if (git == null || headHash == null) {
            throw new RuntimeException("Git and headHash are both required parameters.");
        }

        ObjectId headObjId = ObjectId.fromString(headHash);

        if (tailHash == null) {
            return git.log().add(headObjId).call();
        } else {
            ObjectId tailObjId = ObjectId.fromString(tailHash);
            return git.log().addRange(tailObjId, headObjId).call();
        }
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

        Map<Integer, Integer> sectionIDs = CanvasIntegrationImpl.sectionIDs;
        for (Map.Entry<Integer, Integer> i : sectionIDs.entrySet()) {

            Collection<User> students;
            Map<String, ArrayList<Integer>> commitMap = new TreeMap<>();

            try {
                students = CanvasIntegration.getCanvasIntegration().getAllStudentsBySection(i.getValue());
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
