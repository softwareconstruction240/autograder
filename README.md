# Autograder

Autograder for [BYU CS 240 Chess project](https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/chess.md#readme)

## Development and Contributing

This project relies on TAs like you to maintain and adapt the program. Come join the team!
Check out the [Contribution Guide](docs/CONTRIBUTING.md) to learn how to effectively contribute as a part of the Autograder Development Team.

Read the [Getting Started Guide](docs/getting-started/getting-started.md) to get the project set up on your machine for development.

## Code Tour
```txt
├─ .devcontainer/devcontainer.json            Skeleton for opening the project inside a development container (required for significant development on windows machines). This can be done in IntelliJ Ultimate or VSCode 
├─ .github                                    Contains files for interacting with GitHub like workflows to make sure the tests still pass
├─ docs                                       Documentation for the autograder outside of the code itself (START HERE if new)
└─ src                                        you know what this is for 😉
   ├─ main
   │  ├─ java/edu.byu.cs                      Root folder for autograder backend source code
   │  │  ├─ analytics                         Git commit analytics, which is it's own feature (see downloads tab) and used by the git commit verification whenever someone submits a phase
   │  │  ├─ autograder                        Central grading funcionality
   │  │  │  ├─ compile                        Submission code verification, modification, and then compilation
   │  │  │  │  ├─ modifiers                   Modifies student code before compilation (like replacing pom.xml files, etc.)
   │  │  │  │  └─ verifiers                   Looks for common errors in student code (like checking that they didn't edit test files). Produces (hopefully) helpful warnings for students 
   │  │  │  ├─ git                            Git commit verification (how many commits over how many days, etc.)
   │  │  │  ├─ quality                        Runs the code quality checker for submissions, reads the output, and compiles the output into a Rubric.Results object 
   │  │  │  ├─ score                          Scores submissions, including applying penalties, saving submissions, and sending scores to canvas
   │  │  │  ├─ test                           Runs JUnit tests (our passoff tests or student unit tests) on submitted code and reads the results of tests
   │  │  │  ├─ Grader.java                    Central class governing (and delegating) all grading functionality
   │  │  │  └─ GradingContext.java            POJO record for items relevant to the particular submission as it's being graded
   │  │  ├─ canvas                            Code governing the contacting of Canvas for things like retreiving course users, retrieving due dates, sending scores, etc.
   │  │  ├─ controller                        HTTP endpoints. In chess we would usually call this a "handler" but Paul initially called them "controller"'s (before they were split from services) and nobody ever changed it
   │  │  ├─ dataAccess                        Accesses the MySQL database (you should be able to figure this one out)
   │  │  ├─ honorChecker                      Downloads and zips up student code for professors to run by the honor checker
   │  │  ├─ model                             POJO model object classes (should be self-explanatory)
   │  │  ├─ properties                        Application properties such as database information, canvas API token, URLs for the frontend and CAS, etc.
   │  │  ├─ server                            Main HTTP server
   │  │  ├─ service                           Service logic for endpoints (should be self-explanatory)
   │  │  ├─ util                              Utility classes, generally for functionality used in multiple unrelated places
   │  │  │  └─ PhaseUtils.java                Special utility class for decisions that depend on which phase is being graded
   │  └─ resources
   │     ├─ frontend                          Code for the frontend of the autograder (written in TypeScript and Vue)
   │     │  └─ src                            Main frontend source code
   │     │     ├─ components                  Contains some Vue components, generally smaller components or components reused in multiple places
   │     │     ├─ network                     Generic Typescript code for contacting the backend server
   │     │     ├─ router                      Main router for the app frontend (determines if the page should go to /login, /admin, etc.
   │     │     ├─ services                    Specific code for contacting the backend server (delegated to 'network')
   │     │     ├─ stores                      Caches items like app config and past submissions so they don't need to be retreived again the next time they're needed 
   │     │     ├─ types                       Typescript type definitions (similar to 'model' in the backend)
   │     │     ├─ utils                       Utility methods, generally used in multiple unrelated contexts
   │     │     └─ views                       Contains some Vue components, generally larger components meant to take up a large portion of the screen and only used in one place.
   │     │        ├─ AdminView                Contains larger Vue components only shown to admins (TAs and instructors)
   │     │        └─ StudentView              Contains larger Vue components relevant to students, although most are reused for admins as well
   │     └─ phases                            Contains raw files used by the autograder backend
   │        ├─ libs                           Necessary jar files (junit, quality checker), and a quality rubric for what to look for
   │        ├─ phase[x]                       Contains the test cases for the phase
   │        └─ pom                            Chess project pom files. These are used for replacing poms in student repos in case they added extra dependencies.
   └─ test
      └─ java
         ├─ edu.byu.cs                        Basic unit tests for the backend systems. Generally the package path is the same as the class it's testing (e.g. the tests for src/main/java/edu/byu/cs/autograder/score/Scorer.java are in src/test/java/edu/byu/cs/autograder/score/ScorerTest.java) 
         └─ integration                       Larger integration tests for backend systems
```

## Deployment

Before running the server, the `src/main/resources/frontend/.env.dev` configuration file needs to match your
environment (the default may be sufficient). It should contain the URL of the backend.

### Docker

For a docker deployment, run one of the following commands:

```bash
# For a deployment with a MySQL database included
docker compose --profile with-db up -d
```

```bash
# For a deployment requiring an external MySQL database
docker compose up -d
```
