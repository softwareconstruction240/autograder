# Class Entity Relationships

This Entity Relationship Diagram (ERD) represents the database schema for the **CS 240 AutoGrader** project, an automated system for managing student submissions, evaluating code, and assigning grades.

## Entity Overview

The system tracks users, including students and administrators, through the `USER` table, where each user is uniquely identified by their `net_id`. When a student submits their code, the submission details—including repository information, commit data, and grading results—are stored in the `SUBMISSION` table. The `QUEUE` table manages submissions awaiting evaluation, ensuring that each student's work is processed in the order it was received.

To support grading consistency, the `RUBRIC_CONFIG` table defines grading criteria, associating different grading components with specific phases of the submission process. The `CONFIGURATION` table stores system-wide settings, allowing administrators to fine-tune the AutoGrader’s behavior remotely. Additionally, the `REPO_UPDATE` table logs updates to repositories, tracking changes made by both students and administrators.

For details on the `commit_context`, `commit_result`, and `verification` fields in the `SUBMISSION` table, refer to the [Git Commit Verification documentation](./git-commit-verification.md).

More details about the `CONFIGURATION` table will be available soon. Stay tuned!

## Diagram

```mermaid
erDiagram

USER {
    string net_id PK
    int canvas_user_id
    string first_name
    string last_name
    string repo_url
    string role
}

SUBMISSION {
    int id PK
    string net_id FK
    string repo_url
    string head_hash
    datetime timestamp
    string phase
    bool passed
    float score
    float raw_score
    text notes
    json rubric
    string verified_status
    json commit_context
    json commit_result
    json verification
    bool admin
}

QUEUE {
    string net_id PK, FK
    string phase
    datetime time_added
    bool started
}

RUBRIC_CONFIG {
    string phase PK
    string type PK
    text category
    text criteria
    int points
    string rubric_id
}

CONFIGURATION {
    string config_key PK
    text value
}

REPO_UPDATE {
    timestamp timestamp PK
    string net_id FK
    string repo_url
    bool admin_update
    string admin_net_id
}

USER ||--o{ SUBMISSION : submits
USER ||--o| QUEUE : queues
USER ||--o{ REPO_UPDATE : updates
```
