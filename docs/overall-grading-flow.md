## Overview

The UML diagrams present a high-level representation of the interactions among the system components, clarifying the internal architecture and behavior of the autograder.
Any changes that impact the flow of the diagrams should be accompanied by corresponding updates to ensure the diagrams remain relevant.

The autograder's interaction with BYU's authentication service (OAuth 2.0) is described [here](https://developer.byu.edu/data/api-usage/learn-about-oauth-2-0). 
You will need to log in to your BYU student account to access the documentation.
It will not be covered in the diagram because it happens before the student is granted access to the autograder and does not happen as part of the grading flow.

### Autograder Submission Flow
The following diagram provides a high-level overview, similar to the sequence diagram for phase 2 of the chess project. It traces a single submission from request to completion.
The diagram is simplified as follows: 
- Does not differentiate between the different Service classes or DAOs but refers to them collectively as `Service` and `DAO` respectively.
- Models only the backend.
- Omits most error handling and paths.
- Encapsulates supporting utilities and grading infrastructure within the `Service` component for simplicity.
- Simplifies the response sent to the client during grading as if it was handled synchronously by `Service`, when in reality it is not.

More specific details concerning the grading flow (which is handled asynchronously) can be seen [here](#grading-flow-diagram).
```mermaid
sequenceDiagram
    actor client
    participant Server
    participant Controller
    participant Service
    participant dao as DAO
    participant db@{ "type" : "database" }

    client->>Server:POST /submit <br/>{ "user" : {},<br/>"phase" : "phase",<br/>"githubLink" : "link" }
    Server->>Controller:SubmissionController.submitPost()
    Controller->>Service:SubmissionService.submit()
    Service->>Service:ConfigService.checkForShutdown()
    Service->>dao:ConfigurationDao.getConfiguration(<br/>GRADER_SHUTDOWN_DATE)
    dao->>db:get scheduled shutdown time
    dao-->>Service:Instant
    opt autograder shutdown time has passed
        Service->>Service:ConfigService.triggerShutdown()
        Service->>dao:ConfigurationDao.getConfiguration(<br/>STUDENT_SUMBISSIONS_ENABLED)
        dao->>db:get enabled phases
        dao-->>Service: String
        Service->>dao:ConfigurationDao.setConfiguration(<br/>STUDENT_SUBMISSIONS_ENABLED, [QUALITY])
        dao->>db:set active phases: quality
        Service->>dao:ConfigurationDao.setConfiguration(<br/>GRADER_SHUTDOWN_DATE, Instant.MAX)
        dao->>db:set shutdown date to far future
        Service-->>Server:BadRequestException
        Server-->>client:[400]<br/>"Student Submission is disabled for <phase>"
    end

    Service->>Service:PhaseUtils.isPhaseEnabled()
    Service->>dao:ConfigurationDao.getConfiguration(<br/>STUDENT_SUBMISSIONS_ENABLED)
    dao->>db:get enabled submissions
    dao-->>Service:String

    Service->>Service:SubmissionService.assertHasNewCommits()
    create participant git@{ "type" : "entity" }
    Service->>git:get newest commit
    destroy git
    git-->>Service:newest commit
    Service->>dao:SubmissionDao.getSubmissionsForPhase(<br/>netid, phase)
    dao->>db:get student's previous graded submissions
    dao-->>Service:Collection<Submission>

    Service->>Service:SubmissionService.startGrader()
    Service->>dao:QueueDao.addToQueue(netid, phase)
    dao->>db:add submission to queue
    Service->>Controller:TrafficController.addGrader(Grader)
    Controller->>Service:ExecutorService.submit(Grader)
    Service-)Controller:TrafficController.notifySubscribers(<br/>netid, message)
    Controller->>Controller:WebSocketController.<br/>send(Session, message)
    Controller-->>client:200<br/>{ "results" : {} }
```

### Grading flow diagram
The following diagram expands on the `submit(Grader)` call at the end of the previous diagram. The diagram illustrates the execution of a single Grader from start to completion.
Multiple Graders are managed by an [Executor Service](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html) with a threadpool size of 1. 
Upon completion, a `Submission` is created and uploaded to the database. The diagram is simplified as follows:
- Canvas integration is abstracted and modeled [here](#canvas-integration-diagram). All interaction with canvas happens immediately before `Scorer` returns the `Submission` to `Grader`, as indicated by the red box on the diagram
- One DAO is used to represent all DAOs used, in addition to `DatabaseHelper`.
- Supporting utility classes (`GitHelper`, `CompileHelper`, `TestHelper`, `LateDayCalculator`) are identified collectively as `Helper`.
- The diagram represents the grading flow for a non-admin user; admin-specific behavior is not shown.
- The timeline of the Sequence Diagram begins at the invocation of the `run()` method in `Grader`.
- Exception handling is largely ignored.
- Logical flow is represented only for phases 0–6; grading for the GitHub repository assignment or submissions for code quality alone follow a similar flow with minor variations.
```mermaid
sequenceDiagram
    actor user
    participant TrafficController
    participant GradingObserver
    participant Grader
    participant Helper
    participant Scorer
    participant dao as DAO
    
    Grader->>GradingObserver:notifyStart()
    GradingObserver->>dao:QueueDao.markStarted(netid)
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type", "started" })
    TrafficController->>user:notify start
    
    Grader->>Helper:GitHelper.setUpAndVerifyHistory()
    Helper->>GradingObserver:update("Fetching Repo...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type", "update" })
    TrafficController->>user:"Fetching Repo..."
    
    Helper->>GradingObserver:update("Verifying Commits...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type", "update" })
    TrafficController->>user:"Verifying Commits..."
    
    opt git commit verification contains warnings
        Helper->>GradingObserver:notifyWarning(warningMessage)
        GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type", "warning" })
        TrafficController->>user:Warning Message    
    end
    Helper-->>Grader:CommitVerificationReport
    
    Grader->>dao:DatabaseHelper.setup()
    dao->>dao:Create User
    dao->>dao:Grant Privileges
    dao->>dao:inject db.config file into student repo
    
    Grader->>Helper:CompileHelper.compile()
    Helper->>Helper:verify()
    Helper->>GradingObserver:update("Verifying Code...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type", "update" })
    TrafficController->>user:"Verifying Code..."
 
    Helper->>Helper:modify()
    
    Helper->>Helper:packageRepo()
    Helper->>GradingObserver:update("Compiling Code...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type", "update" })
    TrafficController->>user:"Compiling Code..."
    
    Grader->>Grader:PreviousPhasePassoffTestGrader.runTests()
    Grader->>GradingObserver:update("Compiling previous<br/>phase passoff tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Compiling previous phase passoff tests..."
    Grader->>Helper:TestHelper.compileTests()
    Grader->>GradingObserver:update("Running previous<br/>phase passoff tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Running previous phase passoff tests..."
    Grader->>dao:RubricConfigDao.getRubricConfig(previousPhase)
    dao-->>Grader:RubricConfig
    Grader->>dao:RubricConfigDao.getRubricConfig(phase)
    dao-->>Grader:RubricConfig
    Grader-->>Grader:Rubric.Results

    Grader->>dao:RubricConfigDao.getRubricConfig(phase)
    dao-->>Grader:RubricConfig

    Grader->>Grader:PassoffTestGrader.runTests()
    Grader->>GradingObserver:update("Compiling<br/>passoff tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Compiling passoff tests..."
    Grader->>Helper:TestHelper.compileTests()
    Grader->>GradingObserver:update("Running<br/>passoff tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Running passoff tests..."
    Grader->>dao:RubricConfigDao.<br/>getRubricConfig(previousPhases)
    dao-->>Grader:RubricConfig
    Grader-->>Grader:Rubric.Results

    Grader->>Grader:UnitTestGrader.runTests()
    Grader->>GradingObserver:update("Compiling<br/>unit tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Compiling unit tests..."
    Grader->>Helper:TestHelper.compileTests()
    Grader->>GradingObserver:update("Running<br/>unit tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Running unit tests..."
    Grader->>dao:RubricConfigDao.<br/>getRubricConfig(phase)
    dao-->>Grader:RubricConfig
    Grader-->>Grader:Rubric.Results

    Grader->>Grader:ExtraCreditTestGrader.runTests()
    Grader->>GradingObserver:update("Compiling<br/>extra credit tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Compiling extra credit tests..."
    Grader->>Helper:TestHelper.compileTests()
    Grader->>GradingObserver:update("Running<br/>extra credit tests...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Running extra credit tests..."
    Grader->>dao:RubricConfigDao.<br/>getRubricConfig(phase)
    dao-->>Grader:RubricConfig
    Grader-->>Grader:Rubric.Results
   
    Grader->>Grader:QualityGrader.runQualityChecks()
    Grader->>dao:RubricConfigDao.<br/>getRubricConfig(phase)
    dao-->>Grader:RubricConfig
    Grader->>GradingObserver:update("Running<br/>code quality...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Running code quality..."
    Grader-->>Grader:Rubric.Results

    Grader->>Scorer:score(rubric)
    Scorer->>GradingObserver:update("Grading...")
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "update" })
    TrafficController->>user:"Grading..."
    Scorer->>Helper:LateDayCalculator.<br/>calculateLateDays()
    Helper-->>Scorer:numDaysLate
    Scorer->>Scorer:applyLatePenalty()

    rect rgb(100, 0, 0)
        note left of Scorer: see Canvas Integration Diagram
    end    
    Scorer-->>Grader:Submission
    
    Grader->>dao:SubmissionDao.insertSubmission()
    Grader->>GradingObserver:notifyDone(submission)
    GradingObserver->>TrafficController:notifySubscribers(<br/>netid, { "type" : "results" })
    TrafficController->>user:grading results
    GradingObserver->>TrafficController:clearSessions(netid)
    GradingObserver->>dao:QueueDao.remove(netid)<br/>(removes submission from queue)
    Grader->>dao:DatabaseHelper.db cleanup
```

### Canvas Integration Diagram

Interaction between Canvas and the Autograder during grading is modeled below.
The Scorer's entry point is the `score()` method, which is traced from beginning to end.
Anything enclosed in carats (`<`,`>`) will be replaced by an actual number or string during execution.
At the end of the scoring sequence, `Scorer` returns a `Submission` to the `Grader`.

```mermaid
sequenceDiagram
    participant Helper
    participant Scorer
    participant dao as DAO
    participant ci as CanvasIntegration
    participant cAPI as CanvasAPI
    
    Scorer->>dao:getCanvasUserId(netid)
    dao-->>Scorer:CanvasID
    Scorer->>dao:getPhaseAssignmentNumber(phase)
    dao-->>Scorer:assignmentNum
    
    Scorer->>ci:getSubmission(canvasID, assignmentNum)
    ci->>dao:getCourseNum()
    dao-->>ci:courseNum
    ci->>cAPI:GET<br/>https://canvas.instructure.com/api/v1<br/>/courses/<courseNum>/submissions/<canvasID><br/>?include[]=rubric_assessment
    cAPI-->>ci:{ "url" : <url> ,<br/>"rubric_assessment" : {} ,<br/>"score" : <score> }
    ci-->>Scorer:CanvasSubmission
    
    Scorer->>dao:getRubricConfig()
    dao-->>Scorer:RubricConfig
    Scorer->>Helper:convertToAssessment(rubric, rubricConfig, phase)
    Helper-->>Scorer:CanvasRubricAssessment
    Scorer->>Scorer:Combine new and current assessments
    Scorer->>dao:getCanvasRubricId(RubricType, phase)
    dao-->>Scorer:rubricId
    Scorer->>Scorer:setCommitVerificationPenalty()
    Scorer->>ci:submitGrade()
    ci->>cAPI:PUT<br/>https://canvas.instructure.com/api/v1<br/>/courses/<courseNum>/assignments/<assignmentNum>/submissions/[canvasID]<br/>?submission[posted_grade]=<grade>&comment[text_comment]=<comment>    
```
