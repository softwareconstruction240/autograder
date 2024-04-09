# README: AutoGrader Sql DAO's

## What's normal about this project?

This project has SQL DataAcessObjects (DAO's) for production and memory DAO's for testing, 
just as is common in production systems. These separate DAO's are coordinated and sychronized
by a set of interfaces. There is one interface for each table, and then multiple implementations 
of each interface. The interfaces are defined in `../` and the DAO's stored in separate folders
by DAO type (memory or SQL).

## What's __unique__ about this project?

1. **We built an innovative and unique system for our SQL DAO's.**
    - It performs all the same processes that are performed when writing JDBC code manually, 
    except that the boilerplate code has been encapsulated in appropriate classes.
    - This streamlines the process of writing new DAO methods and reduces duplicate of the column names.
    - See [softwareconstruction240/autograder#284](https://github.com/softwareconstruction240/autograder/pull/284) for more info.
    - Writing full SQL code is still valid, but you can use helpful methods from the `SqlReader` class. 
   Below are some of our favorites. You can read about their call signatures, behaviors, and overloads in the JavaDoc comments.
   For examples, consult the existing code that uses the methods.
        - `SqlDb#getConnection()`
        - `SqlReader#getTableName()`
        - `SqlReader#selectAllStmt()`
        - `SqlReader#executeQuery(String additionalSqlClauses)`
        - `SqlReader#executeQuery(String additionalSqlClauses, StatementPreparer statmentPreparer)`
        - `SqlReader#insertItem(T item)`
        - `SqlReader#executeQuery(String additionalSqlClauses, StatementPreparer statementPreparer)`
    - Relying on the methods above allows the core of the unique JDBC code to rise quickly to the surface.
    - When writing new queries, it's fine to start with standard JDBC code, and then simplify with our unique methods.
2. **We are considering extending the above system to further streamline SQL statement management.**
   - This would streamline the process required to add/remove new fields to the objects
   - See [softwareconstructino240/autograder#305](https://github.com/softwareconstruction240/autograder/pull/305) for the details.
