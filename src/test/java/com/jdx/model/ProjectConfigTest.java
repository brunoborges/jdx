package com.jdx.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectConfigTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    @Test
    void testProjectConfigCreation() {
        ProjectConfig.RuntimeSettings runtime = new ProjectConfig.RuntimeSettings("21", "any");
        ProjectConfig.CompileSettings compile = new ProjectConfig.CompileSettings(17, true);
        ProjectConfig.ProjectSettings project = new ProjectConfig.ProjectSettings(runtime, compile);
        ProjectConfig.ToolingSettings tooling = new ProjectConfig.ToolingSettings(true, true, true);
        
        ProjectConfig config = new ProjectConfig(1, project, tooling, "Test config");

        assertThat(config.version()).isEqualTo(1);
        assertThat(config.project().runtime().require()).isEqualTo("21");
        assertThat(config.project().runtime().vendor()).isEqualTo("any");
        assertThat(config.project().compile().release()).isEqualTo(17);
        assertThat(config.project().compile().enforce()).isTrue();
        assertThat(config.tooling().maven_manage_toolchains()).isTrue();
        assertThat(config.tooling().gradle_manage_toolchain_block()).isTrue();
        assertThat(config.tooling().ide_hint()).isTrue();
        assertThat(config.notes()).isEqualTo("Test config");
    }

    @Test
    void testProjectConfigSerialization(@TempDir Path tempDir) throws IOException {
        ProjectConfig config = new ProjectConfig(
            1,
            new ProjectConfig.ProjectSettings(
                new ProjectConfig.RuntimeSettings("21", "temurin"),
                new ProjectConfig.CompileSettings(17, true)
            ),
            new ProjectConfig.ToolingSettings(true, true, false),
            "This file is maintained by jdx."
        );

        Path configFile = tempDir.resolve("test.jdxrc");
        mapper.writeValue(configFile.toFile(), config);

        assertThat(configFile).exists();
        String content = Files.readString(configFile);
        assertThat(content).contains("version: 1");
        assertThat(content).contains("require: \"21\"");
        assertThat(content).contains("vendor: \"temurin\"");
        assertThat(content).contains("release: 17");
    }

    @Test
    void testProjectConfigDeserialization(@TempDir Path tempDir) throws IOException {
        String yaml = """
            version: 1
            project:
              runtime:
                require: "21"
                vendor: "any"
              compile:
                release: 17
                enforce: true
            tooling:
              maven_manage_toolchains: true
              gradle_manage_toolchain_block: true
              ide_hint: true
            notes: "Test configuration"
            """;

        Path configFile = tempDir.resolve("test.jdxrc");
        Files.writeString(configFile, yaml);

        ProjectConfig config = mapper.readValue(configFile.toFile(), ProjectConfig.class);

        assertThat(config.version()).isEqualTo(1);
        assertThat(config.project().runtime().require()).isEqualTo("21");
        assertThat(config.project().runtime().vendor()).isEqualTo("any");
        assertThat(config.project().compile().release()).isEqualTo(17);
        assertThat(config.project().compile().enforce()).isTrue();
        assertThat(config.tooling().maven_manage_toolchains()).isTrue();
        assertThat(config.notes()).isEqualTo("Test configuration");
    }

    @Test
    void testMinimalProjectConfig() {
        ProjectConfig config = new ProjectConfig(
            1,
            new ProjectConfig.ProjectSettings(
                new ProjectConfig.RuntimeSettings("17", "any"),
                new ProjectConfig.CompileSettings(11, false)
            ),
            new ProjectConfig.ToolingSettings(false, false, false),
            null
        );

        assertThat(config.version()).isEqualTo(1);
        assertThat(config.project().runtime().require()).isEqualTo("17");
        assertThat(config.project().compile().release()).isEqualTo(11);
        assertThat(config.tooling().maven_manage_toolchains()).isFalse();
        assertThat(config.notes()).isNull();
    }
}
