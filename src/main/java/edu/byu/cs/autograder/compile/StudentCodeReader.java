package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Finds all files for each module in the student's repo and reads their contents.
 */
public class StudentCodeReader {
    private final Set<File> files;
    private final Map<File, List<String>> fileContents = new HashMap<>();

    public static StudentCodeReader from(GradingContext context) throws IOException {
        return new StudentCodeReader(moduleFiles(context.stageRepo(), "shared", "server", "client"));
    }

    /**
     * Traverses all the files for each module and returns the files as a set.
     *
     * @param stageRepo the file where the student's repo is located
     * @param modules a list of modules to traverse
     * @return the set of files found from traversing each module
     * @throws IOException if a module isn't located in the student's repo
     */
    private static Set<File> moduleFiles(File stageRepo, String... modules) throws IOException {
        Set<File> files = new HashSet<>();
        for(String module: modules) {
            Path moduleRoot = Path.of(stageRepo.getPath(), module);
            if(moduleRoot.toFile().exists()) {
                try (Stream<Path> paths = Files.walk(moduleRoot)) {
                    paths.filter(path -> path.toFile().isFile()).forEach((path -> files.add(path.toFile())));
                }
            }
        }
        return files;
    }

    private StudentCodeReader(Set<File> files) {
        this.files = files;
    }

    /**
     * Gets the file's contents. If the file has already been read, it will return
     * what has already been read. If not, it will read the all the lines in the file.
     * If an IOException occurs, it will return an empty list.
     *
     * @param file a file object
     * @return a list of strings where each string is a line in the file
     */
    public List<String> getFileContents(File file) {
        if(!fileContents.containsKey(file)) {
            List<String> lines;
            try {
                lines = Files.readAllLines(file.toPath());
            } catch (IOException e) {
                lines = new ArrayList<>();
            }
            fileContents.put(file, lines);
        }
        return fileContents.get(file);
    }

    /**
     * Gets all the student's files whose path matches the provided regex.
     *
     * @param regex a regex pattern
     * @return a stream of files
     */
    public Stream<File> filesMatching(String regex) {
        return files.stream().filter(file -> file.getPath().matches(regex));
    }

    /**
     * Gets a map of the student's file names mapped to its absolute path
     * whose path matches the provided regex
     *
     * @param regex a regex pattern
     * @return a map of the file name mapped to its absolute path
     */
    public Map<String, String> getFileNameToAbsolutePath(String regex) {
        Map<String, String> fileNamesToAbsoluteFilePaths = new HashMap<>();
        for (File file : filesMatching(regex).toList()) {
            fileNamesToAbsoluteFilePaths.put(file.getName(), file.getAbsolutePath());
        }
        return fileNamesToAbsoluteFilePaths;
    }

}
