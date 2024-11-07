package edu.byu.cs.controller;

import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.controller.httpexception.BadRequestException;
import edu.byu.cs.controller.httpexception.ResourceNotFoundException;
import edu.byu.cs.model.User;
import edu.byu.cs.service.AdminService;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.Collection;

import static edu.byu.cs.util.JwtUtils.generateToken;

public class AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

    public static final Handler usersGet = ctx -> ctx.json(AdminService.getUsers());

    public static final Handler userPatch = ctx -> {
        String netId = ctx.pathParam(":netId");
        String firstName = ctx.queryParam("firstName");
        String lastName = ctx.queryParam("lastName");
        String repoUrl = ctx.queryParam("repoUrl");
        String roleString = ctx.queryParam("role");

        User.Role role = null;
        if (roleString != null) {
            try {
                role = User.Role.parse(roleString);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("invalid role. must be one of: STUDENT, ADMIN", e);
            }
        }

        User userData = new User(netId, -1, firstName, lastName, repoUrl, role);
        AdminService.updateUser(userData);

        ctx.status(204);
    };

    public static final Handler testModeGet = ctx -> {
        User testStudent = AdminService.updateTestStudent();
        ctx.cookie("token", generateToken(testStudent.netId()), 14400);
    };

    public static final Handler commitAnalyticsGet = ctx -> {
        String option = ctx.pathParam(":option");
        String data;

        try {
            data = AdminService.getCommitAnalytics(option);
        } catch (IllegalStateException e) {
            LOGGER.error(e.getMessage());
            throw new ResourceNotFoundException(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw e;
        }

        ctx.result(data);
    };

    public static final Handler honorCheckerZipGet = ctx -> {
        String sectionStr = ctx.pathParam(":section");

        try (OutputStream os = ctx.res().getOutputStream()) {
            AdminService.streamHonorCheckerZip(sectionStr, os);

        } catch (Exception e) {
            LOGGER.error("Error compiling honor checker", e);
            throw e;
        }

        ctx.header("Content-Type", "application/zip");
        ctx.header("Content-Disposition", "attachment; filename=" + "downloaded_file.zip");
    };

    public static Handler sectionsGet = ctx -> ctx.json(AdminService.getAllSections());
}
