# Insert `config` SQL Statements

This guide is primarily responsible for initializing the `configuration` table,
assuming that the table begins in an empty state.

> [!TIP]
> The configuration table can _more easily_ be configured with programmatic tools built in to the AutoGrader.
> Find these tools in AutoGrader > Config > Update using Canvas.

## Updating Guide

The primary things that needs to be updated on an ongoing basis are:
1. The value of the `COURSE_NUMBER`
2. Each of the assignment numbers


## Statements
> [!NOTE]
> Updated as of **SUMMER 2024**

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
