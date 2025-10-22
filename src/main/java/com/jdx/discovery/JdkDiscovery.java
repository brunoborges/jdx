package com.jdx.discovery;

import com.jdx.model.JdkInfo;

import java.util.List;

/**
 * Interface for discovering JDK installations on the system.
 */
public interface JdkDiscovery {
    
    /**
     * Scan the system for installed JDKs.
     * 
     * @return List of discovered JDK installations
     */
    List<JdkInfo> scan();
}
