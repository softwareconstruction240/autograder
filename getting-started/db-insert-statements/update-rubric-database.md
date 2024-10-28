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