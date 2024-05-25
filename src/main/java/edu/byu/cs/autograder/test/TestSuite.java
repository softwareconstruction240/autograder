package edu.byu.cs.autograder.test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSuite {
    @JacksonXmlElementWrapper(useWrapping = false)
    List<TestCase> testcase = new ArrayList<>();

    public List<TestCase> getTestcase() {
        return testcase;
    }

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
