package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SubmissionMemoryDao implements SubmissionDao {

    private static final ConcurrentLinkedQueue<Submission> submissions = new ConcurrentLinkedQueue<>();

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
        ConcurrentHashMap<String, Submission> latestSubmissions = new ConcurrentHashMap<>();
        submissions.forEach(submission -> {
            String key = submission.netId() + submission.phase();
            latestSubmissions.compute(key, (k, v) -> {
                if (v == null || submission.timestamp().isAfter(v.timestamp())) {
                    return submission;
                } else {
                    return v;
                }
            });
        });
        return latestSubmissions.values();
    }
}
