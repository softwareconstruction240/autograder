package edu.byu.cs.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A utility class that enables resource files to be copied from the AutoGrader backend application
 * into a specified directory on the file system
 */
public class ResourceUtils {

    /**
     * The main entry point of the {@link ResourceUtils} class. This method copies
     * resource files from {@code origin} to {@code destinationDir} based on whether
     * the app is running from a JAR or not.
     * <br>
     * More precisely, this method performs the following steps:
     * <ol>
     *     <li>Deletes any existing resource files at the target location (<code>destinationDir</code>)</li>
     *     <li>Gets the path where this class was loaded from (e.g., the JAR or <code>/target/classes/</code></li>
     *     <li>
     *         Depending on the path the class was loaded from, this method calls:
     *         <ul>
     *             <li>{@link ResourceUtils#copyResourceFilesJar(String, String, File)} (running from a JAR)</li>
     *             <li>{@link ResourceUtils#copyResourceFilesNormal(String, File)} (running from a dev environment)</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @param origin the file/folder to copy
     * @param destinationDir the directory to copy into
     */
    public static void copyResourceFiles(String origin, File destinationDir) {
        FileUtils.removeDirectory(new File(destinationDir, origin));
        String codeSource = ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (codeSource.endsWith(".jar")) {
            copyResourceFilesJar(codeSource, origin, destinationDir);
        } else {
            copyResourceFilesNormal(origin, new File(destinationDir.getPath() + origin));
        }
    }

    /**
     * Copies resource files using the file system from {@code origin} to {@code destinationDir}.
     * <br><br>
     * If {@code origin} is a file: the file just gets copied directly.
     * <br>
     * If {@code origin} is a folder: the target directory, {@code destinationDir}, gets created.
     * {@code origin}'s contents is read into a list. Each entry in the list is recursively copied.
     *
     * @param origin the file/folder to copy
     * @param destinationDir the directory to copy into
     */
    private static void copyResourceFilesNormal(String origin, File destinationDir) {
        try (InputStream inputStream = Objects.requireNonNull(
                ResourceUtils.class.getClassLoader().getResourceAsStream(origin))) {
            if (origin.contains(".")) {
                Files.copy(inputStream, destinationDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                FileUtils.createDirectory(destinationDir.getPath());
                String contents = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                String[] fileNames = contents.split("\n");
                for (String fileName : fileNames) {
                    if (fileName.replace('/', ' ').isBlank()) continue;
                    copyResourceFilesNormal(origin + '/' + fileName,
                            new File(destinationDir.getPath() + '/' + fileName));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies resources from a JAR file to {@code destinationDir}.
     * <br><br>
     * More precisely:
     * <ol>
     *     <li>The JAR file is opened</li>
     *     <li>The entries of the JAR file are iterated through</li>
     *     <li>
     *         For each entry, if the entry starts with the {@code origin} path, then:
     *         <ul>
     *             <li>If the entry is a file, that file is copied</li>
     *             <li>If the entry is a folder, a corresponding folder in {@code destinationDir} is created</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @param codeSource the path of the JAR file
     * @param origin the file/folder to copy
     * @param destinationDir the directory to copy into
     */
    private static void copyResourceFilesJar(String codeSource, String origin, File destinationDir) {
        try (JarFile jarFile = new JarFile(codeSource)) {
            Iterator<JarEntry> iter = jarFile.entries().asIterator();
            while (iter.hasNext()) {
                JarEntry next = iter.next();
                String path = next.toString();
                if (!path.startsWith(origin)) continue;
                if (path.contains(".")) {
                    Files.copy(jarFile.getInputStream(next), Path.of(destinationDir.getPath() + path),
                            StandardCopyOption.REPLACE_EXISTING);
                } else {
                    FileUtils.createDirectory(destinationDir.getPath() + path);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
