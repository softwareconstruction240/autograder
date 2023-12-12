package edu.byu.cs.server;

import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.PhaseOneGrader;

import java.io.IOException;

public class Server implements Grader.Observer {

    private static final String ALL_PASS_REPO = "https://github.com/leesjensen/chess.git";
    private static final String ALL_FAIL_REPO = "https://github.com/softwareconstruction240/chess.git";
    public static void main(String[] args) {

        PhaseOneGrader grader;
        try {
            grader = new PhaseOneGrader(ALL_FAIL_REPO, "./stage", new Server());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        grader.run();
    }

    @Override
    public void update(String message) {
        System.out.println(message);
    }

    @Override
    public void notifyError(String message) {
        System.err.println(message);
    }

    @Override
    public void notifyDone(TestAnalyzer.TestNode results) {
        System.out.println(results);
    }
}