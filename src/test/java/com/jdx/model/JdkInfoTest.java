package com.jdx.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JdkInfoTest {

    @Test
    void testJdkInfoCreation() {
        JdkInfo jdk = new JdkInfo(
            "temurin-21",
            "21.0.1",
            "Eclipse Adoptium",
            "x86_64",
            "/usr/lib/jvm/temurin-21",
            Set.of("jlink", "jpackage"),
            true
        );

        assertThat(jdk.id()).isEqualTo("temurin-21");
        assertThat(jdk.version()).isEqualTo("21.0.1");
        assertThat(jdk.vendor()).isEqualTo("Eclipse Adoptium");
        assertThat(jdk.arch()).isEqualTo("x86_64");
        assertThat(jdk.path()).isEqualTo("/usr/lib/jvm/temurin-21");
        assertThat(jdk.capabilities()).containsExactlyInAnyOrder("jlink", "jpackage");
        assertThat(jdk.valid()).isTrue();
    }

    @Test
    void testHasCapability() {
        JdkInfo jdk = new JdkInfo(
            "temurin-17",
            "17.0.9",
            "Eclipse Adoptium",
            "x86_64",
            "/usr/lib/jvm/temurin-17",
            Set.of("jlink"),
            true
        );

        assertThat(jdk.hasCapability("jlink")).isTrue();
        assertThat(jdk.hasCapability("jpackage")).isFalse();
        assertThat(jdk.hasCapability("nonexistent")).isFalse();
    }

    @Test
    void testHasCapabilityWithNullCapabilities() {
        JdkInfo jdk = new JdkInfo(
            "oracle-11",
            "11.0.21",
            "Oracle Corporation",
            "x86_64",
            "/usr/lib/jvm/oracle-11",
            null,
            true
        );

        assertThat(jdk.hasCapability("jlink")).isFalse();
    }

    @Test
    void testEquality() {
        JdkInfo jdk1 = new JdkInfo(
            "temurin-21",
            "21.0.1",
            "Eclipse Adoptium",
            "x86_64",
            "/usr/lib/jvm/temurin-21",
            Set.of("jlink"),
            true
        );

        JdkInfo jdk2 = new JdkInfo(
            "temurin-21",
            "21.0.1",
            "Eclipse Adoptium",
            "x86_64",
            "/usr/lib/jvm/temurin-21",
            Set.of("jlink"),
            true
        );

        assertThat(jdk1).isEqualTo(jdk2);
        assertThat(jdk1.hashCode()).isEqualTo(jdk2.hashCode());
    }

    @Test
    void testInequality() {
        JdkInfo jdk1 = new JdkInfo(
            "temurin-21",
            "21.0.1",
            "Eclipse Adoptium",
            "x86_64",
            "/usr/lib/jvm/temurin-21",
            Set.of("jlink"),
            true
        );

        JdkInfo jdk2 = new JdkInfo(
            "temurin-17",
            "17.0.9",
            "Eclipse Adoptium",
            "x86_64",
            "/usr/lib/jvm/temurin-17",
            Set.of("jlink"),
            true
        );

        assertThat(jdk1).isNotEqualTo(jdk2);
    }

    @Test
    void testInvalidJdk() {
        JdkInfo jdk = new JdkInfo(
            "broken-jdk",
            "unknown",
            "Unknown",
            "x86_64",
            "/invalid/path",
            Set.of(),
            false
        );

        assertThat(jdk.valid()).isFalse();
        assertThat(jdk.capabilities()).isEmpty();
    }
}
