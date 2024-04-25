package edu.byu.cs.analytics;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
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
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
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
    public static CommitsByDay countCommitsByDay(
            Git git, @NonNull CommitThreshold lowerBound, @NonNull CommitThreshold upperBound)
            throws GitAPIException, IOException {

        // Prepare data for repeated calculation
        DiffFormatter diffFormatter = prepareDiffFormatter(git);
        Iterable<RevCommit> commits = getCommitsBetweenBounds(git, upperBound.commitHash(), lowerBound.commitHash());
        long lowerTimeBound = lowerBound.timestamp().getEpochSecond();
        long upperTimeBound = upperBound.timestamp().getEpochSecond();

        // Will hold results
        Map<String, Integer> days = new TreeMap<>();
        int singleParentCommits = 0;
        int mergeCommits = 0;
        List<Integer> changesPerCommit = new ArrayList<>();
        boolean commitsInOrder = true;
        boolean commitsInFuture = false;
        boolean commitsInPast = false;

        // Iteration helpers
        int commitTime;
        for (RevCommit rc : commits) {
            commitTime = rc.getCommitTime();
            if (commitTime <= lowerTimeBound) {
                commitsInPast = true;
                // Actually, we want to just skip these commits since these could legitimately
                // occur when rebasing or otherwise. No need to flag them as "suspicious histories."
                continue;
            }
            if (commitTime > upperTimeBound) {
                commitsInFuture = true;
            }

            for (var pc : rc.getParents()) {
                if (commitTime < pc.getCommitTime()) {
                    // Verifies that all parents are older than the child
                    commitsInOrder = false;
                    break;
                }
            }

            // Skip merge commits
            if (rc.getParentCount() > 1) {
                ++mergeCommits;
                continue;
            }

            // Count changes in each commit
            changesPerCommit.add(getNumChangesInCommit(diffFormatter, rc));

            // Add the commit to results
            String dayKey = DateTimeUtils.getDateString(commitTime, false);
            days.put(dayKey, days.getOrDefault(dayKey, 0) + 1);
            ++singleParentCommits;
        }
        return new CommitsByDay(
                days, changesPerCommit,
                singleParentCommits, mergeCommits,
                commitsInOrder, commitsInFuture, commitsInPast,
                lowerBound, upperBound);
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

    /**
     * Prepares a {@link DiffFormatter} for efficient use on multiple diffs later.
     * <br>
     * Note: This formatter is configured to ignore all white space differences,
     * and to not print out any output. This DiffFormatter is intended to be used
     * to analyze changes in commits programmatically.
     *
     * @param git The Git object to read.
     * @return A prepared {@link DiffFormatter}.
     */
    private static DiffFormatter prepareDiffFormatter(Git git) {
        DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
        diffFormatter.setRepository(git.getRepository());

        return diffFormatter;
    }
    /**
     * Uses a pre-prepared {@link DiffFormatter} to count the lines changed in a target commit, compared to it's first parent.
     *
     * @param diffFormatter A pre-prepared DiffFormatter.
     * @param revCommit The target commit to evaluate
     * @return An int representing the number of changed lines in the commit
     * @throws IOException When the system can't read the data properly.
     */
    private static int getNumChangesInCommit(DiffFormatter diffFormatter, RevCommit revCommit) throws IOException {
        int parentCount = revCommit.getParentCount();
        if (parentCount == 0) {
            return 0; // Root commit doesn't have any changes
        } else if (parentCount > 1) {
            throw new IllegalArgumentException("Cannot count changes in a merge commit.");
        }

        List<DiffEntry> diffs = diffFormatter.scan(revCommit.getParent(0), revCommit);

        int totalChanges = 0;
        for (var diff : diffs) {
            FileHeader fileHeader = diffFormatter.toFileHeader(diff);
            totalChanges += fileHeader.toEditList().stream().mapToInt(Edit::getLengthB).sum();
        }

        return totalChanges;
    }

    private record CommitDatum(
            String netId,
            Phase phase,
            int commits,
            int days,
            String section,
            String timestamp
    ) {}

    /**
     * generates a CSV-formatted string of all commit data
     * takes around 5 minutes to run for 300+ students
     *
     * @return a serialized version of the data
     */
    public static String generateCSV() throws CanvasException, DataAccessException {
        SubmissionDao submissionDao = DaoService.getSubmissionDao();

        Map<String, Map<String, ArrayList<Integer>>> commitInfo = compile();

        ArrayList<CommitDatum> csvData = new ArrayList<>();
        ArrayList<Phase> phases = new ArrayList<>();
        phases.add(Phase.Phase0);
        phases.add(Phase.Phase1);
        phases.add(Phase.Phase3);
        phases.add(Phase.Phase4);
        phases.add(Phase.Phase6);

        for (Map.Entry<String, Map<String, ArrayList<Integer>>> e : commitInfo.entrySet()) {
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
    private static Map<String, Map<String, ArrayList<Integer>>> compile() throws CanvasException {

        Map<String, Map<String, ArrayList<Integer>>> commitsBySection = new TreeMap<>();

        CanvasSection[] sections = CanvasService.getCanvasIntegration().getAllSections();
        for (CanvasSection section: sections) {

            Collection<User> students;
            Map<String, ArrayList<Integer>> commitMap = new TreeMap<>();

            try {
                students = CanvasService.getCanvasIntegration().getAllStudentsBySection(section.id());
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

            String sectionName = section.name();
            if(sectionName.matches("C S 240(-[0-9]+): Adv Software Construction")) {
                sectionName = String.valueOf(Integer.parseInt(sectionName.substring(8, sectionName.indexOf(':'))));
            }

            commitsBySection.put(sectionName, commitMap);
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
