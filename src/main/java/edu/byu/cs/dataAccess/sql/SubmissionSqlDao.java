package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.util.Collection;

public class SubmissionSqlDao implements SubmissionDao {
    @Override
    public void insertSubmission(Submission submission) {

    }

    @Override
    public Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) {
        return null;
    }

    @Override
    public Collection<Submission> getSubmissionsForUser(String netId) {
        return null;
    }
}
