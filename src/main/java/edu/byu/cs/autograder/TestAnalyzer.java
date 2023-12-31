package edu.byu.cs.autograder;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses the output of the JUnit Console Runner
 * <b>Important: this class is ONLY compatible with the testfeed details mode on the JUnit standalone client</b><br/>
 * <i>e.g. --details=testfeed</i>
 */
public class TestAnalyzer {

    public static class TestNode {
        String testName;
        Boolean passed;
        String errorMessage;
        Map<String, TestNode> children = new HashMap<>();

        /**
         * The number of tests that passed under this node
         */
        Integer numTestsPassed;

        /**
         * The number of tests that failed under this node
         */
        Integer numTestsFailed;

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            printNode(this, stringBuilder, "");
            return stringBuilder.toString();
        }

        private void printNode(TestNode node, StringBuilder sb, String indent) {
            sb.append(indent).append(node.testName);
            if (node.passed != null) {
                sb.append(node.passed ? " : SUCCESSFUL" : " : FAILED");
                if (node.errorMessage != null && !node.errorMessage.isEmpty()) {
                    sb.append("\n").append(indent).append("   Error: ").append(node.errorMessage);
                }
            } else {
                sb.append(" (").append(node.numTestsPassed).append(" passed, ").append(node.numTestsFailed).append(" failed").append(")");
            }
            sb.append("\n");
            for (TestNode child : node.children.values()) {
                printNode(child, sb, indent + "  ");
            }
        }

        public static void countTests(TestNode node) {
            if (node.passed != null) {
                if (node.passed) {
                    node.numTestsPassed = 1;
                    node.numTestsFailed = 0;
                } else {
                    node.numTestsPassed = 0;
                    node.numTestsFailed = 1;
                }
            } else {
                node.numTestsPassed = 0;
                node.numTestsFailed = 0;
            }

            for (TestNode child : node.children.values()) {
                countTests(child);
                node.numTestsPassed += child.numTestsPassed;
                node.numTestsFailed += child.numTestsFailed;
            }
        }
    }

    /**
     * The root of the test tree
     */
    private TestNode root;

    /**
     * The last test that failed. Error messages are added to this test
     */
    private TestNode lastFailingTest;

    /**
     * Parses the output of the JUnit Console Runner
     *
     * @param inputLines the lines of the output of the JUnit Console Runner
     * @return the root of the test tree
     */
    public TestNode parse(String[] inputLines) {

        for (String line : inputLines) {
            line = line.replaceAll("\u001B\\[[;\\d]*m", ""); //strip off color codes

            // the test results section has started
            if (line.startsWith("JUnit Jupiter") && root == null) {
                root = new TestNode();
                root.testName = "JUnit Jupiter";

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

        if (root != null)
            TestNode.countTests(root);

        return root;
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
        for (String part : parts) {
            if (!currentNode.children.containsKey(part)) {
                TestNode newNode = new TestNode();
                newNode.testName = part;
                currentNode.children.put(part, newNode);
            }

            currentNode = currentNode.children.get(part);
        }

        currentNode.passed = line.endsWith("SUCCESSFUL");

        if (!currentNode.passed) {
            lastFailingTest = currentNode;
        }
    }

    /**
     * Handles a line that is an error message and adds it to the last failing test
     *
     * @param line an error message from a failed test
     */
    private void handleErrorMessage(String line) {
        if (lastFailingTest == null) {
            throw new RuntimeException("Error message without a test: " + line);
        }

        if (lastFailingTest.errorMessage == null)
            lastFailingTest.errorMessage = "";

        lastFailingTest.errorMessage += line;
    }
}