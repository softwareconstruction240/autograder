package edu.byu.cs.canvas;

import edu.byu.cs.properties.ApplicationProperties;

import java.util.Properties;

public class TestMain {
    public static void main(String[] args) throws CanvasException {
        Properties testProperties = new Properties();
        testProperties.setProperty("db-host", "unused");
        testProperties.setProperty("db-port", "unused");
        testProperties.setProperty("db-name", "unused");
        testProperties.setProperty("db-user", "unused");
        testProperties.setProperty("db-pass", "unused");
        testProperties.setProperty("frontend-url", "unused");
        testProperties.setProperty("cas-callback-url", "unused");
        testProperties.setProperty("canvas-token", "7407~yrcdLQbc9D5H7sVzWKQtZHsadE9np7C55AYfAyTOS773tIZAZ8C9kH3Bzi1s6vwr");
        testProperties.setProperty("use-canvas", "true");

        ApplicationProperties.loadProperties(testProperties);
        var hi = new CanvasIntegrationImpl().getAssignmentsForClass();
        System.out.println(hi);
    }
}
