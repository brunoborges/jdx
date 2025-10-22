package com.jdx.model;

/**
 * Represents information about a discovered JDK installation.
 */
public record JdkInfo(
    String id,
    String version,
    String vendor,
    String architecture,
    String path,
    boolean hasJlink,
    boolean hasJpackage,
    boolean isValid
) {
}
