package com.jdx.shell;

import com.jdx.model.JdkInfo;

/**
 * Interface for generating shell-specific activation scripts.
 */
public interface ShellActivation {
    
    /**
     * Generate activation script for the current shell.
     * 
     * @param jdk The JDK to activate
     * @return Shell-specific commands to set up the JDK
     */
    String generateActivationScript(JdkInfo jdk);
    
    /**
     * Get the type of shell this activation is for.
     */
    ShellType getShellType();
}
