package edu.byu.cs.util;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.util.Collection;

public class DataUtils {

    /**
     * Returns the student's first passing submission for the given phase
     *
     * @param netID the netID of the student
     * @param phase the phase they passed off (or didn't)
     * @return the Submission object, or null
     */
    public static Submission getFirstPassingSubmission(String netID, Phase phase) {
        Collection<Submission> submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(netID, phase);
        Submission firstPassing = null;
        int earliest = Integer.MAX_VALUE;
        for (Submission s : submissions) {
            if (!s.passed()) continue;
            if (s.timestamp().getEpochSecond() < earliest) firstPassing = s;
        }
        return firstPassing;
    }
}
