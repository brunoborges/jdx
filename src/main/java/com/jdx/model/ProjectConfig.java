package com.jdx.model;

/**
 * Represents the .jdxrc project configuration file.
 */
public record ProjectConfig(
    int version,
    ProjectSettings project,
    ToolingSettings tooling,
    String notes
) {
    public record ProjectSettings(
        RuntimeSettings runtime,
        CompileSettings compile
    ) {}
    
    public record RuntimeSettings(
        String require,
        String vendor
    ) {}
    
    public record CompileSettings(
        int release,
        boolean enforce
    ) {}
    
    public record ToolingSettings(
        boolean maven_manage_toolchains,
        boolean gradle_manage_toolchain_block,
        boolean ide_hint
    ) {}
}
