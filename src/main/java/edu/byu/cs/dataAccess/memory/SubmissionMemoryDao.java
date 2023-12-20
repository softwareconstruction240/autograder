package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class SubmissionMemoryDao implements SubmissionDao {

    private static final ConcurrentHashMap<String, Submission> submissions = new ConcurrentHashMap<>();

    @Override
    public void insertSubmission(Submission submission) {
        submissions.put(submission.netId(), submission);
    }

    @Override
    public Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) {
        return submissions
                .values()
                .stream()
                .filter(submission ->
                        submission.netId().equals(netId) && submission.phase().equals(phase))
                .toList();

    }

    @Override
    public Collection<Submission> getSubmissionsForUser(String netId) {
        return submissions
                .values()
                .stream()
                .filter(submission ->
                        submission.netId().equals(netId))
                .toList();
    }
}
