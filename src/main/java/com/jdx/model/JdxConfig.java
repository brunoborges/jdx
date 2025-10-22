package com.jdx.model;

import java.util.List;

/**
 * Represents the global jdx configuration stored in ~/.jdx/config.yaml
 */
public record JdxConfig(
    CatalogConfig catalog,
    DefaultsConfig defaults,
    SafetyConfig safety,
    TelemetryConfig telemetry
) {
    public record CatalogConfig(int autorefreshDays) {}
    
    public record DefaultsConfig(String runtime, List<String> vendorPreference) {}
    
    public record SafetyConfig(boolean requireConfirmationOnPersist) {}
    
    public record TelemetryConfig(boolean enabled) {}
}
