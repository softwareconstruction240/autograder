package edu.byu.cs.server;

import edu.byu.cs.autograder.PhaseOneGrader;

import java.io.IOException;

public class Server {
    public static void main(String[] args) {

        PhaseOneGrader grader;
        try {
            grader = new PhaseOneGrader("https://github.com/softwareconstruction240/chess.git", "./stage", new Server());
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
    public void notifySuccess() {
        System.out.println("Tests passed!");
    }
}