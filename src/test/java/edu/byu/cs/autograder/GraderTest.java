package edu.byu.cs.autograder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
public class GraderTest {

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should strip off trailing characters after repo name when given repo URL")
        void should_stripOffTrailingCharactersAfterRepoName_when_givenRepoUrl() throws GradingException {
        // Should strip off characters after repo name
        String expectedUrl = "https://github.com/USERNAME/REPO_NAME";
        String[] urlVariants = {
                "https://github.com/USERNAME/REPO_NAME/tree/main",
                "https://github.com/USERNAME/REPO_NAME?tab=readme-ov-file",
                "https://github.com/USERNAME/REPO_NAME/tree/main/0-chess-moves/starter-code/chess",
                "https://github.com/USERNAME/REPO_NAME/pull/1",
                "https://github.com/USERNAME/REPO_NAME#/potential/routing/behavior",
        };
        for (String urlVariant : urlVariants) {
            assertEquals(expectedUrl, Grader.cleanRepoUrl(urlVariant));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should standardize all URLs to a consistent format without a trailing '.git' extension")
    void should_standardizeAllUrlsToConsistentFormatWithoutGitExtension() throws GradingException {
        String expectedUrl = "https://github.com/USERNAME/REPO_NAME";
        String[] urlVariants = {
                "https://www.github.com/USERNAME/REPO_NAME",
                "https://www.github.com/USERNAME/REPO_NAME/",
                "https://github.com/USERNAME/REPO_NAME.git/",
                "https://GITHUB.com/USERNAME/REPO_NAME.git",
                "github.com/USERNAME/REPO_NAME.git",
                "GITHUB.com/USERNAME/REPO_NAME.git",

                "           https://www.github.com/USERNAME/REPO_NAME/              ",
                "           https://github.com/USERNAME/REPO_NAME.git/              ",
                "           git@github.com:USERNAME/REPO_NAME.git   "
        };
        for (String urlVariant : urlVariants) {
            assertEquals(expectedUrl, Grader.cleanRepoUrl(urlVariant));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should convert to HTTPS URL when given SSH URL")
    void should_convertToHttpsUrl_when_givenSshUrl() throws GradingException {
        assertEquals("https://github.com/USERNAME/REPO_NAME",
                Grader.cleanRepoUrl("git@github.com:USERNAME/REPO_NAME.git"));
        assertEquals("https://github.com/softwareconstruction240/autograder",
                Grader.cleanRepoUrl("git@github.com:softwareconstruction240/autograder.git"));
        assertThrows(GradingException.class,
                () -> Grader.cleanRepoUrl("git@github.com:USERNAME/REPO_NAME-git"));
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should not throw exceptions when given properly formed URLs")
    void should_accept_when_givenValidUrl() {
        String[] goodUrls = {
                "https://github.com/valid-username/valid.repo.name.10.git/",
                "https://github.com/valid-1-username/valid_repo_name_1234567890.git/",
                "https://github.com/valid-username-0123456789/valid-repo-name.git/",
                "https://github.com/valid-username-0123456789/1_2_3_4_5_6_7_8_9_0.git/",
        };
        for (String badUrl: goodUrls) {
            assertDoesNotThrow(() -> Grader.cleanRepoUrl(badUrl));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should throw exception when given malformed or not related Url")
    void should_throwException_when_givenMalformedOrNotRelatedUrl() {
        String[] badUrls = {
                "https://www.github.com/<USERNAME>/<REPO_NAME>",
                "https://fake-github.com/USERNAME/REPO_NAME/pull/1",
                "fake-github.com/USERNAME/REPO_NAME.git/",
                "fake-github.com/USERNAME/REPO_NAME.git/",
                "https://github.com/invalid.username/valid.repo.name.git/",
                "https://github.com/valid-username/invalid-repo🏴‍☠️/",
                "https://github.com/valid🏴‍☠️username/valid-repo/",
                "https://byu.instructure.com/courses/25927",
                "https://wahoooo.com/user/repo",
                "https://wahoooo.com/user/",
                "github.com:8080/USERNAME/REPO_NAME.git",
                "https://github.com:443/softwareconstruction240/autograder",
        };
        for (String badUrl: badUrls) {
            assertThrows(GradingException.class, () -> Grader.cleanRepoUrl(badUrl));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Admin submissions are not cleaned")
    void adminSubmissionsAreNotCleaned() throws GradingException, IOException {
        String originalUrl = "https://github.com/USERNAME/REPO_NAME/tree/main/0-chess-moves/starter-code/chess";
        // This cannot be easily tested since `Grader` has many other dependencies that would need to be organized as well.
        // As this point, we are not spending the time to make `Grader` into a more testable format.
//        var grader = new Grader(originalUrl, "student_id", null, null , true);
//        Assertions.assertEquals(originalUrl, grader.gradingContext.repoUrl());
    }

}
