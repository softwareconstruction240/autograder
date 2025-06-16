package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

public class RepoUrlValidatorTest {

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
            assertEquals(expectedUrl, RepoUrlValidator.clean(urlVariant));
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
        };
        for (String urlVariant : urlVariants) {
            assertEquals(expectedUrl, RepoUrlValidator.clean(urlVariant));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should accept all URLs even when provided with superfluous whitespace")
    void should_accept_whenGivenValidUrlsContainsSuperfluousWhitespace() throws GradingException {
        String expectedUrl = "https://github.com/USERNAME/REPO_NAME";
        String[] urlVariants = {
                "           https://www.github.com/USERNAME/REPO_NAME/              ",
                "           https://github.com/USERNAME/REPO_NAME.git/              ",
                "           git@github.com:USERNAME/REPO_NAME.git   "
        };
        for (String urlVariant : urlVariants) {
            assertEquals(expectedUrl, RepoUrlValidator.clean(urlVariant));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should convert to HTTPS URL when given SSH URL")
    void should_convertToHttpsUrl_when_givenSshUrl() throws GradingException {
        assertEquals("https://github.com/USERNAME/REPO_NAME",
                RepoUrlValidator.clean("git@github.com:USERNAME/REPO_NAME.git"));
        assertEquals("https://github.com/softwareconstruction240/autograder",
                RepoUrlValidator.clean("git@github.com:softwareconstruction240/autograder.git"));
        assertThrows(RepoUrlValidator.InvalidRepoUrlException.class,
                () -> RepoUrlValidator.clean("git@github.com:USERNAME/REPO_NAME-git"));
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
        for (String url : goodUrls) {
            assertDoesNotThrow(() -> RepoUrlValidator.clean(url));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should reject with InvalidRepoUrlException when given NULL")
    void should_reject_when_givenNull() {
        assertThrows(RepoUrlValidator.InvalidRepoUrlException.class, () -> RepoUrlValidator.clean(null));
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should throw exception when given malformed or not related Url")
    void should_throwException_when_givenMalformedOrNotRelatedUrl() {
        String[] badUrls = {
                // Invalid characters in <USERNAME> and <REPO_NAME>
                "https://www.github.com/<USERNAME>/REPO_NAME",
                "https://www.github.com/USERNAME/<REPO_NAME>",
                "https://github.com/invalid.username/valid.repo.name.git/",
                "https://github.com/valid-username/invalid-repo🏴‍☠️/",
                "https://github.com/valid🏴‍☠️username/valid-repo/",

                // Not on GITHUB.com
                "https://fake-github.com/USERNAME/REPO_NAME/pull/1",
                "fake-github.com/USERNAME/REPO_NAME.git/",
                "fake-github.com/USERNAME/REPO_NAME.git/",
                "https://byu.instructure.com/courses/25927",
                "https://wahoooo.com/user/repo",
                "https://wahoooo.com/user/",

                // Contain ports which would be stripped off (dangerously)
                "github.com:8080/USERNAME/REPO_NAME.git",
                "https://github.com:443/softwareconstruction240/autograder",
        };
        for (String url : badUrls) {
            assertThrows(RepoUrlValidator.InvalidRepoUrlException.class,
                    () -> RepoUrlValidator.clean(url),
                    "Did not reject input: " + url);
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @Disabled
    @DisplayName("Admin submissions are not cleaned")
    void adminSubmissionsAreNotCleaned() throws GradingException, IOException {
        String originalUrl = "https://github.com/USERNAME/REPO_NAME/tree/main/0-chess-moves/starter-code/chess";
        // This cannot be easily tested since `Grader` has many other dependencies that would need to be organized as well.
        // As this point, we are not spending the time to make `Grader` into a more testable format.
//        var grader = new Grader(originalUrl, "student_id", null, null , true);
//        Assertions.assertEquals(originalUrl, grader.gradingContext.repoUrl());
    }

    @ParameterizedTest
    @Tag("isNotFork")
    @CsvSource({
            "softwareconstruction240, chess, true",
            "softwareconstruction240, invalid-repo-name, false",
            "invalid-username, chess, false"
    })
    @DisplayName("Test RepoUrlValidator.isNotFork with various inputs")
    void isNotForkTests(String username, String repoName, boolean expectedResult) {
        assertEquals(expectedResult, RepoUrlValidator.isNotFork(username, repoName));
    }

    @ParameterizedTest
    @CsvSource({
            "git@github.com:softwareconstruction240/chess.git, true",
            "git@github.com:softwareconstruction240/missing-repo-name.git, false",
            "git@github.com:missing-username/chess.git, false"
    })
    @DisplayName("Test RepoUrlValidator.isValid with various inputs")
    void isValidRepoUrlTests(String url, boolean expectedResult) {
        assertEquals(expectedResult, RepoUrlValidator.isValid(url));
    }

}
