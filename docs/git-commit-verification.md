# Git Commit Verification

This Sequence Diagram is provided to help put each of the internal methods
in their proper context in the bigger picture. Minor details are omitted,
but emphasis is placed on actions that directly affect the user 
(like sending notifications to `Grader`), and network calls to DAOs.
Emphasis is placed on showing where system requirements are calculated in the logical flow.

Locating a particular function on this map should visualize how it is being used,
and therefore enable future developers to understand behavior and extend functionality.

## Participant Overview

| Participant | Notes |
| :---------- | :---- |
| `Grader` | Driver class responsible for running the grading algorithm. |
| `Observer` | Sends updates to display directly to student. |
| **`GitHelper`** | **Driver class for git commit verification.** Contains a few reusable methods in other contexts. |
| `SubmissionDao` | Interacts with a database over the internet. |
| `CommitAnalytics` | Intentionally separates low-level interactions with actual `git` commits from the higher level algorithm. |
| `CommitVerificationStrategy` | [Strategy pattern](https://refactoring.guru/design-patterns/strategy) intentionally separates the interpretation of the commit results from analysis algorithm. |

## Sequence Diagram
```mermaid
---
title: AutoGrader — Git Commit Verification Sequence Diagram
---

sequenceDiagram

participant Grader
participant Observer
participant GitHelper
participant SubmissionDao
participant CommitAnalytics
participant CommitVerificationStrategy

%% GRADING INITIALIZATION
note over Grader,GitHelper: Grading initialization
Grader->>+Grader: new Grader(...)
Grader->>GitHelper: new GitHelper(gradingContext)
GitHelper->>CommitVerificationStrategy: new DefaultGitVerificationStrategy()
Grader-->>-Grader: Grader

%% GRADING EVALUATION
note over Grader,GitHelper: Grading evaluation
Grader->>+Grader: run()
Grader->>+GitHelper: setUpAndVerifyHistory()

%% Initial internal setup
GitHelper->>+GitHelper: setUp()
note right of GitHelper: Download repository from internet <br>and determine head hash
GitHelper-->>-GitHelper: void
%% Finish internal setup

%% Decision making points
GitHelper->>+GitHelper: verifyCommitHistory()
GitHelper-->>Observer: update("Verifying commits...")
GitHelper->>+GitHelper: shouldVerifyCommits()
note right of GitHelper: Consider admin and phase<br>grading requirements
GitHelper-->>-GitHelper: boolean

    alt shouldVerifyCommits
        %% Verify commit requirements
        GitHelper->>+GitHelper: verifyCommitRequirements()

            %% Preserving original verification results
            GitHelper->>+GitHelper: preserveOriginalVerification()
            GitHelper->>SubmissionDao: getFirstPassingSubmission(netId, phase)
            GitHelper->>GitHelper: generateFailureMessage(verified, firstPassingSubmission)
            GitHelper-->>-GitHelper: CommitVerificationResult | null
            opt if verification != null
                GitHelper-->>GitHelper: return verification
            end

            %% Determine commit ranges
            GitHelper->>SubmissionDao: getAllPassingSubmissions(netId)
            GitHelper->>+GitHelper: getMostRecentPassingSubmission(passingSubmissions)
            note right of GitHelper: Evaluate commit ancestry and<br>submissions timestamps. <br>Represents the submission from<br> the previous phase.
            GitHelper-->>-GitHelper: CommitThreshold

            GitHelper->>+GitHelper: constructCurrentThreshold()
            note right of GitHelper: Represents the instant the<br> submission was received
            GitHelper-->>-GitHelper: CommitThreshold

            %% Perform commit verification
            GitHelper->>+GitHelper: verifyRegularCommits(lowerThreshold, upperThreshold)
            loop do until no commits added to exclude set
                GitHelper->>+CommitAnalytics: countCommitsByDay()
                note over CommitAnalytics: Analyze all commits between<br>thresholds, checking conditions<br> and tallying violations
                CommitAnalytics-->>-GitHelper: CommitsByDay

                GitHelper->>+CommitVerificationStrategy: evaluate(CommitVerificationContext, GradingContext)
                note over CommitVerificationStrategy: Review results to determine<br>if the submission passes.<br> Optionally, signal to repeat by<br> preparing an exclude set of commit hashes.
                CommitVerificationStrategy-->>-GitHelper: void
                GitHelper->>CommitVerificationStrategy: extendExcludeSet()
            end
            GitHelper->>CommitVerificationStrategy: getWarnings()
            GitHelper->>CommitVerificationStrategy: getErrors()

            GitHelper-->>-GitHelper: CommitVerificationResult

        GitHelper-->>-GitHelper: CommitVerificationResult
        %% End commit verification
    else
        GitHelper->>+GitHelper: skipCommitVerification()
        note right of GitHelper: Return empty, valid verification
        GitHelper-->>-GitHelper: CommitVerificationResult
    end

    loop warning in warningMessages
        GitHelper-->>Observer: notifyWarning(warning)
    end

    break GradingException thrown
        GitHelper-->>Observer: notifyWarning("Internally failed<br> to evaluate commit history: ...")
        GitHelper-->>Grader: return skipCommitVerification()
    end

GitHelper-->>-GitHelper: CommitVerificationResult
%% Finish decision making

%% Return to caller
GitHelper-->>-Grader: CommitVerificationResult

%% GRADING COMPLETION
%% (Notice this continues in `run()` from above.)
note over Grader,GitHelper: Grading Completion
Grader->>+Grader: evaluateProject(CommitVerificationResult)
note right of Grader: Phase graders may use<br> verification results to assign<br> points directly (ex. GitHub Repo).
Grader-->>-Grader: Rubric

Grader->>+Grader: Scorer.score(rubric, CommitVerificationResult)
note right of Grader: Scorer applies penalty in<br> commit verification result<br> with other penalties.
Grader-->>-Grader: Submission

Grader->>SubmissionDao: insertSubmission(submission)
Grader->>Observer: notifyDone(submission)

Grader-->>-Grader: void
```

## Class Diagram

```mermaid
classDiagram
direction TB

namespace Git {
    class GitHelper {
        <<Service>>
        -Logger LOGGER$
        -GradingContext gradingContext
        -CommitVerificationStrategy commitVerificationStrategy
        -String headHash
        +setUpAndVerifyHistory() CommitVerificationReport
        +setUp() void
        +verifyCommitHistory() CommitVerificationReport
        %% -shouldVerifyCommits() boolean
        %% -fetchRepo(File intoDirectory) void
        +fetchRepoFromUrl(String repoUrl) File$
        +fetchRepoFromUrl(String repoUrl, File intoDirectory) void$
        %% -skipCommitVerification(boolean verified, File stageRepo) CommitVerificationReport
        %% -skipCommitVerification(boolean verified, String headHash, String failureMessage) CommitVerificationReport
        %% #verifyCommitRequirements(File stageRepo) CommitVerificationReport
        %% -preserveOriginalVerification() CommitVerificationReport
        %% -generateFailureMessage(boolean verified, Submission firstPassingSubmission) String
        %% #verifyRegularCommits(git, lowerThreshold, upperThreshold) CommitVerificationReport
        %% -getMostRecentPassingSubmission(Git git, Collection~Submission~ passingSubmissions) CommitThreshold
        %% -getEffectiveTimestampOfSubmission(RevWalk revWalk, Submission submission) Instant
        %% -constructCurrentThreshold(Git git) CommitThreshold
        %% -getPassingSubmissions() Collection~Submission~
        %% -getFirstPassingSubmission() Submission
        %% -getHeadHash(File stageRepo) String
        %% #getHeadHash(Git git) String$
    }

    class CommitVerificationReport {
        +CommitVerificationContext context
        +CommitVerificationResult result
    }

    class CommitVerificationResult {
        +boolean verified
        +boolean isCachedResponse
        +int totalCommits
        +int significantCommits
        +int numDays
        +boolean missingTail
        +int penaltyPct
        +String failureMessage
        +Instant minAllowedThreshold
        +Instant maxAllowedThreshold
        +String headHash
        +String tailHash
        +toReport(context) CommitVerificationReport
    }

    class CommitVerificationConfig {
        +int requiredCommits
        +int requiredDaysWithCommits
        +int minimumChangedLinesPerCommit
        +int commitVerificationPenaltyPct
        +int forgivenessMinutesHead
    }

}

namespace CommitValidation {
    class CommitVerificationStrategy {
        <<Interface>>
        +evaluate(commitContext, gradingContext) void
        +extendExcludeSet() Collection~String~
        +getWarnings() Result
        +getErrors() Result
    }

    class DefaultGitVerificationStrategy

    class CommitVerificationContext {
        +CommitVerificationConfig config
        +CommitsByDay commitsByDay
        +int numCommits
        +int daysWithCommits
        +long significantCommits
    }

    class Result {
        +Collection~String~ messages
        +Set~String~ commitsAffected
        +boolean isEmpty
        +evaluateConditions(conditions, visitor) Result$
    }

    class CV["CV (CommitValidation)"]
    class CV {
        +boolean fails
        +Collection~String~ commitsAffected
        +String errorMsg
        +CV(boolean fails, String errorMsg)$
    }
}

namespace Analytics {
    class CommitsByDay {
        +Map~String, Integer~ dayMap
        +Map~String, Integer~ lineChangesPerCommit
        %% NOTE: Mermaid cannot represent nested generics with multiple types.
        %% That is why we use the square brackets instead of angled brackets.
        %% https://mermaid.js.org/syntax/classDiagram.html#generic-types
        +Map~String, List[String]~ erroringCommits
        +int totalCommits
        +int mergeCommits
        +boolean commitsInOrder
        +boolean commitsInFuture
        +boolean commitsInPast
        +boolean commitsBackdated
        +boolean commitTimestampsDuplicated
        +boolean missingTailHash
        +CommitThreshold lowerThreshold
        +CommitThreshold upperThreshold
        +getErroringCommitsSet(String groupId) Collection~String~ 
    }

    class CommitAnalytics {
        +countCommitsByDay(git, lowerBound, upperBound, excludeCommits) CommitsByDay$
        +generateCSV() String$
    }

    class CommitThreshold {
        +Instant timestamp
        +String commitHash
    }

    class CommitsBetweenBounds {
        +Iterable~RevCommit~ commits
        +boolean missingTail
    }
}

%% Package GIT
GitHelper o-- CommitVerificationStrategy
%% GitHelper -- CommitAnalytics

CommitVerificationReport o-- CommitVerificationContext
CommitVerificationReport o-- CommitVerificationResult
CommitVerificationContext o-- CommitVerificationConfig

CommitVerificationResult --> CommitVerificationReport : toReport()

GradingContext <-- GitHelper
Logger <-- GitHelper

%% Package CommitValidation
CommitVerificationStrategy <|.. DefaultGitVerificationStrategy
CommitsByDay --o CommitVerificationContext

DefaultGitVerificationStrategy *-- "*" CV : Defines
DefaultGitVerificationStrategy <-- "*" Result
CV "*" .. Result: summarizes

%% Package Commit Analytics
CommitThreshold --o CommitsByDay
CommitAnalytics ..> CommitsBetweenBounds : Uses
CommitAnalytics ..> CommitsByDay : Produces
```
