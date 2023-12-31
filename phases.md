# Testing steps for each phase

Note, the directory structure of the tests within each phase does not matter, however it will be easiest to just copy in the tests the students are provided into a mostly mirrored directory structure.

## Phase 0
Phase 0 requires a working implementation of the pieces, their rules, and the board. Nothing in the `ChessGame` class is required for this phase.

### Testing
The passoff resources for this phase are in `phases/phase0/`. The structure will look like this:
```
phases/
└── phase0/
    └── passoffTests/
        └── TestFactory.java
        └── chessTests/ 
            ├── ChessBoardTests.java
            ├── ChessMoveTests.java
            ├── ChessPositionTests.java
            └── chessPieceTests/
                └── <tests for each piece>.java
```

All `.java` files within `phase0/passoffTests` are compiled against the student's submission by using the shared artifact generated by maven. This will be located at `shared/target/shared-jar-with-dependencies.jar`. A submitted repo must pass all tests in `passoffTests` to pass the phase.

## Phase 1
Phase 1 requires a working implementation of all chess logic. This will require implementing the classes `ChessBoard`, `ChessGame`, `ChessMove`, `ChessPiece`, and `ChessPosition`

### Testing
The passoff resources for this phase are in `phases/phase1`. The structure will look like this:
```
phases/
└── phase1/
    └── passoffTests/
        ├── TestFactory.java
        └── chessTests/ 
            ├── ChessBoardTests.java
            ├── ChessGameTests.java
            ├── ChessMoveTests.java
            ├── ChessPositionTests.java
            └── chessPieceTests/
                └── <tests for each piece>.java
```


All `.java` files within `phase1/` are compiled against the student's submission by using the shared artifact generated by maven. This will be located at `shared/target/shared-jar-with-dependencies.jar`.

A submitted repo must pass all tests to pass the phase.

## Phase 1 Extra Credit
Phase 1 requires a working implementation of all chess logic, including support for en passant and castling. This will require implementing the classes `ChessBoard`, `ChessGame`, `ChessMove`, `ChessPiece`, and `ChessPosition`

### Testing
The passoff resources for this phase are in `phases/phase1-ec/`. The structure will look like this:
```
phases/
└── phase1-ec/
    └── passoffTests/
        ├── TestFactory.java
        └── chessTests/
            ├── ChessBoardTests.java
            ├── ChessGameTests.java
            ├── ChessMoveTests.java
            ├── ChessPositionTests.java
            └── chessExtraCredit/
            │   ├── CastlingTests.java
            │   └── EnPassantTests.java
            └── chessPieceTests/
                └── <tests for each piece>.java
```


All `.java` files within `phase1-ec/passoffTests` are compiled against the student's submission by using the shared artifact generated by maven. This will be located at `shared/target/shared-jar-with-dependencies.jar`.

A submitted repo must pass all tests to pass the phase.

## Phase 3
Phase 3 requires a working implementation of the server.

### Testing
The passoff resources for this phase are in `phases/phase3/`. The structure will look like this:
```
phases/
└── phase3/
    └── passoffTests/
        └── serverTests/
           └── StandardAPITests.java
```

`StandardAPITests.java` will be compiled against the student's submission by using the server artifact generated by maven. This will be located at `server/target/server-jar-with-dependencies.jar`. These tests will start the server automatically, so the student's submission should not start the server.

A submitted repo must pass all tests to pass the phase.

## Phase 4
Phase 4 requires a working implementation of the server with persistent storage.

### Testing
The passoff resources for this phase are in `phases/phase4/`. The structure will look like this:
```
phases/
└── phase4/
    ├── passoffTests/
    │   └── serverTests/
    │       ├── PersistenceTests.java
    │       └── StandardAPITests.java
    └── resources/
        └── db.properties
```

`db.properties` will replace the student's version of `db.properties` when compiling the student repo. This will allow the tests to use a database operated by the autograder.

`PersistenceTests.java` and `StandardAPITests.java` will be compiled against the student's submission by using the server artifact generated by maven. This will be located at `server/target/server-jar-with-dependencies.jar`.

 A submitted repo must pass all tests to pass the phase.

## Phase 6
Phase 6 requires a working implementation of the shared chess module and the full server with websockets.

### Testing
The passoff resources for this phase are in `phases/phase6/`. The structure will look like this:
```
phases/
└── phase6/
    ├── passoffTests/
    │   └── serverTests/
    │       └── WebSocketTests.java
    └── resources/
        └── db.properties
```

`db.properties` will replace the student's version of `db.properties` when compiling the student repo. This will allow the tests to use a database operated by the autograder.

`WebSocketsTests.java` will be compiled against the student's submission by using the server artifact generated by maven. This will be located at `server/target/server-jar-with-dependencies.jar`.

 A submitted repo must pass all tests to pass the phase.
