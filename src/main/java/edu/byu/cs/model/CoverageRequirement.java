package edu.byu.cs.model;

public record CoverageRequirement(CoverageType type, String name) {
    public enum CoverageType{
        PACKAGE,
        CLASS
    }
}
