package edu.byu.cs.autograder;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Parses the output of the JUnit Console Runner
 * <b>Important: this class is ONLY compatible with the testfeed details mode on the JUnit standalone client</b><br/>
 * <i>e.g. --details=testfeed</i>
 */
public class TestAnalyzer {

    public record TestAnalysis(TestNode root, String error) {}

    public static class TestNode implements Comparable<TestNode>, Cloneable {
        String testName;
        Boolean passed;
        String ecCategory;
        String errorMessage;
        Map<String, TestNode> children = new HashMap<>();

        /**
         * The number of tests that passed under this node (excluding extra credit)
         */
        Integer numTestsPassed;

        /**
         * The number of tests that failed under this node (excluding extra credit)
         */
        Integer numTestsFailed;

        /**
         * The number of extra credit tests that passed under this node
         */
        Integer numExtraCreditPassed;

        /**
         * The number of extra credit tests that failed under this node
         */
        Integer numExtraCreditFailed;

        public Boolean getPassed() {
            return passed;
        }

        public Integer getNumExtraCreditFailed() {
            return numExtraCreditFailed;
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            printNode(this, stringBuilder, "");
            return stringBuilder.toString();
        }

        private void printNode(TestNode node, StringBuilder sb, String indent) {
            sb.append(indent).append(node.testName);
            if (node.ecCategory != null) sb.append(" (Extra Credit)");
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
                    if (node.ecCategory != null) {
                        node.numExtraCreditPassed = 1;
                        node.numTestsPassed = 0;
                    } else {
                        node.numExtraCreditPassed = 0;
                        node.numTestsPassed = 1;
                    }
                    node.numTestsFailed = 0;
                    node.numExtraCreditFailed = 0;
                } else {
                    if (node.ecCategory != null) {
                        node.numTestsFailed = 0;
                        node.numExtraCreditFailed = 1;
                    } else {
                        node.numTestsFailed = 1;
                        node.numExtraCreditFailed = 0;
                    }
                    node.numTestsPassed = 0;
                    node.numExtraCreditPassed = 0;
                }
            } else {
                node.numTestsPassed = 0;
                node.numTestsFailed = 0;
                node.numExtraCreditPassed = 0;
                node.numExtraCreditFailed = 0;
            }

            for (TestNode child : node.children.values()) {
                countTests(child);
                node.numTestsPassed += child.numTestsPassed;
                node.numTestsFailed += child.numTestsFailed;
                node.numExtraCreditPassed += child.numExtraCreditPassed;
                node.numExtraCreditFailed += child.numExtraCreditFailed;
            }
        }

        @Override
        public int compareTo(TestNode o) {
            return this.testName.compareTo(o.testName);
        }

        @Override
        public TestNode clone() {
            try {
                TestNode clone = (TestNode) super.clone();
                clone.children = new HashMap<>();
                for (Map.Entry<String, TestNode> entry : children.entrySet()) {
                    clone.children.put(entry.getKey(), entry.getValue().clone());
                }
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Bundles two TestNodes into a single TestNode
         *
         * @param bundleName the name of the parent TestNode
         * @param nodes      the second TestNode
         * @return a new TestNode that is the result of bundling a and b
         */
        public static TestNode bundle(String bundleName, TestNode... nodes) {
            TestNode merged = new TestNode();
            merged.testName = bundleName;

            for (TestNode node : nodes)
                merged.children.put(node.testName, node);

            TestNode.countTests(merged);

            return merged;
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
            if (!currentNode.children.containsKey(part)) {
                TestNode newNode = new TestNode();
                newNode.testName = part;
                currentNode.children.put(part, newNode);
            }

            if (ecCategories.contains(part)) ec = part;
            if (ec != null) currentNode.children.get(part).ecCategory = part;

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
    private void handleErrorMessage(String line) throws GradingException {
        if (lastFailingTest == null) {
            throw new GradingException("Error message without a test: " + line);
        }

        if (lastFailingTest.errorMessage == null)
            lastFailingTest.errorMessage = "";

        lastFailingTest.errorMessage += (line + "\n");
    }
}