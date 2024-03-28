Well right now it’s specifically requesting the most recent successful submission timestamp **in the current phase**. At a minimum, we would need to be requesting the last submission timestamp for any phase.

## Timestamps
Tracking timestamps should align well, except for when the timestamps don’t line up. We mean to measure the **commits** new to the branch since the first submissions. If there is a misalignment between the authorship of their commits and the submission timestamps, then this would lead to issues.

One example of an issue, posed above, is a case where student’s cannot submit for some reason, and then hit the submit button on multiple phases at once. This could happen if the autograder is down, or if they go offline and code in an airplane or on a vacation, or simply don’t have internet. They could locally have all their commits spaced out, but then the autograder would expect the submissions to be interweaved among the commits, which they may not be.

## Local Clocks
This also opens the door to issues relating to the proper setting of a student’s _local clock_ since the timestamps of commits are based on the local time of the computer; this means a student could cheese the system by changing the local time on their computer to a different day before committing to still meet the requirements. One way to verify this is to verify that all commits are authored in a strictly chronological order, _and that the previous head hash is a direct ancestor of the current submission_.

Besides setting their clocks backwards, a student could also set their clock forward to gain extra days. If a student’s clock is set in the future, then they could easily fast forward an extra day before authoring their next commit, and then fast-forward the clock and commit again. This would count as two commits on two days and would get them their credit.

## Advanced `git` commands
However, I just realized that strictly tracking the commit hashes would also not be foolproof since a student could easily rebase their code and force-push their branch. These commits would all be “new” and count towards their history even though they really are not. Likely, no student in this class is even using multiple branches or even know how to `git rebase` as these are certainly advanced commands. Additionally, even if a student knew how to do the commands, they likely won’t realize that this is a vulnerability and a way to cheese the system.

## Recommendation
Based on all the information above, I propose the following:
1. We count only commits towards the commit limit if they were authored after **the most recent timestamp of the commit of any commit that resulted in an accepted submission from any phase**.
2. We throw an error and refuse to grade if we detect any commits that are authored before their parent commit (indicates intentionally messing with the local clock, or cherry-picking commits).
3. We withhold the score as “not qualified” if any of the commits are authored in the future of the server timestamp (indicates messing with the local clock, or it could be a simple mistake).
4. We explicitly preserve a student’s commit verification result for any subsequent submissions on the same phase. I.E. If they pass on their first valid submission, then we’ll continue passing them even if they are resubmitting to fix quality errors etc… And, if they fail on their first submissions, they’ll be able to continue resubmitting, but none of those grades will be submitted to Canvas even if they are resolving quality errors.

### Advantages
1. A student is able to submit multiple times on the autograder with code that fails, and are only held accountable for meeting commit requirements on their first successful submission.
2. A student cannot cheese the system by quickly adding nearly-empty commits after a successful run in order to resubmit without a penalty
3. This definition handles the cases where students don’t submit to the autograder in between phase submissions.
4. It is not vulnerable to simply rebasing commits in order to count new commits.
5. It also protects against simply changing the local clock to count commits as older.
6. Additionally, a student who has their clock set in the future will still be held accountable for authoring new commits.
7. Students cannot resubmit their code to the autograder with an earlier passing commit in order to count more commits as credit for the current phase. If we only evaluated the most recent passing submission, then a student could push their maximum time back by resubmitting with an earlier commit that still passes. Even if the commit lowers results in a lower score (from quality checks etc…), this wouldn't hurt them since we don’t send lower scores to Canvas.

### Shortcomings
1. It does not protect against a student modifying **all the commit timestamps** of their code block by a fixed amount, but if a student is familiar enough with git to do such an advanced operation, they probably deserve to keep their points gained.
2. Additionally, any student who was not intentionally breaking the system but accidentally has their clock set forward for some reason (I’ve done that before) would be prevented from having their submission graded.
3. Students who cherry-pick commits out of chronological order will be prevented from grading.
4. This still assumes that students submit the phases in order (probably safe).
5. If a student resubmits a previous phase unnecessarily, it could result in removing not counting commits for the current phase.
