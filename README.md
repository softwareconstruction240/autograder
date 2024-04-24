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


note: `sudo` may be required

(see [installing `yarn`](https://yarnpkg.com/getting-started/install))

#### Backend

The backend of the app can be run with either `maven`, or by running your own MySQL server.

##### Maven

Go fish🐟 These instructions are not included in this file.

#### Database

You can run the database inside a Docker container, or locally with your own MySQL server.

##### Docker MySQL Server

Run the following in the root of the project to start the db container:

```bash
docker compose up db -d
```

##### Manual MySQL Server

Go fish🐟 These instructions are not included in this file.

##### Notes for Windows Development

The autograder unfortunately won't work directly from Windows, so you must use Docker or WSL for your database.

If you run the autograder from WSL, you can use your normal Windows MySQL for the database. (Or you can install MySQL
for Linux from within WSL, but having both Windows and Linux MySQL servers can cause weird issues.) You can't simply
use `localhost` as the database hostname, however, since that refers to the WSL instance. Running `echo $(hostname)`
from a WSL terminal will tell you what your computer's host name is (ex. `LAPTOP-ABC123`). Appending `.local` to that (
ex. `LAPTOP-ABC123.local`) gives you the hostname that WSL uses to refer to the Windows machine. Use this as
the `--db-host` program argument.

By default, MySQL users have "Limit to Host Matching" set to `localhost`, which does not allow requests coming from the
WSL virtual machine. In MySQL Workbench, you will have to expand this. The easiest way is to change it to `%`, which allows all hostnames (but it is
highly recommended that you do only do this for a new user with restricted privileges rather than using root). Another option is to
use `wsl hostname -I` to determine the WSL instance's IP address and use that, but this IP may change whenever WSL restarts.

### Getting Started

NOTE: These instructions will help you set up this project in IntelliJ.
If there are any holes or gaps in the instructions, please submit a new pull request
to preserve the learned knowledge for future generations.

1. Clone the repo onto your local machine.
2. Ensure that you are using the latest version of JVM. The system currently requires version 21+.
3. Use `yarn` to install all dependencies. This must be done from the _front end_ root folder. (If using WSL, run this
   from an actual WSL terminal. Windows-based shells, even POSIX ones, won't install the correct files.)
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
6. **Run the Autograder Locally**
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
--db-user <username>
--db-pass <password>
--db-host localhost
--db-port 3306
--db-name autograder
--frontend-url http://localhost:5173
--cas-callback-url http://localhost:8080/auth/callback
--canvas-token <canvas api key>
```

While you can use any root user credentials to access the MySQL database, you may be interested in creating
a special login for this project with restricted privileges (DELETE and CREATE USER administrator privileges are
required). That decision is left to you.

A Canvas Authorization Key is required to run the project. The Autograder currently relies on information from Canvas
to give you admin access to the app (see [#164](https://github.com/softwareconstruction240/autograder/issues/164)).
To generate a Canvas API key:

1. Login to [Canvas](https://byu.instructure.com/)
2. Visit your [Profile Settings](https://byu.instructure.com/profile/settings)
    - Navigate here manually as "Account > Settings"
3. Scroll down to the **Approved Integrations** section
4. Click "+ New Access Token"
5. Provide the requested information in the modal box
6. Copy the generated access token
7. Use it as the value of the `--canvas-token` program argument above

#### Environment Variables
If you are running Loki locally (not required), then you must set the following environment variable:
```
LOKI_URL=
```

The value can be either `localhost:3100` (if you are NOT using docker to develop the app) or `loki:3100` (if you are using docker to develop the app).

#### Running Locally

The frontend can be easily deployed by navigating to the correct directory, and calling an init script.

```bash
cd src/main/resources/frontend
yarn dev
```
