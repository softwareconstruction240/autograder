# Insert `rubric` SQL Statements

This guide is primarily responsible for initializing the `rubric_config` table,
assuming that the table begins in an empty state.

## Updating Guide

The primary things that needs to be updated on an ongoing basis are:
1. Values in the `rubric_id` column

## Statements
> [!NOTE]
> Updated as of **SUMMER 2024**

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
