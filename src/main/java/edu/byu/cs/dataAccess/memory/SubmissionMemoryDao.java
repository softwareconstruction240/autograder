package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

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
        float bestScore = -1.0f; // This implementation **can** differentiate between submissions and no submissions
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
            submissions.add(new Submission(
                    submission.netId(),
                    submission.repoUrl(),
                    submission.headHash(),
                    submission.timestamp(),
                    submission.phase(),
                    submission.passed(),
                    newScore,                                       // Changed
                    submission.notes(),
                    submission.rubric(),
                    submission.admin(),
                    Submission.VerifiedStatus.ApprovedManually,     // Changed
                    scoreVerification                               // Changed
            ));
            return; // We found it!
        }

        throw new ItemNotFoundException("After verifying that 1 item existed, we didn't actually get to modify it");
    }

}
