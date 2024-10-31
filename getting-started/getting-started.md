# Getting Started

## Pre-requisites

> [!IMPORTANT]
> The autograder unfortunately requires special setup instructions
> in order to operate on **Windows machines**.
>
> Read [Getting Started on Windows](windows.md) before continuing.

### Node/Yarn

The frontend is built using Vue.js. To run the frontend, you will need to have `yarn` (see
[installing yarn](https://yarnpkg.com/getting-started/install)).
After installing `Node` if necessary, run the following to enable `yarn` globally (sudo may be required):

```bash
corepack enable
```

### Backend Database

You can run the database either locally with your own MySQL server or inside a Docker container. To run the database
inside its own Docker container, run the following in the root of the project:

```bash
docker compose up db -d
```

While you can use any root user credentials to access the MySQL database, you may be interested in creating
a special login for this project. That decision is left to you. If you do create a different user, however, it will
need nearly root-level permissions in any case (enough to be able to create a new database, then create a "student"
user and `GRANT ALL` privileges to them for that database). The exact requirements are not listed here.

## Quickstart Instructions

> [!NOTE]
> These instructions will help you set up this project in IntelliJ.\
> If there are any holes or gaps in the instructions, please submit a new pull request\
> to preserve the learned knowledge for future generations.

### 1. Clone the Repo onto your local machine

Any variation of the following will work.
```shell
git clone https://github.com/softwareconstruction240/autograder.git
```

Additionally, you can also leverage Intelli-J's "File > New > New Project from Version Control" 
feature which will do it for you given a GitHub link.

### 2. Use the latest version of the JVM
The system currently requires version **21+**.

Go fish🐟 Specific instructions to update are not included here.

### 3. Install front end dependencies

> [!CAUTION]
> **Windows** If using WSL, run this from an actual WSL terminal.\
> Windows-based shells, even POSIX ones, won't install the correct files.

Use `yarn` to install all dependencies. This must be done from the _front end_ root folder.
 ```bash
 cd src/main/resources/frontend
 yarn
 ```

### 4. Setup "Server" _Run Configuration_

Do the following steps:
1. Navigate to the server file: `src/main/java/edu/byu/cs/server/Server.java`
2. Click the "Run" button to run `Server.main()`
3. This will give you a _Run Configuration_ that you can modify in the following steps.
  

### 5. Set up Program Arguments

For both deployment and development, the following program arguments are required to be set. Some typical
values for development are provided; notice that the URLs all reference localhost, but the port numbers have
been filled in with default values. Update these as needed to match your environment.

Do the following actions:
1. Refer to the required arguments below
2. Fill in the appropriate values for each of the 
3. Prepare the arguments by reformatting them onto a single line
   - Ex. `--arg-1 arg1Val --arg-2 arg2Val`
4. Edit the "Server" _run configuration_
5. Save & apply the changes

#### Required Program Arguments

> [!CAUTION]
> **Windows** users cannot simply use these arguments like users of other platforms can.\
> See [Windows Setup Instructions](./windows.md#setup) for more details.

```shell
--db-user <username>
--db-pass <password>
--db-host localhost
--db-port 3306
--db-name autograder
--frontend-url http://localhost:5173
--cas-callback-url http://localhost:8080/auth/callback

# Use one of the following, but not both
--canvas-token <canvas api key>
--use-canvas false
```

### 6. Run the Autograder Locally

Do the following steps:
1. Run your "Server" run configuration
2. Run the frontend with the following shell commands

> [!NOTE]
> **Windows** In the case you are running on the Autograder inside a dev container,\
> you may need to add the `--host` option at the end of the `yarn dev` command (`yarn dev --host`).
  
```bash
cd src/main/resources/frontend
yarn dev
```

### 7. (Optional) Setup Canvas Integration

> [!TIP]
> This is only required if you hope to transfer data to Canvas during your workflow;\
> otherwise, use the `--use-canvas false` program argument and skip this step.

To link the autograder with Canvas, you will need to generate a Canvas API key and set the autograder to the current
Course and Assignment ID numbers on Canvas.

If you don't need Canvas integration, `--canvas-token <canvas api key>` can be replaced with `--use-canvas false`,
which mocks Canvas calls. Then, you can skip this section.

#### Canvas API Key
A Canvas Authorization Key is required to link the project to Canvas.

To generate a Canvas API key:

1. Login to [Canvas](https://byu.instructure.com/)
2. Visit your [Profile Settings](https://byu.instructure.com/profile/settings)
   - Navigate here manually as "Account > Settings"
3. Scroll down to the **Approved Integrations** section
4. Click "+ New Access Token"
5. Provide the requested information in the modal box
6. Copy the generated access token
7. Add the token along side your other program arguments from [step #5](#required-program-arguments)
   - Add the argument `--canvas-token <canvas api token>`

#### Canvas Data

To update Canvas course ID:
1. Go to Canvas and select the current CS 240 course
2. The URL should change to something like `https://byu.instructure.com/courses/<course_number>`
3. Extract `<course_number>` from the url
   - This is the number that needs to end up in the configuration table
4. You can set this by logging into the autograder and using the `Update Manually` button in the `Config` tab
   1. Click that button and modify the `Course Number` input
   2. Click `Submit`

To ensure that the assignment IDs and rubric IDs/points are synced with Canvas:
1. Go to the `config` tab of the AutoGrader
2. Select the button `Update using Canvas`
3. Select `Yes`

> [!NOTE]
> If there's an issue with the process, you may need to explore updating the course IDs manually.\
> This can be done directly though the database's `configuration` table or 
> by clicking `Update Manually` in the `config` tab.

Additionally, if you want (not required), you can insert values into the `configuration` table manually
(although the step above should do it automatically). See [Insert `config` SQL Statements](db-insert-statements/insert-config-database.md).

### 8. (Optional) Environment Variables

> [!TIP]
> This is only required when running `Loki` locally;\
> otherwise, simply skip this step.

If you are running Loki locally (not required), then you must set the `LOKI_URL` environment variable. The value can be
either `localhost:3100` (if you are NOT using docker to develop the app) or `loki:3100` (if you are using docker to
develop the app).

### 9. Loading the Configuration Related Tables

> [!IMPORTANT]
> If you had the autograder prior to 'SUMMER 2024', there is a separate "update" set of SQL commands to use.\
> See [Update `rubric_config` SQL Statements](db-insert-statements/update-rubric-database.md).

This is required.

As of right now, you will need to manually insert the correct values into the `rubric_config` table before being
able to run the actual grading on the autograder. See [Insert `rubric_config` SQL Statements](db-insert-statements/insert-rubric-database.md).

Insert the commands using a [SQL client](https://github.com/softwareconstruction240/softwareconstruction/blob/main/instruction/mysql/mysql.md#sql-clients)
of your choice on your machine (MySql Shell, SQLWorkbench, or Intelli-J's built in tools).
