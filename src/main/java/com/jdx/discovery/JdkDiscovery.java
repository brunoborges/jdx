package com.jdx.discovery;

import java.util.List;

import com.jdx.model.JdkInfo;

/**
 * Service for discovering JDK installations on the system.
 */
public interface JdkDiscovery {
    /**
     * Scan the system for JDK installations in common locations.
     * 
     * @return List of discovered JDKs
     */
    List<JdkInfo> scan();
    
    /**
     * Scan the system for JDK installations with deep search enabled.
     * Searches beyond common locations, including user home directories,
     * /opt, /usr, and other system directories.
     * 
     * @return List of discovered JDKs
     */
    List<JdkInfo> deepScan();
}
