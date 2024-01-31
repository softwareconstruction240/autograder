package edu.byu.cs.analytics;

import edu.byu.cs.util.DateTimeUtils;
import edu.byu.cs.util.FileUtils;

import java.io.File;
import java.time.Instant;

public class CommitAnalyticsRouter {

    private static final String cacheDir = "commit-cache";

    /**
     * Updates the commit analytics CSV file and returns it
     *
     * @return the CSV data
     */
    public static String update() {
        long ts = Instant.now().getEpochSecond();
        String data = CommitAnalytics.generateCSV();

        FileUtils.removeDirectory(new File(cacheDir));
        FileUtils.writeStringToFile(data, new File(cacheDir + "/" + ts + ".csv"));

        return data;
    }

    /**
     * Returns the data stored in the most recently cached commit analytics CSV file
     *
     * @return the CSV data
     */
    public static String cached() {
        File file = FileUtils.getLastAlphabeticalFile(new File(cacheDir));
        if (file == null) return null;

        return FileUtils.readStringFromFile(file);
    }

    /**
     * Returns the timestamp of the most recently cached commit data
     *
     * @return a formatted timestamp
     */
    public static String when() {
        File file = FileUtils.getLastAlphabeticalFile(new File(cacheDir));
        if (file == null) return null;

        long ts = Long.parseLong(file.getName().substring(0, 10));

        return DateTimeUtils.getDateString(ts, true);
    }
}
