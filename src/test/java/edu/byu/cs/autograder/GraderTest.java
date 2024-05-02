package edu.byu.cs.autograder;

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
        String expectedUrl = "https://github.com/<USERNAME>/<REPO_NAME>";
        String[] urlVariants = {
                "https://github.com/<USERNAME>/<REPO_NAME>/tree/main",
                "https://github.com/<USERNAME>/<REPO_NAME>?tab=readme-ov-file",
                "https://github.com/<USERNAME>/<REPO_NAME>/tree/main/0-chess-moves/starter-code/chess",
                "https://github.com/<USERNAME>/<REPO_NAME>/pull/1"
        };
        for (String urlVariant : urlVariants) {
            assertEquals(expectedUrl, Grader.cleanRepoUrl(urlVariant));
        }
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should not strip github URL when given repo URL with no trailing characters")
    void should_notStripOffGithubUrl_when_givenRepoUrlWithNoTrailingCharacters() throws GradingException {
        // Should not alter already working URLs
        assertEquals("https://github.com/<USERNAME>/<REPO_NAME>",
                Grader.cleanRepoUrl("https://github.com/<USERNAME>/<REPO_NAME>"));

        assertEquals("https://github.com/<USERNAME>/<REPO_NAME>.git",
                Grader.cleanRepoUrl("https://github.com/<USERNAME>/<REPO_NAME>.git"));
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should convert to HTTPS URL when given SSH URL")
    void should_convertToHttpsUrl_when_givenSshUrl() throws GradingException {
        assertEquals("https://github.com/<USERNAME>/<REPO_NAME>",
                Grader.cleanRepoUrl("git@github.com:<USERNAME>/<REPO_NAME>.git"));
    }

    @Test
    @Tag("cleanRepoUrl")
    @DisplayName("Should throw exception when given malformed or not related Url")
    void should_throwException_when_givenMalformedOrNotRelatedUrl() {
        String[] badUrls = {
                "github.com/user/repo",
                "https://wahoooo.com/user/repo",
                "https://wahoooo.com/user/"
        };
        for (String badUrl: badUrls) {
            assertThrows(GradingException.class, () -> {
                Grader.cleanRepoUrl(badUrl);
            });
        }
    }

}
