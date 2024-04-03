package edu.byu.cs.util;

import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        modifyDirectory(dir, File::delete);
    }

    /**
     * Iterates through a directory and executes the provided action on each file
     *
     * @param dir the directory to modify
     * @param action the action to perform on each file
     */
    public static void modifyDirectory(File dir, Consumer<File> action) {
        if (!dir.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(dir.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(action);
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

    /**
     * Creates a .zip file from a directory
     *
     * @param sourceDirectoryPath the directory to compress
     * @param zipFilePath the path of the .zip file to be created
     */
    public static void zipDirectory(String sourceDirectoryPath, String zipFilePath) {
        try {
            File sourceDirectory = new File(sourceDirectoryPath);
            FileOutputStream fos = new FileOutputStream(zipFilePath);
            ZipOutputStream zipOut = new ZipOutputStream(fos);

            zipDirectoryContents(sourceDirectory, sourceDirectory, zipOut);

            zipOut.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to zip directory: " + e.getMessage());
        }
    }

    private static void zipDirectoryContents(File rootDirectory, File currentDirectory, ZipOutputStream zipOut) throws IOException {
        File[] files = currentDirectory.listFiles();

        if (files == null) {
            throw new RuntimeException("Unable to read current directory");
        }

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectoryContents(rootDirectory, file, zipOut);
            } else {
                FileInputStream fis = new FileInputStream(file);

                String entryName = rootDirectory.toURI().relativize(file.toURI()).getPath();

                ZipEntry zipEntry = new ZipEntry(entryName);
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }

                fis.close();
            }
        }
    }

    /**
     * Replace newFile with oldFile
     * @param oldFile file to be replaced
     * @param newFile new file to take place of oldFile
     */
    public static void copyFile(File oldFile, File newFile) {
        if (oldFile.exists()) oldFile.delete();
        else oldFile.getParentFile().mkdirs();
        try {
            Files.copy(newFile.toPath(), oldFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy file: " + e.getMessage());
        }
    }
}
