package edu.byu.cs.honorChecker;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.User;
import edu.byu.cs.util.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public class HonorCheckerCompiler {

    /**
     * Creates a .zip file for all students' repos in the given section
     *
     * @param sectionID the section ID
     * @return the path to the .zip file
     */
    public static String compileSection(int sectionID) throws CanvasException, DataAccessException {
        Optional<CanvasSection> canvasSection = Arrays.stream(CanvasService.getCanvasIntegration().getAllSections())
                .filter(cs -> sectionID == cs.id()).findFirst();
        if (canvasSection.isEmpty()) throw new CanvasException("Could not find specified section");
        String tmpDir = "tmp-section-" + sectionID;
        String zipFilePath = canvasSection.get().name() + ".zip";

        FileUtils.createDirectory(tmpDir);

        Collection<String> studentNetIds;
        try {
            studentNetIds = CanvasService.getCanvasIntegration().getAllStudentNetIdsBySection(sectionID);
        } catch (CanvasException e) {
            throw new RuntimeException("Canvas Exception: " + e.getMessage());
        }

        try {
            for (String netId : studentNetIds) {
                User student = DaoService.getUserDao().getUser(netId);
                if (student == null || student.repoUrl() == null ||
                        (student.firstName().equals("Test") && student.lastName().equals("Student"))) continue;
                File repoPath = new File(tmpDir, String.join("_", student.firstName().replace(' ', '_'),
                        student.lastName().replace(' ', '_'), student.netId()));

                User dbStudent = DaoService.getUserDao().getUser(student.netId());
                if(dbStudent == null) continue;
                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(dbStudent.repoUrl())
                        .setDirectory(repoPath);

                try {
                    Git git = cloneCommand.call();
                    git.close();
                } catch (Exception e) {
                    FileUtils.removeDirectory(repoPath);
                    continue;
                }

                // delete everything except modules
                Consumer<File> action = file -> {
                    String prefix = repoPath + File.separator;
                    if (!file.getPath().startsWith(prefix + "client") &&
                            !file.getPath().startsWith(prefix + "server") &&
                            !file.getPath().startsWith(prefix + "shared")) {
                        file.delete();
                    }
                };
                FileUtils.modifyDirectory(new File(repoPath.getPath()), action);

                try {
                    FileUtils.zipDirectory(repoPath.getPath(), repoPath.getPath() + ".zip");
                } catch (Exception ignored) {
                    new File(repoPath.getPath() + ".zip").delete();
                }

                FileUtils.removeDirectory(repoPath);
            }

            FileUtils.zipDirectory(tmpDir, zipFilePath);
        } catch (RuntimeException | DataAccessException e) {
            FileUtils.removeDirectory(new File(tmpDir));
            new File(zipFilePath).delete();
            throw e;
        }

        FileUtils.removeDirectory(new File(tmpDir));
        return zipFilePath;
    }
}
