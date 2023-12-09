package edu.byu.cs.server;

import edu.byu.cs.autograder.PhaseOneGrader;

import java.io.IOException;

public class Server {
    public static void main(String[] args) {

        PhaseOneGrader grader;
        try {
            grader = new PhaseOneGrader("https://github.com/softwareconstruction240/chess.git", "./stage");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        grader.run();
    }
}