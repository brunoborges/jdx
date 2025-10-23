package com.jdx.commands;

import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyCommandTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private String originalUserHome;
    private Path originalUserDir;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Set up temporary user home with catalog
        originalUserHome = System.getProperty("user.home");
        originalUserDir = Path.of(System.getProperty("user.dir"));
        
        Path testHome = tempDir.resolve("test-home-" + System.nanoTime());
        try {
            Files.createDirectories(testHome);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.setProperty("user.home", testHome.toString());
        
        // Create test catalog
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        catalog.add(new JdkInfo("temurin-21", "21.0.1", "Eclipse Adoptium", "x86_64", "/usr/lib/jvm/temurin-21", Set.of(), true));
        catalog.add(new JdkInfo("temurin-17", "17.0.9", "Eclipse Adoptium", "x86_64", "/usr/lib/jvm/temurin-17", Set.of(), true));
        catalog.save();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
        if (originalUserDir != null) {
            System.setProperty("user.dir", originalUserDir.toString());
        }
    }

    @Test
    void testApplyCommandWithoutJdxrc() throws Exception {
        Path projectDir = tempDir.resolve("no-jdxrc");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        ApplyCommand command = new ApplyCommand();
        int exitCode = command.call();
        
        assertThat(exitCode).isIn(0, 1);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("No .jdxrc file found");
    }

    @Test
    void testApplyCommandWithValidJdxrc() throws Exception {
        Path projectDir = tempDir.resolve("valid-project");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        // Create .jdxrc
        String jdxrc = """
            version: 1
            project:
              runtime:
                require: "21"
                vendor: "any"
              compile:
                release: 17
                enforce: true
            tooling:
              maven_manage_toolchains: false
              gradle_manage_toolchain_block: false
              ide_hint: false
            notes: "Test"
            """;
        Files.writeString(projectDir.resolve(".jdxrc"), jdxrc);
        
        ApplyCommand command = new ApplyCommand();
        int exitCode = command.call();
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("Applying .jdxrc configuration");
        assertThat(output).contains("21.0.1");
    }

    @Test
    void testApplyCommandGeneratesActivationScript() throws Exception {
        Path projectDir = tempDir.resolve("script-project");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        // Create .jdxrc
        String jdxrc = """
            version: 1
            project:
              runtime:
                require: "21"
                vendor: "any"
              compile:
                release: 17
                enforce: true
            tooling:
              maven_manage_toolchains: false
              gradle_manage_toolchain_block: false
              ide_hint: false
            notes: "Test"
            """;
        Files.writeString(projectDir.resolve(".jdxrc"), jdxrc);
        
        ApplyCommand command = new ApplyCommand();
        command.call();
        
        String output = outContent.toString();
        assertThat(output).containsAnyOf("JAVA_HOME", "$env:JAVA_HOME", "set JAVA_HOME");
    }

    @Test
    void testApplyCommandWithMissingJdk() throws Exception {
        Path projectDir = tempDir.resolve("missing-jdk");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        // Create .jdxrc with non-existent JDK
        String jdxrc = """
            version: 1
            project:
              runtime:
                require: "99"
                vendor: "any"
              compile:
                release: 17
                enforce: true
            tooling:
              maven_manage_toolchains: false
              gradle_manage_toolchain_block: false
              ide_hint: false
            notes: "Test"
            """;
        Files.writeString(projectDir.resolve(".jdxrc"), jdxrc);
        
        ApplyCommand command = new ApplyCommand();
        int exitCode = command.call();
        
        assertThat(exitCode).isIn(0, 1);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("No JDK found");
    }

    @Test
    void testApplyCommandWithStrictMode() throws Exception {
        Path projectDir = tempDir.resolve("strict-project");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        ApplyCommand command = new ApplyCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--strict");
        
        assertThat(exitCode).isEqualTo(1);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("No .jdxrc file found");
    }

    @Test
    void testApplyCommandFindsRuntimeJdk() throws Exception {
        Path projectDir = tempDir.resolve("runtime-project");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        // Create .jdxrc
        String jdxrc = """
            version: 1
            project:
              runtime:
                require: "17"
                vendor: "any"
              compile:
                release: 17
                enforce: true
            tooling:
              maven_manage_toolchains: false
              gradle_manage_toolchain_block: false
              ide_hint: false
            notes: "Test"
            """;
        Files.writeString(projectDir.resolve(".jdxrc"), jdxrc);
        
        ApplyCommand command = new ApplyCommand();
        int exitCode = command.call();
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("Runtime JDK");
        assertThat(output).contains("17.0.9");
    }
}
