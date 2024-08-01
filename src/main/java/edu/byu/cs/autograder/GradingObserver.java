package edu.byu.cs.autograder;

import edu.byu.cs.model.Submission;

public interface GradingObserver {
    void notifyStarted();

    void update(String message);

    void notifyError(String message);

    void notifyError(String message, Submission submission);

    void notifyWarning(String message);

    void notifyDone(Submission submission);
}
