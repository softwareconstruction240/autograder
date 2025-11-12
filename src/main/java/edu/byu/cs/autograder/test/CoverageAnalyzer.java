package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.ClassCoverageAnalysis;
import edu.byu.cs.model.CoverageAnalysis;
import edu.byu.cs.util.FileUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * Parses the code coverage output stored in a JaCoCo CSV file into a
 * {@link CoverageAnalysis} containing the appropriate coverage results
 */
public class CoverageAnalyzer {
    /**
     * The type of coverage to be tested for. This is the only value that should be
     * updated if coverage requirements change.
     */
    private static final String COVERAGE_TESTED = "LINE";
    private static final String COVERAGE_MISSED_HEADER = COVERAGE_TESTED + "_MISSED";
    private static final String COVERAGE_COVERED_HEADER = COVERAGE_TESTED + "_COVERED";
    private static final String PACKAGE_HEADER = "PACKAGE";
    private static final String CLASS_HEADER = "CLASS";

    /**
     * Parses the output of a JaCoCo CSV file, if it exists,
     * and returns the coverage results as a {@link CoverageAnalysis}
     *
     * @param jacocoCsvOutput the file storing the coverage results from running the tests
     * @return a {@link CoverageAnalysis} containing the code coverage results
     * @throws GradingException if an issue arose parsing the coverage results
     */
    public CoverageAnalysis parse(File jacocoCsvOutput) throws GradingException {
        Collection<ClassCoverageAnalysis> classAnalyses = new HashSet<>();
        if(!jacocoCsvOutput.exists()) {
            return new CoverageAnalysis(classAnalyses);
        }

        String csv = FileUtils.readStringFromFile(jacocoCsvOutput);
        String[] lines = csv.split("\n");

        Integer classHeader = null;
        Integer packageHeader = null;
        Integer coverageMissedHeader = null;
        Integer coverageCoveredHeader = null;

        String[] headers = lines[0].split(",");
        for(int i = 0; i < headers.length; i++) {
            String s = headers[i].trim();
            switch (s) {
                case CLASS_HEADER -> classHeader = i;
                case PACKAGE_HEADER -> packageHeader = i;
                case COVERAGE_MISSED_HEADER -> coverageMissedHeader = i;
                case COVERAGE_COVERED_HEADER -> coverageCoveredHeader = i;
            }
        }

        if(classHeader == null || packageHeader == null || coverageMissedHeader == null || coverageCoveredHeader == null) {
            throw new GradingException("Error parsing coverage results");
        }

        for(int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",");

            String className = values[classHeader].trim();
            String packageName = values[packageHeader].trim();
            int coverageMissed = Integer.parseInt(values[coverageMissedHeader].trim());
            int coverageCovered = Integer.parseInt(values[coverageCoveredHeader].trim());

            classAnalyses.add(new ClassCoverageAnalysis(className, packageName, coverageCovered, coverageMissed));
        }

        return new CoverageAnalysis(classAnalyses);
    }
}
