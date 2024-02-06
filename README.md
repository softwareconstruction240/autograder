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
Before running the server, two configuration files need to be modified to match your environment:
1. `src/main/resources/config.properties` - contains the frontend and backend urls
2. `src/main/resources/frontend/.env.prod` - contains the url of the backend

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

Note, if you are using the docker MySQL database, ensure that in `src/main/resources/db.properties` the property `db.url` is set to `db:3306`

## Development
### Pre-requisites
#### Node/Yarn
The frontend is built using Vue.js. To run the frontend, you will need to have `yarn`. After installing `Node`, run the following to enable `yarn` globally:
```bash
corepack enable
```
(`sudo` may be required)

The frontend can be run in development mode by running the following in the `src/main/resources/frontend` directory:
```bash
yarn dev
```

#### Docker
To run the database locally, you will need to have `docker` installed. Run the following in the root of the project to start the db container:
```bash
docker compose up db -d
```

#### Maven
A local installation of maven is required to run the backend.


Alternatively, you can run the database locally with your own MySQL server. Be sure to update `src/main/resources/db.properties` with the correct database url, username, and password.

## Environment Variables
For both deployment and development, the following environment variables are required to be set:
```
DB_URL=<host>:<port>
DB_USER=<user>
DB_PASSWORD=<password>
DB_NAME=autograder
CANVAS_AUTHORIZATION_KEY=<canvas api key>
```

## Enabling Logging

To enable logging, add arguments `-Dlog4j2.configurationFile=log4j.properties -Dlog4j2.debug=false` as vm options.

In IntelliJ, Go into the run configuration you want to use -> `Modify Options` -> `Add VM options`. This will reveal an additional box inside of the edit menu. Paste the arguments into the box.

If running from the command line, add the arguments immediately after the `java` command.
