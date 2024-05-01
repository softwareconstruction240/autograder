package edu.byu.cs.autograder.git;

import java.util.HashMap;
import java.util.Map;

public class GitRepoState {
    public final Map<String, NamedCommit> namedCommits = new HashMap<>();
    private int currentChange = 0;
    public final String scriptRoot = "src/test/resources/";

    public record NamedCommit(String name, String hash) {}

    public int makeChange() {
        return ++currentChange;
    }
    public void setChange(int newChange) {
        if (newChange <= currentChange) {
            throw new IllegalArgumentException("Current change must be greater than the current change.");
        }
        currentChange = newChange;
    }

}
