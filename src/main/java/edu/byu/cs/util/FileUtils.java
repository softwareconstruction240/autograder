package edu.byu.cs.util;

import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileUtils {

    /**
     * Creates a directory if it doesn't exist
     *
     * @param path the path to said directory
     */
    public static void createDirectory(String path) {
        try {
            File file = new File(path);
            if (file.exists()) return;
            Files.createDirectory(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to file: " + e.getMessage());
        }
    }

    /**
     * Writes a string to a file
     *
     * @param data the data to write
     * @param file the file to be written to
     */
    public static void writeStringToFile(String data, File file) {
        try {
            if (!file.exists()) Files.createFile(file.toPath());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(data);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to file: " + e.getMessage());
        }

    }

    /**
     * Reads data from a file
     *
     * @param file the file to read
     * @return the contents of the file
     */
    public static String readStringFromFile(File file) {
        StringBuilder contentBuilder = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from file: " + e.getMessage());
        }

        return contentBuilder.toString();
    }

    /**
     * Iterates through a directory and deletes everything
     *
     * @param dir the directory to delete
     */
    public static void removeDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            LoggerFactory.getLogger(FileUtils.class).error("Failed to delete stage directory", e);
            throw new RuntimeException("Failed to delete directory: " + e.getMessage());
        }
    }

    /**
     * Grabs the last file alphabetically from a directory
     *
     * @param dir the directory to search through
     * @return the file, null if there are issues
     */
    public static File getLastAlphabeticalFile(File dir) {
        if (!dir.isDirectory()) return null;

        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            Arrays.sort(files);
            return files[files.length - 1];
        }

        return null;
    }
}
