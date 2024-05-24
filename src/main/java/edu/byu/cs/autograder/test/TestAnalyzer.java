package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.TestAnalysis;
import edu.byu.cs.model.TestNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Parses the output of the JUnit Console Runner
 * <b>Important: this class is ONLY compatible with the testfeed details mode on the JUnit standalone client</b><br/>
 * <i>e.g. --details=testfeed</i>
 */
public class TestAnalyzer {

    /**
     * The root of the test tree
     */
    private TestNode root;

    /**
     * The last test that failed. Error messages are added to this test
     */
    private TestNode lastFailingTest;

    /**
     * The names of the test files (excluding .java) worth bonus points
     */
    private Set<String> ecCategories;

    /**
     * Parses the output of the JUnit Console Runner
     *
     * @param inputLines       the lines of the output of the JUnit Console Runner
     * @param extraCreditTests the names of the test files (excluding .java) worth bonus points. This cannot be null, but can be empty
     * @return the root of the test tree
     */
    public TestAnalysis parse(String[] inputLines, Set<String> extraCreditTests, String error) throws GradingException {
        this.ecCategories = extraCreditTests;

        for (String line : inputLines) {
            line = line.replaceAll("\u001B\\[[;\\d]*m", ""); //strip off color codes

            // the test results section has started
            if (line.startsWith("JUnit Jupiter") && root == null) {
                root = new TestNode();
                root.setTestName("JUnit Jupiter");

            }
            // we haven't started the test results section yet
            else if (root == null) {
                continue;
            }

            // the test results section has ended
            if (line.isEmpty())
                break;

            // Each tests prints two lines, so we can skip the STARTED lines
            if (line.endsWith("STARTED")) continue;

            // Test results always start with "JUnit Jupiter > "
            if (line.startsWith("JUnit Jupiter > "))
                // this line is a new test
                handleTestLine(line.substring("JUnit Jupiter > ".length()));

            else // this line is an error message that relates to the last failing test
                handleErrorMessage(line);
        }

        if (root != null) {
            TestNode.countTests(root);
        }

        return new TestAnalysis(root, error);
    }

    /**
     * Handles a line that is a test result and adds it to the tree
     * Example valid inputs:
     * <i>EnPassantTests > White En Passant Left :: SUCCESSFUL</i><br/>
     * <i>Game Loads :: SUCCESSFUL</i><br/>
     * <i>MoveTests > Pawn > Pawn can move two spaces forward :: FAILED</i>
     *
     * @param line a test result
     */
    private void handleTestLine(String line) {

        String[] parts = line
                .replace(" :: SUCCESSFUL", "")
                .replace(" :: FAILED", "")
                .split(" > ");
        TestNode currentNode = root;
        String ec = null;
        for (String part : parts) {
            if (!currentNode.getChildren().containsKey(part)) {
                TestNode newNode = new TestNode();
                newNode.setTestName(part);
                currentNode.getChildren().put(part, newNode);
            }

            if (ecCategories.contains(part)) ec = part;
            if (ec != null) currentNode.getChildren().get(part).setEcCategory(part);

            currentNode = currentNode.getChildren().get(part);
        }

        currentNode.setPassed(line.endsWith("SUCCESSFUL"));

        if (!currentNode.getPassed()) {
            lastFailingTest = currentNode;
        }
    }

    /**
     * Handles a line that is an error message and adds it to the last failing test
     *
     * @param line an error message from a failed test
     */
    private void handleErrorMessage(String line) throws GradingException {
        if (lastFailingTest == null) {
            throw new GradingException("Error message without a test: " + line);
        }

        if (lastFailingTest.getErrorMessage() == null)
            lastFailingTest.setErrorMessage("");

        lastFailingTest.setErrorMessage(lastFailingTest.getErrorMessage() + (line + "\n"));
    }
}