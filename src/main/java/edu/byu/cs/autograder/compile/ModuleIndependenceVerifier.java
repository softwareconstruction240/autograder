package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModuleIndependenceVerifier implements StudentCodeVerifier {
    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        try {
            Set<File> serverFiles = reader.filesMatching(".*server/src/main/java/.*").collect(Collectors.toSet());
            Set<String> serverPackages = packageNames(serverFiles, reader);

            Set<File> clientFiles = reader.filesMatching(".*client/src/main/java/.*").collect(Collectors.toSet());
            Set<String> clientPackages = packageNames(clientFiles, reader);

            Set<String> duplicatedPackages = new HashSet<>(serverPackages);
            duplicatedPackages.removeIf(packageName -> !clientPackages.contains(packageName));
            serverPackages.removeIf(duplicatedPackages::contains);
            clientPackages.removeIf(duplicatedPackages::contains);

            checkImports(context, reader, serverFiles, clientPackages);
            checkImports(context, reader, clientFiles, serverPackages);
        } catch (IOException e) {
            throw new GradingException("Unable to verify module independence", e);
        }
    }

    private Set<String> packageNames(Set<File> files, StudentCodeReader reader) throws IOException {
        Set<String> packages = new HashSet<>();
        for(File file : files) {
            if(!file.getName().endsWith(".java")) continue;
            List<String> contents = reader.getFileContents(file);
            if(contents.isEmpty()) continue;
            String firstLine = contents.getFirst();
            if(firstLine.startsWith("package")) {
                packages.add(firstLine.substring(8, firstLine.indexOf(';')));
            }
        }
        return packages;
    }

    private void checkImports(GradingContext context, StudentCodeReader reader, Set<File> files, Set<String> packages)
            throws IOException {
        for(File file : files) {
            List<String> contents = reader.getFileContents(file);
            for (int i = 0; i < contents.size(); i++) {
                String line = contents.get(i).trim();
                if (line.isEmpty()) continue;
                if (line.matches("^(/\\*\\*|@|(?!import|package|//).*\\b(class|record|enum|@?interface)\\b).*$")) break;

                String packageImport = line.substring(line.indexOf(' ')).trim();
                if (packageImport.startsWith("static")) {
                    packageImport = packageImport.substring(packageImport.indexOf(' '), packageImport.lastIndexOf('.')).trim();
                }
                if (packageImport.contains(".")) {
                    packageImport = packageImport.substring(0, packageImport.lastIndexOf('.'));
                }

                if (packages.contains(packageImport)) {
                    String warning = ("File %s imports from package %s (line %d), which exists in another module. " +
                            "The client and server modules should be independent")
                            .formatted(context.stageRepo().toPath().relativize(file.toPath()), packageImport, i + 1);
                    context.observer().notifyWarning(warning);
                }
            }
        }
    }
}
