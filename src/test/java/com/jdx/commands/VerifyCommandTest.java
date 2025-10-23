package com.jdx.commands;

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

import static org.assertj.core.api.Assertions.assertThat;

class VerifyCommandTest {

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
        
        // Set up temporary user home
        originalUserHome = System.getProperty("user.home");
        Path testHome = tempDir.resolve("test-home-" + System.nanoTime());
        try {
            Files.createDirectories(testHome);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.setProperty("user.home", testHome.toString());
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
    void testVerifyCommandBasic() throws Exception {
        VerifyCommand command = new VerifyCommand();
        int exitCode = command.call();
        
        // Exit code could be 0 or 4 depending on environment
        assertThat(exitCode).isIn(0, 4);
        String output = outContent.toString();
        assertThat(output).contains("Verifying JDK configuration");
    }

    @Test
    void testVerifyCommandChecksJavaVersion() throws Exception {
        VerifyCommand command = new VerifyCommand();
        command.call();
        
        String output = outContent.toString();
        assertThat(output).containsAnyOf("java -version", "java not found");
    }

    @Test
    void testVerifyCommandChecksJavacVersion() throws Exception {
        VerifyCommand command = new VerifyCommand();
        command.call();
        
        String output = outContent.toString();
        assertThat(output).containsAnyOf("javac -version", "javac not found");
    }

    @Test
    void testVerifyCommandWithoutJdxrc() throws Exception {
        VerifyCommand command = new VerifyCommand();
        command.call();
        
        String output = outContent.toString();
        assertThat(output).contains("No .jdxrc found");
    }

    @Test
    void testVerifyCommandWithJdxrc() throws Exception {
        // Create a .jdxrc file in current directory
        Path originalDir = Path.of(System.getProperty("user.dir"));
        Path testDir = tempDir.resolve("project");
        Files.createDirectories(testDir);
        
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
              maven_manage_toolchains: true
              gradle_manage_toolchain_block: true
              ide_hint: true
            notes: "Test"
            """;
        Files.writeString(testDir.resolve(".jdxrc"), jdxrc);
        
        try {
            System.setProperty("user.dir", testDir.toString());
            
            VerifyCommand command = new VerifyCommand();
            command.call();
            
            String output = outContent.toString();
            assertThat(output).doesNotContain("No .jdxrc found");
        } finally {
            System.setProperty("user.dir", originalDir.toString());
        }
    }

    @Test
    void testVerifyCommandMavenFlag() throws Exception {
        VerifyCommand command = new VerifyCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--maven");
        
        assertThat(exitCode).isIn(0, 4);
        String output = outContent.toString();
        assertThat(output).contains("Verifying JDK configuration");
    }

    @Test
    void testVerifyCommandGradleFlag() throws Exception {
        VerifyCommand command = new VerifyCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--gradle");
        
        assertThat(exitCode).isIn(0, 4);
        String output = outContent.toString();
        assertThat(output).contains("Verifying JDK configuration");
    }

    @Test
    void testVerifyCommandIdeFlag() throws Exception {
        VerifyCommand command = new VerifyCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--ide");
        
        assertThat(exitCode).isIn(0, 4);
        String output = outContent.toString();
        assertThat(output).containsAnyOf("IDE Configuration", "Verifying");
    }
}
