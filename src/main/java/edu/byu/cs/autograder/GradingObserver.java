package edu.byu.cs.autograder;

import edu.byu.cs.autograder.test.TestAnalyzer;
import edu.byu.cs.model.Submission;

public interface GradingObserver {
    void notifyStarted();

    void update(String message);

    void notifyError(String message);

    void notifyError(String message, String details);

    void notifyError(String message, TestAnalyzer.TestAnalysis analysis);

    void notifyDone(Submission submission);
}
