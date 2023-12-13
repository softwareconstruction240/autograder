# Automatico
Autograder for [BYU CS 240 Chess project](https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/chess.md#readme)

## Important folders
```
phases/ - containes the test cases for each phase
  phase0/
  phase1/
  phase3/
  phase4/
  phase6/
  libs/ - contains libraries needed to run the test cases
    junit-jupiter-api-x.y.z.jar
    junit-platform-console-standalone-x.y.z.jar
  
tmp-<hash of repo>-<timestamp>/ - a temporary directory created by the autograder to run the student's code
  repo/ - destination for the student's code
  test/ - destination for a phase's compiled test cases
  
src/ - you know what this is for 😉
```