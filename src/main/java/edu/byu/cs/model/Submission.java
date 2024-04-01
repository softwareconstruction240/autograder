package edu.byu.cs.model;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;

/**
 * Represents and stores a student submission.
 *
 * @param netId Identifies the student who submitted the code.
 * @param repoUrl Points to the GitHub repo where the code is stored.
 * @param headHash The commit hash at which we graded the submission.
 * @param timestamp The time the submission was handed in.
 * @param phase The phase being graded in this submission.
 * @param passed Signifies that the code passed all the grading tests.
 *               <b>Does NOT signify</b> that the score was approved.
 * @param score <p>The final score assigned to this submission (in points) that will
 *              be sent to the grade-book, including penalties and extra credit.</p>
 *              <p>This field will be updated if the score changes because of
 *              additional penalties or manual corrections; however, this is
 *              <b>not the canonical source of truth</b>.</p>
 *              <p>This serves as convenient reference while within the AutoGrader
 *              for many reasons, but the real source of truth is the grade-book.</p>
 * @param notes Additional notes displayed to the user.
 *              These usually represent the status of their score, or
 *              provide remarks about why a passing score was not given.
 *              These sometimes contain directions to meet with a TA, etc...
 * @param rubric Contains detailed breakdowns of the score.
 * @param admin Flag identifies when the submission was started by an
 *              admin or developer for testing purposes.
 *              Among other behaviors that may be different, these
 *              submissions will never be stored in the grade-book
 * @param verifiedStatus <p>The status of the verification.</p>
 *                       <p>Old submissions will have a `null` value;
 *                       in this case, verification is assumed to equal
 *                       the {@link Submission#passed} field.</p>
 * @param verification Represents the approval of the submission.
 *                     Added only after the submission is approved manually.
 */
public record Submission(
        String netId,
        String repoUrl,
        String headHash,
        Instant timestamp,
        Phase phase,
        Boolean passed,
        Float score,
        String notes,
        Rubric rubric,
        Boolean admin,
        @Nullable VerifiedStatus verifiedStatus,
        @Nullable ScoreVerification verification
) {
    public static class InstantAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter jsonWriter, Instant instant) throws IOException {
            jsonWriter.value(instant.toString());
        }

        @Override
        public Instant read(JsonReader jsonReader) throws IOException {
            return Instant.parse(jsonReader.nextString());
        }
    }

    /**
     * Represents the manual approval of a score after it was withheld.
     * <br>
     * When the submission is approved, this entire record should be added to the `Submission`.
     *
     * @param originalScore The score originally calculated by the server.
     * @param approvingNetId The NetId of the individual who approves the score manually.
     * @param approvedTimestamp The timestamp of the approver approving the score.
     * @param approvedScore The approved score that should go in the grade-book.
     */
    public record ScoreVerification(
             @NonNull Float originalScore,
             @NonNull String approvingNetId,
             @NonNull Instant approvedTimestamp,
             @NonNull Float approvedScore
    ) { }

    public enum VerifiedStatus {
        Unapproved,
        ApprovedAutomatically,
        ApprovedManually,
        PreviouslyManuallyApproved,
    }
}
