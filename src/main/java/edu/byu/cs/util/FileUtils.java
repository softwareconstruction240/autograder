package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
        try (var os = new FileOutputStream(file)) {
            os.write(data.getBytes());
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
        try (FileOutputStream fos = new FileOutputStream(zipFilePath)) {
            File sourceDirectory = new File(sourceDirectoryPath);

            ZipOutputStream zipOut = new ZipOutputStream(fos);

            zipDirectoryContents(sourceDirectory, sourceDirectory, zipOut);

            zipOut.close();
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

    /**
     * @param filePath The path to file/directory to find all the file names and the associated absolute paths
     * @return A map of the file names and the associated absolute paths given a path
     * For example:
     * {
     *     "ChessBoardTests.java":
     *     "IdeaProjects/autograder/phases/phase0/passoff/chess/ChessBoardTests.java"
     * }
     */
    public static Map<String, String> getFileNamesToAbsolutePaths(Path filePath) throws GradingException {
        Map<String, String> fileNamesToAbsolutesPaths = new HashMap<>();
        try (Stream<Path> paths = Files.walk(filePath)) {
            for (Path path : paths.toList()) {
                File file = path.toFile();
                if (file.isFile()) {
                    String fileName = file.getName();
                    String absolutePath = file.getAbsolutePath();
                    fileNamesToAbsolutesPaths.put(fileName, absolutePath);
                }
            }
        } catch (IOException e) {
            throw new GradingException(
                    String.format("Could not find file names given %s: %s", filePath, e.getMessage())
            );
        }
        return fileNamesToAbsolutesPaths;
    }
    /**
     * Gets the files and directories within a given directory with given depth. If
     * the file provided is not a directory, an empty collection will be returned.
     * @param file The directory to scan
     * @return A collection of files contained within the directory
     */
    public static Collection<File> getChildren(File file, Integer depth) {
        if (!file.isDirectory()) return new LinkedList<>();
        Path path = Path.of(file.getAbsolutePath());
        try (Stream<Path> paths = Files.walk(path, depth)) {
            return paths.map(Path::toFile).filter(fileObj -> !fileObj.getName().equals(file.getName())).toList();
        } catch (IOException e) {
            return new LinkedList<>();
        }
    }

}
