package com.jdx.shell;

/**
 * Interface for generating shell-specific activation scripts.
 */
public interface ShellActivation {
    
    /**
     * Generate activation script for the current shell.
     * 
     * @param javaHome The JAVA_HOME path to activate
     * @return Shell-specific commands to set up the JDK
     */
    String generateActivationScript(String javaHome);
    
    /**
     * Generate deactivation script for the current shell.
     * 
     * @return Shell-specific commands to restore previous state
     */
    String generateDeactivationScript();
    
    /**
     * Get the type of shell this activation is for.
     */
    ShellType getShellType();
}
