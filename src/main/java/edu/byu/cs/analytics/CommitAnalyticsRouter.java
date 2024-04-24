package edu.byu.cs.analytics;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.util.DateTimeUtils;
import edu.byu.cs.util.FileUtils;

import java.io.File;
import java.time.Instant;

/**
 * Handles different type of commit analytics requests
 */
public class CommitAnalyticsRouter {

    private static final String cacheDir = "commit-cache";

    /**
     * Updates the commit analytics CSV file and returns it
     *
     * @return the CSV data
     */
    public static String update() throws CanvasException, DataAccessException {
        long ts = Instant.now().getEpochSecond();
        String data = CommitAnalytics.generateCSV();

        FileUtils.removeDirectory(new File(cacheDir));
        FileUtils.createDirectory(cacheDir);
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
        if (file == null) return "";

        return FileUtils.readStringFromFile(file);
    }

    /**
     * Returns the timestamp of the most recently cached commit data
     *
     * @return a formatted timestamp
     */
    public static String when() {
        File file = FileUtils.getLastAlphabeticalFile(new File(cacheDir));
        if (file == null) return "";

        long ts = Long.parseLong(file.getName().substring(0, 10));

        return DateTimeUtils.getDateString(ts, true);
    }
}
