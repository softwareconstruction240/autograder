package edu.byu.cs.autograder.test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.ArrayList;
import java.util.List;

/**
 * A deserialization target for the output of running the Junit tests, since the output
 * is printed into an XML file.
 * <p>
 * This is because printing the results out to the terminal may cause some tests to be
 * interpreted as failing even though it didn't due to other printing from background
 * threads intermingling with the Junit test output.
 * </p>
 * <p>
 * The annotations in the class are so the XML serialization library will know where to
 * look for the necessary information, since XML allows for more ambiguity than JSON.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSuite {
    @JacksonXmlElementWrapper(useWrapping = false)
    List<TestCase> testcase = new ArrayList<>();

    public List<TestCase> getTestcase() {
        return testcase;
    }

    /**
     * The deserialization target for the output of running a single testcase
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestCase {
        @JacksonXmlProperty(isAttribute = true)
        private String name;

        @JacksonXmlProperty(isAttribute = true)
        private String classname;

        @JsonProperty("failure")
        private CData failure;

        @JsonProperty("error")
        private CData error;

        @JsonProperty("system-out")
        private CData systemOut;

        public String getName() {
            return name;
        }

        public String getClassname() {
            return classname;
        }

        public CData getFailure() {
            return (failure != null) ? failure : error;
        }

        public CData getSystemOut() {
            return systemOut;
        }
    }

    /**
     * The deserialization target for the output of a single category for a specific testcase
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CData {
        @JacksonXmlCData
        @JacksonXmlText
        private String data;

        public String getData() {
            return data;
        }
    }
}
