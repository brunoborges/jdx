package com.jdx.model;

/**
 * Represents the global jdx configuration stored in ~/.jdx/config.yaml
 */
public record JdxConfig(
    CatalogConfig catalog,
    DefaultsConfig defaults,
    SafetyConfig safety,
    TelemetryConfig telemetry
) {
    public record CatalogConfig(int autorefresh_days) {}
    
    public record DefaultsConfig(String runtime, String[] vendor_preference) {}
    
    public record SafetyConfig(boolean require_confirmation_on_persist) {}
    
    public record TelemetryConfig(boolean enabled) {}
}
