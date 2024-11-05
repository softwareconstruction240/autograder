# Autograder

Autograder for [BYU CS 240 Chess project](https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/chess.md#readme)

## Development and Contributing

This project relies on TAs like you to maintain and adapt the program! Come join the team!
Check out the [Contribution Guide](docs/CONTRIBUTING.md) to learn how to effectively contribute as a part of the Autograder Development Team!

Read the [Getting Started Guide](docs/getting-started/getting-started.md) to get the project set up on your machine for development.

## Important folders

```
phases/ - contains the test cases for each phase
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

> [!NOTE]
> If you are using the docker MySQL database, ensure that in
> `src/main/resources/db.properties` the property `db.url` is set to `db:3306`.
