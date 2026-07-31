package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.canvas.FakeCanvasIntegration;
import edu.byu.cs.canvas.model.CanvasSubmission;
import org.eclipse.jgit.annotations.Nullable;

public class FakeGraceDaysCanvasIntegration extends FakeCanvasIntegration {
    private int graceDays = 0;

    public int getGraceDays(){
        return graceDays;
    }

    public void setGraceDays(int graceDays) {
        this.graceDays = graceDays;
    }

    @Override
    public void submitGrade(int userId, int assignmentNum, @Nullable Float grade, @Nullable String comment){
        graceDays = grade.intValue();
    }

    @Override
    public CanvasSubmission getSubmission(int userId, int assignmentNumber){
        return new CanvasSubmission(null, null, (float) graceDays);
    }
}
