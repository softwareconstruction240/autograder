package edu.byu.cs.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A TestNode may represent a test package with any subpackages
 * represented as TestNode children. A TestNode may have many TestNode
 * children forming a tree. A 'leaf' TestNode represents a singular
 * test case (e.g. a parent TestNode would be 'KingMoveTests',
 * and its children would be each king move test).
 */
public class TestNode implements Comparable<TestNode>, Cloneable {
    private String testName;
    private Boolean passed;
    private String ecCategory;
    private String errorMessage;
    private Map<String, TestNode> children = new HashMap<>();

    /**
     * The number of tests that passed under this node (excluding extra credit)
     */
    private Integer numTestsPassed;

    /**
     * The number of tests that failed under this node (excluding extra credit)
     */
    private Integer numTestsFailed;

    public String getTestName() {
        return testName;
    }

    public Boolean getPassed() {
        return passed;
    }

    public String getEcCategory() {
        return ecCategory;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, TestNode> getChildren() {
        return children;
    }

    public Integer getNumTestsPassed() {
        return numTestsPassed;
    }

    public Integer getNumTestsFailed() {
        return numTestsFailed;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public void setEcCategory(String ecCategory) {
        this.ecCategory = ecCategory;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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
        node.numTestsPassed = 0;
        node.numTestsFailed = 0;
        if (node.passed != null) {
            if (node.passed) {
                node.numTestsPassed++;
            } else {
                node.numTestsFailed++;
            }
        }

        for (TestNode child : node.children.values()) {
            countTests(child);
            node.numTestsPassed += child.numTestsPassed;
            node.numTestsFailed += child.numTestsFailed;
        }
    }

    public static void collapsePackages(TestNode node) {
        while (node.getChildren().size() == 1) {
            TestNode child = node.getChildren().values().iterator().next();
            if (child.passed != null) return;
            node.testName = String.format("%s.%s", node.testName, child.testName);
            node.children = child.children;
        }

        for (TestNode child : node.children.values()) {
            collapsePackages(child);
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
}
