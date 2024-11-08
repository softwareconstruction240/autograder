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

##### Dev Container (recommended for Windows)

You can run all of IntelliJ inside a docker container, which would allow debugging the code on Windows
machines. This requires having docker installed and running on your machine.
To do this, navigate to `.devcontainer/devcontainer.json` in IntelliJ. There should be an icon that pops up next to
the opening curly brace. Click the icon, then select `Create Dev Container and Clone Sources...`. This should pop up
a dialog box that allows you to change a few options about the container. Look through them and change what you need,
then hit the `Build Container and Continue` button. Wait for IntelliJ and Docker to build everything. You may need to
click a few buttons along the way. Eventually a new IntelliJ window will pop up from the dev container. Follow the
[Yarn](#nodeyarn) instructions and [Getting Started](#getting-started) steps below with the new window. 
Use `host.docker.internal` as your db-host argument.

To reopen the container after you've closed it, navigate to the `.devcontainer/devcontainer.json` file again,
click the icon, and select `Show Dev Containers`. Select the container and it should reopen the second IntelliJ.
If nothing appears, make sure the docker engine is running (perhaps by opening Docker Desktop).

##### Other Notes for Windows Development

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
    - Run the frontend by referencing the code below. In the case you are running on the 
      Autograder inside a dev container, you may need to add the `--host` option at the 
      end of the `yarn dev` command (`yarn dev --host`).
   ```bash
   cd src/main/resources/frontend
   yarn dev
   ```
     - Load the configuration related tables by referencing the [section below](#loading-the-configuration-related-tables)

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

#### Loading the Configuration Related Tables

As of right now, you will need to manually insert the correct values into the `rubric_config` table before being
able to run the actual autograding on the autograder. Down below is an `INSERT` statement to do that.

UPDATED AS OF 'SUMMER 2024'. Note that the primary thing that needs to be updated here is the `rubric_id` column.
```mysql
INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id) VALUES
('GitHub', 'GITHUB_REPO', 'GitHub Repository', 'Two Git commits: one for creating the repository and another for `notes.md`.', 15, '_6829'),
('Phase0', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90342_649'),
('Phase0', 'PASSOFF_TESTS', 'Functionality', 'All pass off test cases succeed', 125, '_1958'),
('Phase1', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90342_7800'),
('Phase1', 'PASSOFF_TESTS', 'Functionality', 'All pass off test cases succeed', 125, '_1958'),
('Phase3', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90344_2520'),
('Phase3', 'PASSOFF_TESTS', 'Web API Works', 'All pass off test cases in StandardAPITests.java succeed', 125, '_5202'),
('Phase3', 'QUALITY', 'Code Quality', 'Chess Code Quality Rubric (see GitHub)', 30, '_3003'),
('Phase3', 'UNIT_TESTS', 'Unit Tests', 'All test cases pass\nEach public method on your Service classes has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type', 25, '90344_776'),
('Phase4', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90346_6245'),
('Phase4', 'PASSOFF_TESTS', 'Functionality', 'All pass off test cases succeed', 100, '_2614'),
('Phase4', 'QUALITY', 'Code Quality', 'Chess Code Quality Rubric (see GitHub)', 30, '90346_8398'),
('Phase4', 'UNIT_TESTS', 'Unit Tests', 'All test cases pass\nEach public method on DAO classes has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type', 25, '90346_5755'),
('Phase5', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90347_8497'),
('Phase5', 'QUALITY', 'Code Quality', 'Chess Code Quality Rubric (see GitHub)', 30, '90347_9378'),
('Phase5', 'UNIT_TESTS', 'Unit Tests', 'All test cases pass\nEach public method on the Server Facade class has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type', 25, '90347_2215'),
('Phase6', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90348_9048'),
('Phase6', 'PASSOFF_TESTS', 'Automated Pass Off Test Cases', 'Each provided test case passed is worth a proportional number of points ((passed / total) * 50).', 50, '90348_899'),
('Phase6', 'QUALITY', 'Code Quality', 'Chess Code Quality Rubric (see GitHub)', 30, '90348_3792'),
('Quality', 'QUALITY', 'Code Quality', 'Chess Code Quality Rubric (see GitHub)', 30, NULL);
```

Now you need to get the correct Canvas course ID. Go to Canvas and select the current CS 240 course. The URL should 
change to something like `https://byu.instructure.com/courses/<course_number>` where `<course_number>` is the number 
you need to use inside the `configuration` table. You can set this by logging into the autograder and using the 
`Update Manually` button in the `Config` tab. Click that button and modify the `Course Number` input and then 
click `Submit`.

As of 'SUMMER 2024', the course number is `26822`.

To ensure that the assignment IDs and rubric IDs/points are synced with Canvas, go
to the `config` tab, select the button `Update using Canvas`, and select `Yes`. If there's a slight
issue, you may need to explore updating the course IDs manually, whether that be directly though the database or by
clicking the `Update Manually` in the `config` tab.

---

Additionally, if you want (not required), you can insert values into the `configuration` table manually 
(although the step above should do it automatically).

UPDATED AS OF 'SUMMER 2024'. Note that the primary thing that needs to be updated is the value of the `COURSE_NUMBER`
and each of the assignment numbers.

```mysql
INSERT INTO configuration (config_key, value) VALUES
('BANNER_MESSAGE', ''),
('GITHUB_ASSIGNMENT_NUMBER', 941080),
('PHASE0_ASSIGNMENT_NUMBER', 941084),
('PHASE1_ASSIGNMENT_NUMBER', 941085),
('PHASE3_ASSIGNMENT_NUMBER', 941087),
('PHASE4_ASSIGNMENT_NUMBER', 941088),
('PHASE5_ASSIGNMENT_NUMBER', 941089),
('PHASE6_ASSIGNMENT_NUMBER', 941090),
('COURSE_NUMBER', 26822),
('STUDENT_SUBMISSIONS_ENABLED', '[Phase0, Phase1, Phase3, Phase4, Phase5, Phase6, Quality, GitHub]');
```

#### Environment Variables

If you are running Loki locally (not required), then you must set the following environment variable:
```
LOKI_URL=
```

The value can be either `localhost:3100` (if you are NOT using docker to develop the app) or `loki:3100`
(if you are using docker to develop the app).

##### Updating from the Old Rubric Config Table.

If you have the autograder prior to 'SUMMER 2024', the following SQL should do the job of updating
the `rubric_config` table:

UPDATED AS OF 'SUMMER 2024'
```mysql
ALTER TABLE rubric_config ADD rubric_id VARCHAR(15);
-- could be ten as no rubric ids so far are longer than 10, but just in case
-- we need extra space. It's up to you if you want to change it. I don't mind.
UPDATE rubric_config SET rubric_id = '_6829' WHERE phase = 'GitHub';

UPDATE rubric_config SET rubric_id = '_1958' WHERE phase = 'Phase0' AND type = 'PASSOFF_TESTS';
INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id)
    VALUES ('Phase0', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90342_649');

UPDATE rubric_config SET rubric_id = '_1958' WHERE phase = 'Phase1';
INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id)
    VALUES ('Phase1', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90342_7800');

UPDATE rubric_config SET rubric_id = '_5202' WHERE phase = 'Phase3' and type = 'PASSOFF_TESTS';
UPDATE rubric_config SET rubric_id = '90344_776' WHERE phase = 'Phase3' and type = 'UNIT_TESTS';
UPDATE rubric_config SET rubric_id = '_3003' WHERE phase = 'Phase3' and type = 'QUALITY';
INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id)
    VALUES ('Phase3', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90344_2520');

UPDATE rubric_config SET rubric_id = '_2614' WHERE phase = 'Phase4' and type = 'PASSOFF_TESTS';
UPDATE rubric_config SET rubric_id = '90346_5755' WHERE phase = 'Phase4' and type = 'UNIT_TESTS';
UPDATE rubric_config SET rubric_id = '90346_8398' WHERE phase = 'Phase4' and type = 'QUALITY';
INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id)
    VALUES ('Phase4', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90346_6245');

UPDATE rubric_config SET rubric_id = '90347_2215' WHERE phase = 'Phase5' and type = 'UNIT_TESTS';
UPDATE rubric_config SET rubric_id = '90347_9378' WHERE phase = 'Phase5' and type = 'QUALITY';
INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id)
    VALUES ('Phase5', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90347_8497');

UPDATE rubric_config SET rubric_id = '90348_899' WHERE phase = 'Phase6' and type = 'PASSOFF_TESTS';
UPDATE rubric_config SET rubric_id = '90348_3792' WHERE phase = 'Phase6' and type = 'QUALITY';
INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id)
    VALUES ('Phase6', 'GIT_COMMITS', 'Git Commits', 'Necessary commit amount', 0, '90348_9048');
```
