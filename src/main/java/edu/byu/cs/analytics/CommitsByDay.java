package edu.byu.cs.analytics;

import java.util.*;

/**
 * Contains the results of a commit analytics parse,
 * including the parameters used to generate it.
 * <br>
 * This data structure is stored alongside ever {@link edu.byu.cs.model.Submission} entry.
 * Therefore, this data model is more concerned about stringified storage space and use in
 * back-end detection of errors than in user presentation. Presenting the information contained
 * within will likely require converting the data into a more friendly format.
 *
 * @param dayMap Represents each of the calendar days,
 *               with the number of commits on that day.
 * @param linearizedCommits The linearized order of all commits evaluated where earlier commits appear sooner.
 *                          This order generally corresponds to author time, but may not match exactly
 *                          when branches and merging occurs.
 * @param linearizedLineChanges A corresponding list to {@link CommitsByDay#linearizedCommits()} which provides
 *                              a single number for every commit representing the number of line changes.
 *                              If the line changes for some commit were never computed, it will be included
 *                              with the value of <b>-1</b>.
 * @param erroringCommits Reports commit hashes that triggered any of the failure conditions,
 *                        grouped by a natural key into the kinds of conditions that they failed.
 *                        This will be empty when there are no erroring commits.
 * @param totalCommits The total number of commits processed, excluding merge commits.
 * @param mergeCommits The total number of merge commits.
 * @param commitsOutOfOrder Reports whether any commits were not authored strictly after their parents.
 * @param commitsInPast Reports whether any commits were found before the tail hash chronologically.
 *                      This flag has been deemed unhelpful and intentionally ignored.
 *                      Therefore, these are NOT counted as erroring commits.
 * @param commitsInFuture Reports whether any commits were found after the end point chronologically.
 * @param commitsBackdated Reports whether any commit was detected to have been backdated.
 * @param commitTimestampsDuplicated Reports where any two commits were discovered to have the same timestamp.
 * @param missingTailHash Reports when a tail hash was expected to exist, but the commit could not be found.
 * @param lowerThreshold The {@link CommitThreshold}, exclusive.
 * @param upperThreshold The {@link CommitThreshold}, inclusive.
 */
public record CommitsByDay(
        Map<String, Integer> dayMap,
        List<String> linearizedCommits,
        List<Integer> linearizedLineChanges,
        Map<String, List<String>> erroringCommits,
        int totalCommits,
        int mergeCommits,
        boolean commitsOutOfOrder,
        boolean commitsInFuture,
        boolean commitsInPast,
        boolean commitsBackdated,
        boolean commitTimestampsDuplicated,
        boolean missingTailHash,
        CommitThreshold lowerThreshold,
        CommitThreshold upperThreshold
) {
    /**
     * Safely retrieves a read-only {@link Collection<String>} of any erroring commits within the <code>groupId</code> group.
     *
     * @param groupId String identifying the group to view
     * @return A non-empty collection of commit hashes corresponding to <code>groupId</code>, or <code>null</code>.
     */
    public Collection<String> getErroringCommitsSet(String groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId should not be null");
        }
        var initialList = erroringCommits.get(groupId);
        if (initialList == null || initialList.isEmpty()) {
            return null;
        }
        return List.copyOf(initialList);
    }
}
