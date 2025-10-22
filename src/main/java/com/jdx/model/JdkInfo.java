package com.jdx.model;

import java.util.Set;

/**
 * Represents information about a discovered JDK installation.
 */
public record JdkInfo(
    String id,
    String version,
    String vendor,
    String arch,
    String path,
    Set<String> capabilities,
    boolean valid
) {
    public boolean hasCapability(String capability) {
        return capabilities != null && capabilities.contains(capability);
    }
}
