package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import org.eclipse.jgit.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepoUrlValidator {

    public static boolean isValid(@Nullable String repoUrl) {
        try {
            clean(repoUrl);
            return true;
        } catch (InvalidRepoUrlException e) {
            return false;
        }
    }

    /**
     * Cleans and standardizes the student's repo URL.
     * <br>
     * The resulting string can be used to <code>git clone</code> the repo.
     * The standardized URL is also a suitable identifier for repository uniqueness comparisons.
     *
     * @param repoUrl The student's repository URL.
     * @return Cleaned and standardized repository URL.
     * @throws InvalidRepoUrlException If the repo URL is invalid.
     */
    public static String cleanRepoUrl(@Nullable String repoUrl) throws InvalidRepoUrlException {
        if (repoUrl == null) {
            throw new InvalidRepoUrlException("NULL is not a valid repo URL.");
        }
        String trimmedRepoUrl = repoUrl.trim();

        Pattern pattern;
        Matcher matcher;

        String domainName;
        String githubUsername;
        String repositoryName;
        for (String regexPattern : getRepoRegexPatterns()) {
            pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(trimmedRepoUrl);
            if (matcher.find()) {
                domainName = matcher.group(1).toLowerCase();
                githubUsername = matcher.group(2);
                repositoryName = matcher.group(3);
                return String.format("https://%s/%s/%s", domainName, githubUsername, repositoryName);
            }
        }

        throw new InvalidRepoUrlException("Could not validate repo url given '" + repoUrl + "'.");
    }

    /**
     * Generates an array of Strings representing Regexes for url matching purposes.
     * The array allows for matches from any of multiple formats, and the first match will proceed as a success.
     * The regexes will be evaluated in case-insensitive mode on a whitespace-trimmed string.
     * <br>
     * The regexes should be formed to extract the minimum amount of information while accepting the largest breadth
     * of possible urls. This allows the URL to be constructed into a consistent format for uniqueness checking
     * while also being easy for students to use; they should be able to submit any of the URLs from their repo.
     * <br>
     * The regex should contain exactly three <i>capturing groups</i> which will be interpreted as:
     * <ol>
     *     <li>The domain name of the link. This will be converted to lower case for consistency.</li>
     *     <li>The username. Case sensitivity will be preserved.</li>
     *     <li>The repo name. Case sensitivity will be preserved.</li>
     * </ol>
     * @return An array of strings representing regexes.
     */
    private static String[] getRepoRegexPatterns() {
        // NOTE: This is the place where we require GitHub links. The rest of the grading system behind this wall
        // will function properly with any link that can be run with `git clone`, but we have arbitrarily chosen
        // to require "github.com" links for now.
        // NOTE: Acknowledging the current decision, this system has been set up to be domain name agnostic.
        // Depending on how similarly another provider formats their links, it can be simple to accept BitBucket/GitLabs
        // any other repo link in addition to GitHub links.

        // Strictly match the beginning of the URL to ensure it's a github.com domain (and not fake-github.com)
        // GitHub Username may only contain alphanumeric characters or single hyphens, and cannot begin or end with a hyphen.
        // GitHub repository name can only contain ASCII letters, digits, and the characters ., -, and _. The .git extension will be removed.

        // View the playground regex: https://regex101.com/r/r3dDAW/8

        String domainName = "(github\\.com)";
        String userName = "([a-zA-z0-9-]+)";
        String repoName = "([\\w.-]+?)";
        return new String[]{
                "^(?:https?://)?(?:www\\.)?"+domainName+"/"+userName+"/"+repoName+"(?:\\.git|/|\\?|#|$)", // https
                "^git@"+domainName+":"+userName+"/"+repoName+"\\.git$" // ssh
        };
    }

    public static class InvalidRepoUrlException extends GradingException {
        InvalidRepoUrlException(String message) {
            super(message);
        }
    }

}
