package edu.byu.cs.dataAccess.sql.helpers;

import edu.byu.cs.util.FileUtils;
import org.intellij.lang.annotations.Language;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * A class responsible for reading .sql files and extracting the
 * set of SQL statements contains within it.
 */
public class SqlScriptParser {

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "--.*?$|/\\*.*?\\*/", Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Parses all SQL statements that the given SQL script file contains.
     *
     * @param file the sql script file to parse
     *
     * @return all SQL statements in order as
     */
    public static @Language("SQL") String[] parseSQLScript(File file) {
        String script = FileUtils.readStringFromFile(file);
        script = COMMENT_PATTERN.matcher(script).replaceAll("");

        return Arrays.stream(script.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

}
