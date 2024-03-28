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

Note, if you are using the docker MySQL database, ensure that in `src/main/resources/db.properties` the
property `db.url` is set to `db:3306`

## Development

### Pre-requisites

#### Node/Yarn

The frontend is built using Vue.js. To run the frontend, you will need to have `yarn`.
After installing `Node`, run the following to enable `yarn` globally:

```bash
corepack enable
```

(`sudo` may be required)

#### Backend

The backend of the app can be run with either `maven`, or by running your own MySQL server.

##### Maven

Go fish🐟 These instructions are not included in this file.

#### Database

You can run the database inside a Docker container, or locally with your own MySQL server.

##### Docker

Run the following in the root of the project to start the db container:

```bash
docker compose up db -d
```

##### MySQL

Go fish🐟 These instructions are not included in this file.

### Getting Started

NOTE: These instructions will help you set up this project in Intelli-J.
If there are any holes or gaps in the instructions, please submit a new pull request
to share the learned knowledge with the people behind you.

1. Clone the repo onto your local machine.
2. Ensure that you are using the latest version of JVM. The system currently requires version 21+.
3. Use `yarn` to install all dependencies. This must be done from the _front end_ root folder.
    ```bash
    cd src/main/resources/frontend
    yarn
    ```
4. Setup "Server" _Run Configuration_
    - Navigate to the server file: `src/main/java/edu/byu/cs/server/Server.java`
    - Click the "Run" button to run `Server.main()`
    - This will give you a _Run Configuration_ that you can modify in the following steps.
5. Set up [program arguments](#program-arguments)
    - Reference the [subtitle below](#program-arguments) for the list of required arguments
    - Prepare the values you will use for each argument
        - Put them all onto a single line, with each argument name followed by its value
        - Ex. `--arg-1 arg1Val --arg-2 arg2Val`
    - Edit the "Server" run configuration
        - Paste in the single line of parameters to the line titled "Program arguments"
    - Save & apply the changes
6. [Enable logging](#enabling-logging) by following the instructions below
7. **Change config properties**
    - Navigate to the following file: `src/main/resources/config.properties`
    - For a simple development setup, skip now to the
      subheading [Dev Config Properties](#program-arguments-for-development)
    - Change the value of the following three properties:
        - `frontend_app.url`
        - `backend_app.url`
        - `backend_app.cas_callback_url`
8. **Run the Autograder Locally**
    - Run your "Server" run configuration
    - Run the frontend by referencing the [section below](#running-locally)
   ```bash
   yarn dev
   ```

#### Program Arguments

For both deployment and development, the following program arguments are required to be set. Some typical
values for development are provided; notice that the URLs all reference localhost, but the port numbers have
been filled in with default values. Update these as needed to match your environment.

```
--db-user <user>
--db-pass <password>
--db-host localhost
--db-port 3306
--db-name autograder
--frontend-url http://localhost:5173
--cas-callback-url http://localhost:8080/auth/callback
--canvas-token <canvas api key>
```

While you can use any root user credentials to access the MySQL database, you may be interested in creating
a special login for this project with restricted privileges. That decision is left to you.

A Canvas Authorization Key is required to run the project. The Autograder currently relies on information from Canvas
to give you admin access to the app (see [#164](https://github.com/softwareconstruction240/autograder/issues/164)).
Generating a Canvas API Key is easy and straight forward:

1. Login to [Canvas](https://byu.instructure.com/)
2. Visit your [Profile Settings](https://byu.instructure.com/profile/settings)
    - Navigate here manually as "Account > Settings"
3. Scroll down to the **Approved Integrations** section
4. Click "+ New Access Token"
5. Provide the requested information in the modal box
6. Copy the generated access token
7. Use it as the value of the `--canvas-token` program argument above

#### Enabling Logging

To enable logging, add arguments `-Dlog4j2.configurationFile=log4j.properties -Dlog4j2.debug=false` as vm options.

In IntelliJ, Go into the run configuration you want to use -> `Modify Options` -> `Add VM options`. This will reveal an
additional box inside of the edit menu. Paste the arguments into the box.

If running from the command line, add the arguments immediately after the `java` command.

#### Running Locally

The frontend can be easily deployed by navigating to the correct directory, and calling an init script.

```bash
cd src/main/resources/frontend
yarn dev
```
