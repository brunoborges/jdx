package com.jdx.model;

/**
 * Represents the project-specific configuration stored in .jdxrc
 */
public record ProjectConfig(
    int version,
    Project project,
    Tooling tooling,
    String notes
) {
    public record Project(Runtime runtime, Compile compile) {}
    
    public record Runtime(String require, String vendor) {}
    
    public record Compile(int release, boolean enforce) {}
    
    public record Tooling(Maven maven, Gradle gradle, boolean ideHint) {}
    
    public record Maven(boolean manageToolchains) {}
    
    public record Gradle(boolean manageToolchainBlock) {}
}
