package edu.byu.cs.controller;

import com.google.gson.Gson;
import spark.Route;

import java.io.File;
import java.nio.file.Files;

public class LogsController {
    public static final Route logsGet = (req, res) -> {
        File file = new File("logs");

        if (file.exists() && file.isDirectory()) {
            res.type("application/json");
            res.status(200);

            String[] fileNames = file.list();
            if (fileNames != null) {
                return new Gson().toJson(fileNames);
            }
        }
        res.status(404);
        return "";
    };

    public static final Route logGet = (req, res) -> {
        String fileName = req.params(":log");

        File file = new File("logs", fileName);

        if (file.exists() && file.isFile()) {
            res.type("text");
            res.status(200);

            return Files.readString(file.toPath());
        }
        res.status(404);
        return "";

    };
}
