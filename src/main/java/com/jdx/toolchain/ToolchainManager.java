package com.jdx.toolchain;

/**
 * Interface for managing Maven and Gradle toolchains.
 */
public interface ToolchainManager {
    
    /**
     * Configure toolchains for the specified JDK version.
     * 
     * @param jdkVersion The JDK version to configure
     * @param javaHome The JAVA_HOME path for the JDK
     */
    void configure(String jdkVersion, String javaHome);
    
    /**
     * Verify that toolchains are correctly configured.
     * 
     * @return true if toolchains are valid, false otherwise
     */
    boolean verify();
}
