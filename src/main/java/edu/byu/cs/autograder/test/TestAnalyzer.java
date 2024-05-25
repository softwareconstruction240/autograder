package edu.byu.cs.autograder.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.TestAnalysis;
import edu.byu.cs.model.TestNode;
import edu.byu.cs.util.FileUtils;

import java.io.File;
import java.util.Set;

/**
 * Parses the output of the JUnit Console Runner
 * <b>Important: this class is ONLY compatible with the xml output on the JUnit standalone client</b><br/>
 */
public class TestAnalyzer {

    /**
     * Parses the output of the JUnit Console Runner
     *
     * @param junitXmlOutput   file containing test output
     * @param extraCreditTests the names of the test files (excluding .java) worth bonus points. This cannot be null, but can be empty
     * @return the root of the test tree
     */
    public TestAnalysis parse(File junitXmlOutput, Set<String> extraCreditTests, String error) throws GradingException {
        TestNode root = new TestNode();
        TestNode extraCredit = new TestNode();

        String xml = FileUtils.readStringFromFile(junitXmlOutput);
        TestSuite suite;
        try {
             suite = new XmlMapper().readValue(xml, TestSuite.class);
        } catch (JsonProcessingException e) {
            throw new GradingException("Error parsing test output", e);
        }

        for (TestSuite.TestCase testCase : suite.getTestcase()) {
            TestNode base = root;
            String ecCategory = null;
            for(String category : extraCreditTests) {
                if (testCase.getClassname().endsWith(category)) {
                    ecCategory = category;
                    base = extraCredit;
                    break;
                }
            }

            String name = testCase.getName();
            String[] systemOut = testCase.getSystemOut().getData().split("\n");
            for(String str : systemOut) {
                if(str.startsWith("display-name: ")) {
                    str = str.substring(14);
                    if(name.contains("()")) name = str;
                    else name = String.format("%s %s", str, name);
                }
            }

            TestNode node = new TestNode();
            node.setTestName(name);
            TestNode parent = nodeForClass(base, testCase.getClassname());
            parent.getChildren().put(name, node);

            node.setPassed(testCase.getFailure() == null);
            if(testCase.getFailure() != null) {
                node.setErrorMessage(testCase.getFailure().getData());
            }

            if(ecCategory != null) {
                node.setEcCategory(ecCategory);
                parent.setEcCategory(ecCategory);
            }
        }

        TestNode.countTests(root);
        TestNode.countTests(extraCredit);
        return new TestAnalysis(root, extraCredit, error);
    }

    private TestNode nodeForClass(TestNode base, String name) {
        String extra = null;
        if(name.contains(".")) {
            int dotIndex = name.indexOf('.');
            extra = name.substring(dotIndex + 1);
            name = name.substring(0, dotIndex);
        }
        TestNode node = base.getChildren().get(name);
        if(node == null) {
            node = new TestNode();
            node.setTestName(name);
            base.getChildren().put(name, node);
        }

        if(extra == null) return node;
        else return nodeForClass(node, extra);
    }
}