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

public class ResourceUtils {

    public static void copyResourceFiles(String origin, File destinationDir) {
        FileUtils.removeDirectory(new File(destinationDir, origin));
        String codeSource = ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (codeSource.endsWith(".jar")) {
            copyResourceFilesJar(codeSource, origin, destinationDir);
        } else {
            copyResourceFilesNormal(origin, new File(destinationDir.getPath() + origin));
        }
    }

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
