package edu.byu.cs.autograder;

import edu.byu.cs.model.Submission;
import edu.byu.cs.model.TestAnalysis;

public interface GradingObserver {
    void notifyStarted();

    void update(String message);

    void notifyError(String message);

    void notifyError(String message, Submission submission);

    void notifyWarning(String message);

    void notifyDone(Submission submission);
}
