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

class UseCommandTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private String originalUserHome;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Set up temporary user home with catalog
        originalUserHome = System.getProperty("user.home");
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
    }

    @Test
    void testUseCommandWithValidId() throws Exception {
        UseCommand command = new UseCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("temurin-21");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("/usr/lib/jvm/temurin-21");
        assertThat(output).containsAnyOf("JAVA_HOME", "$env:JAVA_HOME", "set JAVA_HOME");
    }

    @Test
    void testUseCommandWithVersion() throws Exception {
        UseCommand command = new UseCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("17");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("/usr/lib/jvm/temurin-17");
    }

    @Test
    void testUseCommandWithInvalidId() throws Exception {
        UseCommand command = new UseCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("nonexistent");
        
        assertThat(exitCode).isEqualTo(1);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("JDK not found");
    }

    @Test
    void testUseCommandDryRun() throws Exception {
        UseCommand command = new UseCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--dry-run", "temurin-21");
        
        assertThat(exitCode).isEqualTo(0);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("[DRY RUN]");
        assertThat(errorOutput).contains("temurin-21");
    }

    @Test
    void testUseCommandGeneratesActivationScript() throws Exception {
        UseCommand command = new UseCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("temurin-21");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).isNotEmpty();
        // Should contain path reference
        assertThat(output).contains("/usr/lib/jvm/temurin-21");
    }

    @Test
    void testUseCommandWithPersist() throws Exception {
        UseCommand command = new UseCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--persist", "temurin-21");
        
        assertThat(exitCode).isEqualTo(0);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).containsAnyOf("activate.sh", "activate.ps1");
    }
}
