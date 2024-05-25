package edu.byu.cs.model;

import java.util.HashMap;
import java.util.Map;

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
        if (node.passed != null) {
            if (node.passed) {
                node.numTestsPassed = 1;
                node.numTestsFailed = 0;
            } else {
                node.numTestsFailed = 1;
                node.numTestsPassed = 0;
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