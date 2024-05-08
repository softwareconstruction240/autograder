package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class StudentCodeReader {
    private final Set<File> files;
    private final Map<File, List<String>> fileContents = new HashMap<>();

    public static StudentCodeReader from(GradingContext context) throws IOException {
        return new StudentCodeReader(moduleFiles(context.stageRepo(), "shared", "server", "client"));
    }

    private static Set<File> moduleFiles(File stageRepo, String... modules) throws IOException {
        Set<File> files = new HashSet<>();
        for(String module: modules) {
            Path moduleRoot = Path.of(stageRepo.getPath(), module);
            try (Stream<Path> paths = Files.walk(moduleRoot)) {
                paths.filter(path -> path.toFile().isFile()).forEach((path -> files.add(path.toFile())));
            }
        }
        return files;
    }

    private StudentCodeReader(Set<File> files) {
        this.files = files;
    }

    public List<String> getFileContents(File file) throws IOException {
        if(!fileContents.containsKey(file)) {
            fileContents.put(file, Files.readAllLines(file.toPath()));
        }
        return fileContents.get(file);
    }

    public Stream<File> allFiles() {
        return files.stream();
    }

    public Stream<File> filesMatching(String regex) {
        return files.stream().filter(file -> file.getPath().matches(regex));
    }

}
