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

In addition to these config files, the following environment variables must be set:
```
DB_URL=<host>:<port>
DB_USER=<user>
DB_PASSWORD=<password>
DB_NAME=autograder
```

### Docker
For a docker deployment, run one of the following commands:
```bash
# For a deployment with a MySQL database included
docker compose --profile with-db up -d

# For a deployment requiring an external MySQL database
docker compose up -d
```

Note, if you are using the docker MySQL database, ensure that in `src/main/resources/db.properties` the property `db.url` is set to `db:3306`

## Development
### Running the database
There is a docker compose file in the root of the project that will start a MySQL database. To start the database, run the following command (you will need docker installed):
```bash
docker compose up db -d
```

Alternatively, you can run the database locally with your own MySQL server. Be sure to update `src/main/resources/db.properties` with the correct database url, username, and password.

## Websocket Commands
0. Client joins submit queue by sending a POST to /api/submit
1. Client connects via /ws
2. Client sends a message with their auth token
    ```jwt
    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlBhdWwgaXMgYSBjb29sIGR1ZGUiLCJpYXQiOjE1MTYyMzkwMjJ9.5L4Fjs4D8R1RGDERzyPdSeltqQ_u-n1EeRlBTRspaEI
    ```
3. Server responds with a message with the following format:
    ```json
    {
      "type": "queueStatus",
      "position": 3,
      "total": 4
    }
   ```
4. When the position in the queue changes, new `queueStatus` messages are sent
5. When the client's submission starts to be processed, the server sends a `started` message:
    ```json
    {
      "type": "started"
    }
    ```
6. As the autograder runs, the server sends `update` messages:
    ```json
    {
      "type": "update",
      "message": "Step x is starting"
    }
    ```
7. When the autograder is done, the server sends a `results` message, containing the results of the autograder:
    ```json
    {
      "type": "results",
      "results": "{...}"
    }
    ```
8. If an error occurs, the server sends an `error` message:
    ```json
    {
      "type": "error",
      "message": "Something went wrong"
    }
    ```


## Enabling Logging

To enable logging, add arguments `-Dlog4j2.configurationFile=log4j.properties -Dlog4j2.debug=false` as vm options.

In IntelliJ, Go into the run configuration you want to use -> `Modify Options` -> `Add VM options`. This will reveal an additional box inside of the edit menu. Paste the arguments into the box.

If running from the command line, add the arguments immediately after the `java` command.
