package edu.byu.cs.controller;

import com.google.gson.Gson;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import spark.Route;

import java.util.Collection;
import java.util.Map;

public class SubmissionController {

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
        return new Gson().toJson(
                Map.of(
                "submissions", submissions
                )
        );
    };
}
