package edu.byu.cs.analytics;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/**
 * A main class to run analytics, can be replaced with an endpoint in the future
 */
public class RunAnalytics {

    /**
     * Runs commit analytics and prints them to a file
     * @param args not used
     */
    public static void main(String[] args) {
        CommitAnalytics commitAnalytics = new CommitAnalytics();
        String filepath = "commit-report-" + Instant.now().getEpochSecond() + ".txt";
        printReportToFile(commitAnalytics.generateReport(true), filepath);
    }

    private static void printReportToFile(String report, String file) {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.print(report);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
