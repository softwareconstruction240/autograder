# Autograder

Autograder for [BYU CS 240 Chess project](https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/chess.md#readme)

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

## Development

New to autograder development? Follow the [Getting Started guide](docs/getting-started/getting-started.md)!

### How to Contribute

Here are a few tips for first projects or tasks to learn the autograder:
- Review the Sequence/Class Diagrams (_coming soon_) and try creating one
- Add documentation
- Find a section of under-tested code and add some unit tests
- Take a look through the GitHub repo's [Issues](https://github.com/softwareconstruction240/autograder/issues) page 
and find one you like, especially (but not limited to) ones labeled
["good first issue"](https://github.com/softwareconstruction240/autograder/issues?q=is%3Aopen+is%3Aissue+label%3A%22good+first+issue%22).

It's also a good idea to read through the repo's 
`CONTRIBUTING.md` ([COMING SOON](https://github.com/softwareconstruction240/autograder/issues/448)).

Don't be afraid to submit a PR, and most importantly, just get sucked in!
