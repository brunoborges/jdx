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

class PinCommandTest {

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
    void testPinCommandWithRuntimeOnly() throws Exception {
        Path projectDir = tempDir.resolve("project1");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "21");
        
        assertThat(exitCode).isEqualTo(0);
        
        Path jdxrc = projectDir.resolve(".jdxrc");
        assertThat(jdxrc).exists();
        
        String content = Files.readString(jdxrc);
        assertThat(content).contains("require: \"21\"");
    }

    @Test
    void testPinCommandWithCompileOnly() throws Exception {
        Path projectDir = tempDir.resolve("project2");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--compile", "17");
        
        assertThat(exitCode).isEqualTo(0);
        
        Path jdxrc = projectDir.resolve(".jdxrc");
        assertThat(jdxrc).exists();
        
        String content = Files.readString(jdxrc);
        assertThat(content).contains("release: 17");
    }

    @Test
    void testPinCommandWithBothRuntimeAndCompile() throws Exception {
        Path projectDir = tempDir.resolve("project3");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "21", "--compile", "17");
        
        assertThat(exitCode).isEqualTo(0);
        
        Path jdxrc = projectDir.resolve(".jdxrc");
        assertThat(jdxrc).exists();
        
        String content = Files.readString(jdxrc);
        assertThat(content).contains("require: \"21\"");
        assertThat(content).contains("release: 17");
    }

    @Test
    void testPinCommandWithNoArguments() throws Exception {
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute();
        
        assertThat(exitCode).isEqualTo(1);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("specify at least one");
    }

    @Test
    void testPinCommandWithInvalidVersion() throws Exception {
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "99");
        
        assertThat(exitCode).isEqualTo(1);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("no JDK found");
    }

    @Test
    void testPinCommandWithWarningCompileHigherThanRuntime() throws Exception {
        Path projectDir = tempDir.resolve("project4");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "17", "--compile", "21");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("Warning");
    }

    @Test
    void testPinCommandDryRun() throws Exception {
        Path projectDir = tempDir.resolve("project5");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "21", "--dry-run");
        
        assertThat(exitCode).isEqualTo(0);
        
        Path jdxrc = projectDir.resolve(".jdxrc");
        assertThat(jdxrc).doesNotExist();
        
        String output = outContent.toString();
        assertThat(output).contains("[DRY RUN]");
    }

    @Test
    void testPinCommandWithProjectDir() throws Exception {
        Path projectDir = tempDir.resolve("custom-project");
        Files.createDirectories(projectDir);
        
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "21", "--project-dir", projectDir.toString());
        
        assertThat(exitCode).isEqualTo(0);
        
        Path jdxrc = projectDir.resolve(".jdxrc");
        assertThat(jdxrc).exists();
    }

    @Test
    void testPinCommandWithVendor() throws Exception {
        Path projectDir = tempDir.resolve("project6");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "21", "--vendor", "temurin");
        
        assertThat(exitCode).isEqualTo(0);
        
        Path jdxrc = projectDir.resolve(".jdxrc");
        String content = Files.readString(jdxrc);
        assertThat(content).contains("vendor: \"temurin\"");
    }

    @Test
    void testPinCommandUpdatesExistingJdxrc() throws Exception {
        Path projectDir = tempDir.resolve("project7");
        Files.createDirectories(projectDir);
        System.setProperty("user.dir", projectDir.toString());
        
        // Create initial .jdxrc
        String initialJdxrc = """
            version: 1
            project:
              runtime:
                require: "17"
                vendor: "any"
              compile:
                release: 11
                enforce: true
            tooling:
              maven_manage_toolchains: true
              gradle_manage_toolchain_block: true
              ide_hint: true
            notes: "Initial"
            """;
        Files.writeString(projectDir.resolve(".jdxrc"), initialJdxrc);
        
        // Update with pin command
        PinCommand command = new PinCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--runtime", "21");
        
        assertThat(exitCode).isEqualTo(0);
        
        Path jdxrc = projectDir.resolve(".jdxrc");
        String content = Files.readString(jdxrc);
        assertThat(content).contains("require: \"21\"");
    }
}
