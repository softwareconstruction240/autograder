# Autograder Config Info

The autograder has a bunch of settings that change from time to time, but need to persist across reboots. For instance, whether a phase is enabled for student submissions, or the Canvas Course ID. Thats where the Autograder Config system comes in.

The `Configuration` table in the database is a set of key value pairs. This gives us a lot of flexibility, but also the need to define how it will be used. That is what this file is for.

### Live Phases
The `STUDENT_SUBMISSION_ENABLED` Configuration enum has a value of an array of phases. Each phase in the array is enabled, the rest are not.
