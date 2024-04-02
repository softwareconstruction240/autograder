package edu.byu.cs.model;

import java.util.Map;
import java.util.Set;

public enum Phase {

    // FIXME: Load this list of data dynamically from a configuration table #156

    Phase0(
            "0",
            "0",
            true,
            null,
            880445,
            Map.of(Rubric.RubricType.PASSOFF_TESTS, "_1958"),
            -1,
            "shared",
            null,
            null,
            Set.of("passoffTests.chessTests", "passoffTests.chessTests.chessPieceTests")
    ),
    Phase1(
            "1",
            "1",
            true,
            Phase0,
            880446,
            Map.of(Rubric.RubricType.PASSOFF_TESTS, "_1958"),
            -1,
            "shared",
            null,
            null,
            Set.of("passoffTests.chessTests", "passoffTests.chessTests.chessExtraCredit")
    ),
    Phase3(
            "3",
            "3",
            true,
            Phase1,
            880448,
            Map.of(
                Rubric.RubricType.PASSOFF_TESTS,  "_5202",
                Rubric.RubricType.UNIT_TESTS,  "90344_776",
                Rubric.RubricType.QUALITY,  "_3003"
            ),
            13,
            "server",
            "service",
            Set.of("serviceTests"),
            Set.of("passoffTests.serverTests")
    ),
    Phase4(
            "4",
            "4",
            true,
            Phase3,
            880449,
            Map.of(
                Rubric.RubricType.PASSOFF_TESTS,  "_2614",
                Rubric.RubricType.UNIT_TESTS,  "_930"
            ),
            18,
            "server",
            "dao",
            Set.of("dataAccessTests"),
            Set.of("passoffTests.serverTests")
    ),
    Phase5(
            "5",
            "5",
            true,
            Phase4,
            880450,
            Map.of(Rubric.RubricType.UNIT_TESTS,  "_8849"),
            12,
            "client",
            "server facade",
            Set.of("clientTests"),
            null
    ),
    Phase6(
            "6",
            "6",
            true,
            Phase5,
            880451,
            null,
            -1,
            "server",
            null,
            null,
            null
    ),
    Quality(
            "42",
            "Quality",
            false,
            null,
            0,
            null,
            -1,
            null,
            null,
            null,
            null
    );

    // ## Value configuration

    public final String stringValue;
    public final String stringName;
    public final boolean isGraded;
    public final Phase previousPhase;

    // Canvas config values
    public final int assignmentNumber;
    public final Map<Rubric.RubricType, String> rubricIds;

    // Test configuration
    public final int minUnitTests;
    public final String moduleUnderTest;
    public final String unitTestCodeUnderTest;
    public final Set<String> unitTestPackagesToTest;
    public final Set<String> passoffPackagesToTest;

    Phase(String stringValue, String stringName, boolean isGraded, Phase previousPhase, int assignmentNumber,
          Map<Rubric.RubricType, String> rubricIds, int minUnitTests, String moduleUnderTest,
          String unitTestCodeUnderTest, Set<String> unitTestPackagesToTest, Set<String> passoffPackagesToTest
    ) {
        this.stringValue = stringValue;
        this.stringName = stringName;
        this.isGraded = isGraded;
        this.previousPhase = previousPhase;
        this.assignmentNumber = assignmentNumber;
        this.rubricIds = rubricIds;
        this.minUnitTests = minUnitTests;
        this.moduleUnderTest = moduleUnderTest;
        this.unitTestCodeUnderTest = unitTestCodeUnderTest;
        this.unitTestPackagesToTest = unitTestPackagesToTest;
        this.passoffPackagesToTest = passoffPackagesToTest;
    }
}
