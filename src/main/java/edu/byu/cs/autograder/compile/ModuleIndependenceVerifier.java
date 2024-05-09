package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModuleIndependenceVerifier implements StudentCodeVerifier {
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        Set<File> serverFiles = reader.filesMatching(".*server/src/main/java/.*").collect(Collectors.toSet());
        Path serverPath = context.stageRepo().toPath().resolve("server/src/main/java");
        Set<String> serverClasses = qualifiedNames(serverFiles, serverPath);

        Set<File> clientFiles = reader.filesMatching(".*client/src/main/java/.*").collect(Collectors.toSet());
        Path clientPath = context.stageRepo().toPath().resolve("client/src/main/java");
        Set<String> clientClasses = qualifiedNames(clientFiles, clientPath);

        try {
            checkImports(context, reader, serverFiles, clientClasses);
            checkImports(context, reader, clientFiles, serverClasses);
        } catch (IOException e) {
            throw new GradingException("Unable to verify module independence", e);
        }
    }

    private Set<String> qualifiedNames(Set<File> files, Path relativeTo) {
        Set<String> classes = new HashSet<>();
        for(File file : files) {
            String fileName = relativeTo.relativize(file.toPath()).toString();
            if(!fileName.contains(".java")) continue;
            classes.add(fileName.substring(0, fileName.indexOf('.')).replace('/', '.'));
        }
        return classes;
    }

    private void checkImports(GradingContext context, StudentCodeReader reader, Set<File> files, Set<String> classes)
            throws IOException {
        for(File file : files) {
            List<String> contents = reader.getFileContents(file);
            for (String line : contents) {
                if (line.matches("(?:abstract )?class|interface|record")) break;
                for (String classImport : classes) {
                    if (line.matches("import %s;".formatted(classImport))) {
                        String warning = "File %s imports %s. The client and server modules should be independent"
                                .formatted(context.stageRepo().toPath().relativize(file.toPath()), classImport);
                        context.observer().notifyWarning(warning);
                    }
                }
            }
        }
    }
}
