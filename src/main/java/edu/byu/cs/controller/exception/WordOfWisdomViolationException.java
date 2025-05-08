package edu.byu.cs.controller.exception;

/**
 * A more accurate name for this exception would be 'RepoAlreadyClaimedException'.
 * This exception get thrown whenever a user tries claiming a repo url already
 * claimed by another user.
 * <br>
 * This particular exception yields an HTTP response status code of
 * "<a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Status/418">
 *     <code>418 I'm a teapot</code>
 * </a>",
 * and tea is clearly against the Word of Wisdom.
 */
public class WordOfWisdomViolationException extends Exception {
    private static final String shortAndStout = "yo im a teapot";

    public WordOfWisdomViolationException(String message) {
        super(message);
    }
}
