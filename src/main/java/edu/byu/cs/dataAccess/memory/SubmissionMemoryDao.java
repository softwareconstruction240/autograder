package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class SubmissionMemoryDao implements SubmissionDao {

    private final Deque<Submission> submissions = new LinkedList<>();

    @Override
    public void insertSubmission(Submission submission) {
        submissions.add(submission);
    }

    @Override
    public Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) {
        return submissions
                .stream()
                .filter(submission ->
                        submission.netId().equals(netId) && submission.phase().equals(phase))
                .toList();

    }

    @Override
    public Collection<Submission> getSubmissionsForUser(String netId) {
        return submissions
                .stream()
                .filter(submission ->
                        submission.netId().equals(netId))
                .toList();
    }

    @Override
    public Submission getLastSubmissionForUser(String netId) throws DataAccessException {
        Collection<Submission> submissions = getSubmissionsForUser(netId);
        Submission latest = null;
        long max = 0;
        for (Submission s : submissions) {
            if (s.passed() && s.timestamp().getEpochSecond() > max) latest = s;
        }
        return latest;
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions() {
        return getAllLatestSubmissions(-1);
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions(int batchSize) {
        HashMap<String, Submission> latestSubmissions = new HashMap<>();
        if (batchSize==0) return latestSubmissions.values();

        for (Submission submission : submissions) {
            String key = submission.netId() + submission.phase();
            latestSubmissions.compute(key, (k, v) -> {
                if (v == null || submission.timestamp().isAfter(v.timestamp())) {
                    return submission;
                } else {
                    return v;
                }
            });

            batchSize -= 1;
            if (batchSize == 0) { break; }
        }

        return latestSubmissions.values();
    }

    @Override
    public void removeSubmissionsByNetId(String netId, int daysOld) {
        submissions.removeIf(submission -> submission.netId().equals(netId) &&
                submission.timestamp().compareTo(Instant.now().minus(daysOld, ChronoUnit.DAYS)) < 0);
    }

    @Override
    public Submission getFirstPassingSubmission(String netId, Phase phase) {
        Collection<Submission> submissions = getSubmissionsForPhase(netId, phase);
        Submission earliest = null;
        long min = Long.MAX_VALUE;
        for (Submission s : submissions) {
            if (s.passed() && s.timestamp().getEpochSecond() < min) earliest = s;
        }
        return earliest;
    }

    @Override
    public Submission getBestSubmissionForPhase(String netId, Phase phase) {
        Collection<Submission> submissions = getSubmissionsForPhase(netId, phase);
        Submission bestSubmission =  null;
        for (Submission s : submissions) {
            if (bestSubmission == null) { bestSubmission = s; }
            else if (s.score() > bestSubmission.score()) { bestSubmission = s; }
        }
        return bestSubmission;
    }

    @Override
    public Collection<Submission> getAllPassingSubmissions(String netId) {
        return submissions
                .stream()
                .filter(submission -> submission.passed() && submission.netId().equals(netId))
                .collect(Collectors.toSet());
    }

    @Override
    public void manuallyApproveSubmission(Submission targetSubmission, Float newScore,
                                          Submission.ScoreVerification scoreVerification) throws ItemNotFoundException {
        if (targetSubmission == null) {
            throw new ItemNotFoundException("Target submission must not be null");
        }

        long matchingSubmissions = submissions.stream().filter(s -> s.equals(targetSubmission)).count();
        if (matchingSubmissions != 1) {
            throw new ItemNotFoundException("Did not isolate a single Submission "
                    + "based on the provided criteria. Found %d".formatted(matchingSubmissions));
        }

        // Search and replace the item in the deque
        Submission submission;
        Iterator<Submission> iterator = submissions.iterator();
        while (iterator.hasNext()) {
            submission = iterator.next();
            if (!targetSubmission.equals(submission)) continue;

            iterator.remove();
            submissions.add(submission.updateApproval(
                    newScore, Submission.VerifiedStatus.ApprovedManually, scoreVerification));
            return; // We found it!
        }

        throw new ItemNotFoundException("After verifying that 1 item existed, we didn't actually get to modify it");
    }

}
