package edu.byu.cs.analytics;

import edu.byu.cs.autograder.git.CommitsBetweenBounds;
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
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
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
     * @param excludeCommits A non-null set of commit hashes which will be skipped during analysis
     * @return A {@link CommitsByDay} record with the results
     */
    public static CommitsByDay countCommitsByDay(
            Git git, @NonNull CommitThreshold lowerBound, @NonNull CommitThreshold upperBound,
            Set<String> excludeCommits
    )
            throws GitAPIException, IOException {

        // Verify arguments
        if (git == null) {
            throw new RuntimeException("The git parameter cannot be null");
        }
        if (lowerBound == null || upperBound == null) {
            throw new IllegalArgumentException("Both bounds must not be null");
        }

        // Prepare data for repeated calculation
        DiffFormatter diffFormatter = prepareDiffFormatter(git);
        CommitsBetweenBounds commitsBetweenBounds = getCommitsBetweenBounds(git, upperBound.commitHash(), lowerBound.commitHash());
        long lowerTimeBoundSecs = lowerBound.timestamp().getEpochSecond();
        long upperTimeBoundSecs = upperBound.timestamp().getEpochSecond();

        // Will hold results
        Map<String, Integer> days = new TreeMap<>();
        int singleParentCommits = 0;
        int mergeCommits = 0;
        List<Integer> changesPerCommit = new LinkedList<>();
        Map<String, List<String>> erroringCommits = new HashMap<>();
        boolean commitsInOrder = true;
        boolean commitsInFuture = false;
        boolean commitsInPast = false;
        boolean commitsBackdated = false;

        Map<Long, List<String>> commitsByTimestamp = new HashMap<>();
        boolean commitsWithSameTimestamp = false;

        boolean missingTailHash = commitsBetweenBounds.missingTail();
        if (missingTailHash) {
            groupCommitsByKey(erroringCommits, "missingTailHash", lowerBound.commitHash());
        }

        // Iteration helpers
        CommitTimestamps commitTimes;
        String commitHash;
        for (RevCommit rc : commitsBetweenBounds.commits()) {
            commitHash = rc.getName();
            if (excludeCommits.contains(commitHash)) {
                groupCommitsByKey(erroringCommits, "excludedCommits", commitHash);
                continue;
            }

            commitTimes = getCommitTime(rc);
            if (commitTimes.seconds <= lowerTimeBoundSecs) {
                groupCommitsByKey(erroringCommits, "commitsInPast", commitHash);
                commitsInPast = true;
                // Actually, we want to just skip these commits since these could
                // legitimately occur when rebasing or otherwise.
                continue;
            }
            if (commitTimes.seconds > upperTimeBoundSecs) {
                groupCommitsByKey(erroringCommits, "commitsInFuture", commitHash);
                commitsInFuture = true;
            }

            for (var pc : getCommitParents(git, rc)) {
                if (commitTimes.seconds < getCommitTime(pc).seconds) {
                    // Verifies that all parents are older than the child
                    groupCommitsByKey(erroringCommits, "commitsInOrder", commitHash);
                    commitsInOrder = false;
                    break;
                }
            }

            // Skip merge commits
            if (rc.getParentCount() > 1) {
                ++mergeCommits;
                continue;
            }

            if (detectCommitBackdating(commitTimes)) {
                groupCommitsByKey(erroringCommits, "commitsBackdated", commitHash);
                commitsBackdated = true;
            }

            // Count changes in each commit
            changesPerCommit.add(getNumChangesInCommit(diffFormatter, rc));
            groupCommitsByKey(commitsByTimestamp, commitTimes.seconds, commitHash);

            // Add the commit to results
            String dayKey = DateTimeUtils.getDateString(commitTimes.seconds, false);
            days.put(dayKey, days.getOrDefault(dayKey, 0) + 1);
            ++singleParentCommits;
        }

        // Check for multiple commits with the same timestamp
        var duplicatedTimestampCommits = analyzeDuplicatedTimestamps(commitsByTimestamp);
        if (!duplicatedTimestampCommits.isEmpty()) {
            commitsWithSameTimestamp = true;
            erroringCommits.put("commitTimestampsDuplicated", duplicatedTimestampCommits.allEffectedCommits());
            erroringCommits.put("commitTimestampsDuplicatedSubsequentOnly", duplicatedTimestampCommits.duplicatedCommitsOnly());
        }

        return new CommitsByDay(
                days, changesPerCommit, erroringCommits,
                singleParentCommits, mergeCommits,
                commitsInOrder, commitsInFuture, commitsInPast, commitsBackdated, commitsWithSameTimestamp, missingTailHash,
                lowerBound, upperBound);
    }

    private static <T> void groupCommitsByKey(Map<T, List<String>> dataMap, T groupId, String commitHash) {
        dataMap.putIfAbsent(groupId, new LinkedList<>());
        dataMap.get(groupId).add(commitHash);
    }
    private static DuplicatedTimestamps analyzeDuplicatedTimestamps(Map<Long, List<String>> dataMap) {
        List<String> allEffectedCommits = new ArrayList<>();
        List<String> duplicatedCommitsOnly = new ArrayList<>();
        for (var commitsAtTimestamp : dataMap.values()) {
            if (commitsAtTimestamp.size() > 1) {
                allEffectedCommits.addAll(commitsAtTimestamp);
                duplicatedCommitsOnly.addAll(commitsAtTimestamp.subList(0, commitsAtTimestamp.size()-1));
            }
        }
        return new DuplicatedTimestamps(allEffectedCommits, duplicatedCommitsOnly);
    }

    /**
     * Contains an analyzed view of the commits which have been discovered to have the exact same timestamp.
     *
     * @param allEffectedCommits The commit hashes of all affected commits.
     *                           This set of results could be displayed to a user for understanding the problem.
     * @param duplicatedCommitsOnly The commit hashes of only the duplicating commits.
     *                              The first commit of each bucket of duplicated timestamps is excluded.
     *                              This set of results could be used to identify commits to skip in a re-evaluation
     *                              of the repo while still honoring the first of each of the commits.
     */
    private record DuplicatedTimestamps(
            List<String> allEffectedCommits,
            List<String> duplicatedCommitsOnly
    ) {
        public boolean isEmpty() {
            return allEffectedCommits.isEmpty();
        }
    }


    /**
     * Returns the parents of the specified commit with a buffer included for detailed parsing.
     * If the data isn't immediately available, it will be freshly retrieved from the repo.
     * <br>
     * This method works even when the parents of the commit were marked as "uninteresting" during
     * initial processing. When this happens, the library includes the parents but excludes
     * all buffer data which causes a NPE when attempting to access their authorship data.
     *
     * @param git The Git repo being processed
     * @param commit An existing {@link RevCommit} that will be traversed.
     * @return An array of RevCommits representing all parents, or an empty array.
     * @throws IOException When Jgit has an issue reading the disk.
     */
    private static RevCommit[] getCommitParents(Git git, RevCommit commit) throws IOException {
        // If the parents are already available, return them. Otherwise, fetch them anew from Git with a RevWalk
        var allParentsHaveBuffer = true;
        var parents = commit.getParents();
        for (var parent : parents) {
            allParentsHaveBuffer &= parent.getRawBuffer() != null;
        }

        if (allParentsHaveBuffer) return parents;

        // Replace existing parent commit objects with freshly loaded objects from the repo
        // RevWalk.parseCommit is expensive and can be used multiple times, but only once per commit.
        // That is sufficient for our purposes here.
        // Generally speaking, this code will only on run the first commit authored after a passing submission.
        var revWalk = new RevWalk(git.getRepository());
        for (int i = 0; i < parents.length; ++i) {
            if (parents[i].getRawBuffer() != null) continue;
            parents[i] = revWalk.parseCommit(parents[i].toObjectId());
        }
        return parents;
    }

    /**
     * Responsible for providing an iterable of the commits to analyze for this phase,
     * along with some status flags indicating how they were retrieved.
     * <br>
     * Generally, this will result in only the new commits since the last submission being evaluated.
     * However, if the previous submission commit is missing, or if this is the first submission,
     * then the entire history will be provided.
     * <br>
     * If a tail commit was expected, but not found, that will be reported as a flag.
     *
     * @param git An open Git repo
     * @param headHash The current head hash to evaluate.
     * @param tailHash The previous submission head hash, if any.
     * @return {@link CommitsBetweenBounds} containing the iterable of commits and other flags.
     * @throws IncorrectObjectTypeException When the GitAPI is used incorrectly.
     * @throws MissingObjectException When the ead hash cannot be found. If the tailhash cannot be found,
     * the entire history will be evaluated and the issue flagged.
     * @throws GitAPIException When the GitAPI has an issue
     */
    private static CommitsBetweenBounds getCommitsBetweenBounds(
            Git git, @NonNull String headHash, @Nullable String tailHash)
            throws IncorrectObjectTypeException, MissingObjectException, GitAPIException {
        if (git == null || headHash == null) {
            throw new RuntimeException("Git and headHash are both required parameters.");
        }

        ObjectId headObjId = ObjectId.fromString(headHash);

        if (tailHash == null) {
            return produceCommitIterable(git, headObjId, false);
        } else {
            ObjectId tailObjId = ObjectId.fromString(tailHash);
            return produceCommitIterable(git, headObjId, tailObjId);
        }
    }

    private static CommitsBetweenBounds produceCommitIterable(Git git, ObjectId headObjId, boolean missingTail)
            throws IncorrectObjectTypeException, MissingObjectException, GitAPIException {
        return new CommitsBetweenBounds(git.log().add(headObjId).call(), missingTail);
    }
    private static CommitsBetweenBounds produceCommitIterable(Git git, ObjectId headObjId, ObjectId tailObjId)
            throws IncorrectObjectTypeException, MissingObjectException, GitAPIException {
        try {
            return new CommitsBetweenBounds(git.log().addRange(tailObjId, headObjId).call(), false);
        } catch (MissingObjectException missingObjectException) {
            return produceCommitIterable(git, headObjId, true);
        }
    }

    /**
     * Returns the authorship time of the provided commit in seconds.
     * <br>
     * Note that the <b>authorship time</b> differs from the <b>commit time</b>
     * in cases where the commit is amended or changed after original authorship.
     * For example, if a commit is cherry-picked, rebased, or amended.
     * <br>
     * Note that it is relatively easy to change the author date of commits,
     * compared to changing the commit date for an experienced user. However, it makes the most sense to
     * monitor the author date because the automated tools preserve these generally.
     *
     * @param revCommit The commit to analyze
     * @return A long representing the author time in seconds.
     */
    private static CommitTimestamps getCommitTime(RevCommit revCommit) {
        long commitTimestamp = revCommit.getCommitTime();
        long bestTimestamp = commitTimestamp;

        long authorTimestamp = -1;
        PersonIdent authorIdent = revCommit.getAuthorIdent();
        if (authorIdent != null) {
            Date authorTime = authorIdent.getWhen();
            if (authorTime != null) {
                authorTimestamp = authorTime.getTime() / 1000;
                bestTimestamp = authorTimestamp;
            }
        }

        return new CommitTimestamps(commitTimestamp, authorTimestamp, bestTimestamp);
    }

    private static final long secondsInDay = 60*60*24;

    /**
     * Performs several checks to determine if the commits may have been manually backdated (not allowed).
     *
     * @param timestamps The CommitTimestamps of the commit
     * @return A boolean indicating if backdating was detected
     */
    private static boolean detectCommitBackdating(CommitTimestamps timestamps) {
        if (timestamps.author == -1) {
            // CONSIDER: Throwing an error instead if the author timestamp isn't available?
            return false;
        }

        // Detect if the author timestamp is after than the commit timestamp
        if (timestamps.author > timestamps.commit) {
            return true;
        }

        // When git backdates commits, it uses the time part as the commit timestamp.
        // Detect if the commit and author timestamps are separated exactly by a multiple of days.
        if ((timestamps.commit != timestamps.author) && (timestamps.commit - timestamps.author) % secondsInDay == 0) {
            return true;
        }

        return false; // No backdating detected
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
        RevCommit comparisonCommit;
        int parentCount = revCommit.getParentCount();
        if (parentCount == 1) {
            comparisonCommit = revCommit.getParent(0);
        } else if (parentCount == 0) {
            comparisonCommit = null; // Root commits can still have changes
        } else {
            throw new IllegalArgumentException("Cannot count changes in a merge commit.");
        }

        List<DiffEntry> diffs = diffFormatter.scan(comparisonCommit, revCommit);

        int totalChanges = 0;
        for (var diff : diffs) {
            FileHeader fileHeader = diffFormatter.toFileHeader(diff);
            for (var edit : fileHeader.toEditList()) {
                // https://archive.eclipse.org/jgit/docs/jgit-2.0.0.201206130900-r/apidocs/org/eclipse/jgit/diff/Edit.html
                totalChanges += edit.getLengthA() + edit.getLengthB();
            }
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
     * Represents the timestamps relating to a commit.
     * <br>
     * Both values are `long`'s representing the number of seconds since the epoch.
     *
     * @param commit Represents the commit timestamp. Will always exist.
     * @param author Represents the authorship timestamp, or -1 if it cannot be extracted
     * @param seconds Represents the author timestamp, or the commit timestamp if not available.
     */
    private record CommitTimestamps(
         long commit,
         long author,
         long seconds
    ) { }

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
