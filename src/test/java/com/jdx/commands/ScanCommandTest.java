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

class ScanCommandTest {

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
    void testScanCommand() {
        ScanCommand command = new ScanCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute();
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("Scanning for JDK installations");
    }

    @Test
    void testScanCommandWithDeepFlag() {
        ScanCommand command = new ScanCommand();
        CommandLine cmd = new CommandLine(command);
        
        int exitCode = cmd.execute("--deep");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("deep scan");
    }

    @Test
    void testScanCommandUpdatedCatalog() throws IOException {
        ScanCommand command = new ScanCommand();
        CommandLine cmd = new CommandLine(command);
        
        cmd.execute();
        
        String output = outContent.toString();
        // Should mention catalog update
        assertThat(output).containsAnyOf("Catalog updated successfully", "No JDKs found");
    }

    @Test
    void testScanCommandCatalogPersistence() {
        ScanCommand command = new ScanCommand();
        CommandLine cmd = new CommandLine(command);
        
        cmd.execute();
        
        // Check that catalog file was created
        Path catalogFile = Path.of(System.getProperty("user.home"), ".jdx", "catalog.json");
        if (outContent.toString().contains("Catalog updated successfully")) {
            assertThat(catalogFile).exists();
        }
    }

    @Test
    void testScanCommandOutput() {
        ScanCommand command = new ScanCommand();
        CommandLine cmd = new CommandLine(command);
        
        cmd.execute();
        
        String output = outContent.toString();
        assertThat(output).contains("Scanning for JDK installations");
    }
}
