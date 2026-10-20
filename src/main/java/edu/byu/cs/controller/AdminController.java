package edu.byu.cs.controller;

import java.io.OutputStream;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.controller.exception.ResourceNotFoundException;
import edu.byu.cs.model.User;
import edu.byu.cs.service.AdminService;
import edu.byu.cs.service.AuthenticationService;
import static edu.byu.cs.util.JwtUtils.generateToken;
import io.javalin.http.Cookie;
import io.javalin.http.Handler;

/**
 * Handles admin-specific HTTP endpoints
 */
public class AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;
    private final AuthenticationService authenticationService;

    public AdminController(AdminService adminService, AuthenticationService authenticationService) {
        this.adminService = adminService;
        this.authenticationService = authenticationService;
    }

    public Handler usersGet() {
        return ctx -> {
            Collection<User> users = adminService.getUsers();
            ctx.json(users);
        };
    }

    public Handler testModeGet() {
        return ctx -> {
            User testStudent = adminService.updateTestStudent();
            ctx.cookie(new Cookie("token",
                    generateToken(testStudent.netId()),
                    "/",
                    14400,
                    authenticationService.isSecure(),
                    0,
                    true)
            );
        };
    }

    public Handler commitAnalyticsGet() {
        return ctx -> {
            String option = ctx.pathParam("option");
            try {
                String data = adminService.getCommitAnalytics(option);
                ctx.result(data);
            } catch (IllegalStateException e) {
                LOGGER.error(e.getMessage());
                throw new ResourceNotFoundException(e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
                throw e;
            }
        };
    }

    public Handler honorCheckerZipGet() {
        return ctx -> {
            String sectionStr = ctx.pathParam("section");

            try (OutputStream os = ctx.res().getOutputStream()) {
                adminService.streamHonorCheckerZip(sectionStr, os);

            } catch (Exception e) {
                LOGGER.error("Error compiling honor checker", e);
                throw e;
            }

            ctx.header("Content-Type", "application/zip");
            ctx.header("Content-Disposition", "attachment; filename=" + "downloaded_file.zip");
        };
    }

    public Handler sectionsGet() {
        return ctx -> {
            CanvasSection[] sections = adminService.getAllSections();
            ctx.json(sections);
        };
    }
}
