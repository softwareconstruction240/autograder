package edu.byu.cs.autograder;

import edu.byu.cs.model.Submission;

public interface GradingObserver {
    /**
     * Initializes the student grading process by marking the student queue item as "started",
     * and sending a message to the user.
     * <br>
     * This must be called at the very beginning of the grading process.
     */
    void notifyStarted();

    /**
     * Sends a normal textual update to the user which typically appears in gray to the user.
     * @param message A message to report to the student.
     */
    void update(String message);

    /**
     * **DESTRUCTIVELY** Closes the connection to the student while reporting an error.
     * <br>
     * This should typically only be called from the Grader class.
     *
     * @param message A message to report to the student.
     */
    void notifyError(String message);

    /**
     * **DESTRUCTIVELY** Closes the connection to the student while reporting an error.
     * <br>
     * This should typically only be called from the Grader class.
     *
     * @param message A message to report to the student.
     * @param submission A submission to send to the student, reporting status to the student without saving anywhere.
     */
    void notifyError(String message, Submission submission);

    /**
     * Sends a warning to the user which will be displayed in yellow text.
     * @param message A message to report to the user.
     */
    void notifyWarning(String message);

    /**
     * **TERMINALLY** Closes the connection to the student while reporting success.
     * <br>
     * Finishes by removing the student from the queue.
     *
     * @param submission A submission to send to the student, reporting status to the student without saving anywhere.
     */
    void notifyDone(Submission submission);
}
