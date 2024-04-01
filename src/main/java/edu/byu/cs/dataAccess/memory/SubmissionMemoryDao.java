package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.dataAccess.SubmissionHelper;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
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
    public void removeSubmissionsByNetId(String netId) {
        submissions.removeIf(submission -> submission.netId().equals(netId));
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
    public float getBestScoreForPhase(String netId, Phase phase) {
        Collection<Submission> submissions = getSubmissionsForPhase(netId, phase);
        float bestScore = 0;
        for (Submission s : submissions) {
            if (s.score() > bestScore) { bestScore = s.score(); }
        }
        return bestScore;
    }

    @Override
    public Collection<Submission> getAllPassingSubmissions(String netId) {
        return submissions
                .stream()
                .filter(submission -> submission.passed() && submission.netId().equals(netId))
                .collect(Collectors.toSet());
    }

    @Override
    public void manuallyApproveSubmission(Submission submission, Submission.ScoreVerification scoreVerification) throws ItemNotFoundException {
        throw new RuntimeException("Method not implemented");
    }

}
