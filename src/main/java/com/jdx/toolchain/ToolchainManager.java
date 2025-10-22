package com.jdx.toolchain;

import com.jdx.model.ProjectConfig;

import java.io.IOException;

/**
 * Interface for managing Maven and Gradle toolchains.
 */
public interface ToolchainManager {
    
    /**
     * Configure toolchains based on project configuration.
     * 
     * @param config The project configuration
     */
    void configure(ProjectConfig config) throws IOException;
    
    /**
     * Verify that toolchains are correctly configured.
     * 
     * @param config The project configuration
     * @return true if toolchains are valid, false otherwise
     */
    boolean verify(ProjectConfig config);
}
