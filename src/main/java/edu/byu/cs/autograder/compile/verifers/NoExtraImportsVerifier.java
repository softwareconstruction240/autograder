package edu.byu.cs.autograder.compile.verifers;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoExtraImportsVerifier {

    public static Set<String> findExtraImports(String mavenOutput) {
        Set<String> extraImports = new HashSet<>();
        Pattern pattern = Pattern.compile("package (.+) does not exist");
        Matcher matcher = pattern.matcher(mavenOutput);
        while (matcher.find()) {
            extraImports.add(matcher.group(1));
        }
        return extraImports;
    }
}
