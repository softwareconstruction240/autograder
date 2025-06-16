package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import org.eclipse.jgit.annotations.Nullable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides methods to validate and clean/standardize repo URLs
 */
public class RepoUrlValidator {

    /**
     * Determines if a repo URL is a valid repo URL to use for the AutoGrader
     *
     * @param repoUrl the repo URL
     * @return A boolean indicating the result
     */
    public static boolean isValid(@Nullable String repoUrl) {
        try {
            var validator = new RepoUrlValidator();
            var parts = validator.extractRepoParts(repoUrl);
            return isNotFork(parts.username, parts.repoName);
        } catch (InvalidRepoUrlException e) {
            return false;
        }
    }

    public static boolean canClean(@Nullable String repoUrl) {
        try {
            clean(repoUrl);
            return true;
        } catch (GradingException e) {
            return false;
        }
    }

    /**
     * Determines if a repository exists and is not a fork for a given GitHub username
     * and repository name
     *
     * @param githubUsername the name of the GitHub user
     * @param repoName the name of the repository
     * @return a boolean indicating if the repository exists and is not a fork
     */
    public static boolean isNotFork(@Nullable String githubUsername, @Nullable String repoName) {
        if (githubUsername == null || repoName == null) {
            return false; // Invalid to have NULL
        }

        String apiUrl = String.format("https://api.github.com/repos/%s/%s", githubUsername, repoName);
        var apiJSON = NetworkUtils.readGetRequestBody(apiUrl);

        var jsonObj = Serializer.deserialize(apiJSON, Map.class);
        if (jsonObj == null || jsonObj.isEmpty()) {
            return false; // Error response, empty response. Could indicate network error.
        }

        // The repo exists and is not a fork.
        // `True` values are obvious failures.
        // `null` can also occur, but are not acceptable for our purposes.
        return jsonObj.containsKey("fork") && jsonObj.get("fork").equals(false);
    }

    /**
     * Cleans and standardizes the student's repo URL.
     * See {@link RepoUrlValidator#cleanRepoUrl(String)} for more information.
     *
     * @param repoUrl the student's repository URL
     * @return a cleaned and standardized repository URL
     * @throws InvalidRepoUrlException if the repo URL is invalid
     */
    public static String clean(@Nullable String repoUrl) throws InvalidRepoUrlException {
        return new RepoUrlValidator().cleanRepoUrl(repoUrl);
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
    public String cleanRepoUrl(@Nullable String repoUrl) throws InvalidRepoUrlException {
        var parts = extractRepoParts(repoUrl);
        return assembleCleanedRepoUrl(parts);
    }

    /**
     * Parts of a repo URL split into groups
     *
     * @param domainName the beginning of the URL, the domain name (currently only accepting github.com)
     * @param username the username of the author of the repository
     * @param repoName the name of the repository
     */
    private record RepoUrlParts(String domainName, String username, String repoName) { }

    /**
     * Takes isolated parts of a repo URL and assembles the parts together into a cleaned repo URL
     *
     * @param parts the parts of a repo URL separated into groups
     * @return an assembled and clean repo URL
     */
    private String assembleCleanedRepoUrl(RepoUrlParts parts) {
        return String.format("https://%s/%s/%s", parts.domainName, parts.username, parts.repoName);
    }

    /**
     * Decides if a repo URL is valid, and returns the extracted parts from the URL when the URL is valid.
     *
     * @param repoUrl A string repo URL to parse.
     * @return {@link RepoUrlParts} When the URL is valid.
     * @throws InvalidRepoUrlException When the URL in invalid.
     */
    private RepoUrlParts extractRepoParts(@Nullable String repoUrl) throws InvalidRepoUrlException {
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
                return new RepoUrlParts(domainName, githubUsername, repositoryName);
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
    private String[] getRepoRegexPatterns() {
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

    /**
     * Thrown whenever a repo URL is invalid. A repo URL is valid if:
     * <ul>
     *     <li>It is a GitHub link</li>
     *     <li>
     *         All the parts of the URL were captured properly using the
     *         {@link RepoUrlValidator#getRepoRegexPatterns()}
     *     </li>
     * </ul>
     */
    public static class InvalidRepoUrlException extends GradingException {
        InvalidRepoUrlException(String message) {
            super(message);
        }
    }

}
