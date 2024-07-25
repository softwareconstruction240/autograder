package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ModuleIndependenceVerifier implements StudentCodeVerifier {

    private static final Pattern IMPORT_REGEX = Pattern.compile("^import(\\s+static)?\\s+(\\w+\\.)+\\w+;.*$");

    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        try {
            Set<File> serverFiles = reader.filesMatching(".*server/src/main/java/.*\\.java").collect(Collectors.toSet());
            Set<String> serverPackages = packageNames(serverFiles, reader);

            Set<File> clientFiles = reader.filesMatching(".*client/src/main/java/.*\\.java").collect(Collectors.toSet());
            Set<String> clientPackages = packageNames(clientFiles, reader);

            removeCommonItems(serverPackages, clientPackages);

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
            if (contents.isEmpty()) continue;
            String firstLine = contents.getFirst();
            if (firstLine.startsWith("package")) {
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
                if (!IMPORT_REGEX.matcher(line).matches()) continue;

                String packageImport = getPackageImport(line);
                if (packageImport != null && packages.contains(packageImport)) {
                    String warning = ("File %s imports from package %s (line %d), which exists in another module. " +
                            "The client and server modules should be independent")
                            .formatted(context.stageRepo().toPath().relativize(file.toPath()), packageImport, i + 1);
                    context.observer().notifyWarning(warning);
                }
            }
        }
    }

    /**
     * Gets just the package being imported from a java import statement
     * <p>For example, "import java.util.Set;" would return "java.util"
     * <p>For example, "import static java.sql.Types.NULL;" would return "java.sql"
     *
     * @param line java import statement
     * @return package name
     */
    private static String getPackageImport(String line) {
        int firstSpace = line.indexOf(' ');
        if(firstSpace == -1) return null;
        String packageImport = line.substring(firstSpace).trim();
        int lastPeriod = packageImport.lastIndexOf('.');
        if(lastPeriod != -1) {
            if (packageImport.startsWith("static")) {
                packageImport = packageImport.substring(packageImport.indexOf(' '), packageImport.lastIndexOf('.')).trim();
                lastPeriod = packageImport.lastIndexOf('.');
            }
            packageImport = packageImport.substring(0, lastPeriod);
        }
        return packageImport;
    }

    /**
     * Removes items common in all provided collections
     *
     * @param collections variable number of collections
     */
    private void removeCommonItems(Collection<?>... collections) {
        Collection<?> commonItems = new HashSet<>(collections[0]);
        for(Collection<?> coll : collections) commonItems.removeIf(item -> !coll.contains(item));

        for(Collection<?> coll : collections) coll.removeIf(commonItems::contains);
    }
}
