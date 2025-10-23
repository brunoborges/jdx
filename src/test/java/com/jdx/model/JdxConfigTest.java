package com.jdx.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdxConfigTest {

    @Test
    void testJdxConfigCreation() {
        JdxConfig.CatalogConfig catalog = new JdxConfig.CatalogConfig(30);
        JdxConfig.DefaultsConfig defaults = new JdxConfig.DefaultsConfig("21", new String[]{"temurin", "oracle"});
        JdxConfig.SafetyConfig safety = new JdxConfig.SafetyConfig(true);
        JdxConfig.TelemetryConfig telemetry = new JdxConfig.TelemetryConfig(false);

        JdxConfig config = new JdxConfig(catalog, defaults, safety, telemetry);

        assertThat(config.catalog().autorefresh_days()).isEqualTo(30);
        assertThat(config.defaults().runtime()).isEqualTo("21");
        assertThat(config.defaults().vendor_preference()).containsExactly("temurin", "oracle");
        assertThat(config.safety().require_confirmation_on_persist()).isTrue();
        assertThat(config.telemetry().enabled()).isFalse();
    }

    @Test
    void testDefaultCatalogConfig() {
        JdxConfig.CatalogConfig catalog = new JdxConfig.CatalogConfig(7);
        assertThat(catalog.autorefresh_days()).isEqualTo(7);
    }

    @Test
    void testDefaultsConfigWithSingleVendor() {
        JdxConfig.DefaultsConfig defaults = new JdxConfig.DefaultsConfig("17", new String[]{"oracle"});
        assertThat(defaults.runtime()).isEqualTo("17");
        assertThat(defaults.vendor_preference()).hasSize(1);
        assertThat(defaults.vendor_preference()[0]).isEqualTo("oracle");
    }

    @Test
    void testSafetyConfigDisabled() {
        JdxConfig.SafetyConfig safety = new JdxConfig.SafetyConfig(false);
        assertThat(safety.require_confirmation_on_persist()).isFalse();
    }

    @Test
    void testTelemetryConfigEnabled() {
        JdxConfig.TelemetryConfig telemetry = new JdxConfig.TelemetryConfig(true);
        assertThat(telemetry.enabled()).isTrue();
    }
}
