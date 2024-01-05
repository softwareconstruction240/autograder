package edu.byu.cs.controller;

import com.google.gson.GsonBuilder;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import spark.Route;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SubmissionController {

    /**
     * A queue of netIds that are waiting to be graded
     */
    static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    /**
     * A map of netIds to sessions that are subscribed to updates for that netId
     */
    static final ConcurrentHashMap<String, List<Session>> sessions = new ConcurrentHashMap<>();

    public static Route submitPost = (req, res) -> {

        User user = req.session().attribute("user");
        String netId = user.netId();

        if (queue.stream().anyMatch(netId::equals)) {
            halt(400, "You are already in the queue");
            return null;
        }

        GradeRequest request;
        try {
            request = new Gson().fromJson(req.body(), GradeRequest.class);
        } catch (JsonSyntaxException e) {
            halt(400, "Request must be valid json");
            return null;
        }

        if (request == null) {
            halt(400, "Request is invalid");
            return null;
        }

        // FIXME: improve git url validation
//        if (!request.repoUrl().matches("^https://[\\w.]+.\\w+/[\\w\\D]+/[\\w-/]+.git$")) {
//            halt(400, "That doesn't look like a valid git url");
//            return;
//        }
        if (!Arrays.asList(0, 1, 3, 4, 6).contains(request.phase())) {
            halt(400, "Valid phases are 0, 1, 3, 4, or 6");
            return null;
        }

        queue.add(netId);
        sessions.put(netId, new ArrayList<>());

        try {
            Grader grader = getGrader(netId, request);

            TrafficController.getInstance().addGrader(grader);

        } catch (Exception e) {
            halt(500, "Something went wrong");
        }

        res.status(200);
        return "";
    };

    public static Route submissionXGet = (req, res) -> {
        String phase = req.params(":phase");
        Phase phaseEnum = switch (phase) {
            case "0" -> Phase.Phase0;
            case "1" -> Phase.Phase1;
            case "3" -> Phase.Phase3;
            case "4" -> Phase.Phase4;
            case "6" -> Phase.Phase6;
            default -> null;
        };

        if (phaseEnum == null) {
            res.status(400);
            return "Invalid phase";
        }

        User user = req.session().attribute("user");

        Collection<Submission> submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(user.netId(), phaseEnum);

        res.status(200);
        res.type("application/json");

        return new GsonBuilder()
                .registerTypeAdapter(Instant.class, new Submission.InstantAdapter())
                .create().toJson(submissions);
    };
}
