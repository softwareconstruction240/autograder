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

## Getting Started

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

### Program Arguments

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

### Canvas Integration

To link the autograder with Canvas, you will need to generate a Canvas API key and set the autograder to the current 
Course and Assignment ID numbers on Canvas. If you don't need Canvas integration, 
`--canvas-token <canvas api key>` can be replaced with `--use-canvas false`, which mocks Canvas calls. Then you can 
skip this section.

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
7. Use it as the value of the `--canvas-token` program argument above

#### Canvas Data

To get the correct Canvas course ID, go to Canvas and select the current CS 240 course. The URL should
change to something like `https://byu.instructure.com/courses/<course_number>` where `<course_number>` is the number
you need to use inside the `configuration` table. You can set this by logging into the autograder and using the
`Update Manually` button in the `Config` tab. Click that button and modify the `Course Number` input and then
click `Submit`.

As of 'SUMMER 2024', the course number is `26822`.

To ensure that the assignment IDs and rubric IDs/points are synced with Canvas, go
to the `config` tab, select the button `Update using Canvas`, and select `Yes`. If there's a slight
issue, you may need to explore updating the course IDs manually, whether that be directly though the database's 
`configuration` table or by clicking `Update Manually` in the `config` tab.

Additionally, if you want (not required), you can insert values into the `configuration` table manually
(although the step above should do it automatically). [Here](db-insert-statements/insert-config-database.md) is the insert statement.

### Environment Variables

If you are running Loki locally (not required), then you must set the `LOKI_URL` environment variable. The value can be 
either `localhost:3100` (if you are NOT using docker to develop the app) or `loki:3100` (if you are using docker to 
develop the app).

### Loading the Configuration Related Tables

As of right now, you will need to manually insert the correct values into the `rubric_config` table before being
able to run the actual grading on the autograder. [Here](db-insert-statements/insert-rubric-database.md) is an insert statement for that.

#### Updating from the Old Rubric Config Table

If you have the autograder prior to 'SUMMER 2024', [this SQL statement](db-insert-statements/update-rubric-database.md) should do the job of updating
the `rubric_config` table.
