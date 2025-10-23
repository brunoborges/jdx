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

class ListCommandTest {

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private String originalUserHome;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        
        // Set up temporary user home with catalog
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
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void testListCommandWithEmptyCatalog() throws Exception {
        ListCommand command = new ListCommand();
        int exitCode = command.call();
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("No JDKs found");
    }

    @Test
    void testListCommandWithJdks() throws Exception {
        // Add some JDKs to catalog
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        catalog.add(new JdkInfo("temurin-21", "21.0.1", "Eclipse Adoptium", "x86_64", "/usr/lib/jvm/temurin-21", Set.of(), true));
        catalog.add(new JdkInfo("temurin-17", "17.0.9", "Eclipse Adoptium", "x86_64", "/usr/lib/jvm/temurin-17", Set.of(), true));
        catalog.save();
        
        ListCommand command = new ListCommand();
        int exitCode = command.call();
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("ID");
        assertThat(output).contains("VERSION");
        assertThat(output).contains("VENDOR");
        assertThat(output).contains("PATH");
        assertThat(output).contains("temurin-21");
        assertThat(output).contains("temurin-17");
    }

    @Test
    void testListCommandTableFormat() throws Exception {
        // Add JDK
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        catalog.add(new JdkInfo("temurin-21", "21.0.1", "Eclipse Adoptium", "x86_64", "/usr/lib/jvm/temurin-21", Set.of(), true));
        catalog.save();
        
        ListCommand command = new ListCommand();
        command.call();
        
        String output = outContent.toString();
        assertThat(output).contains("Total:");
        assertThat(output).contains("JDK(s)");
    }

    @Test
    void testListCommandJsonFormat() throws Exception {
        // Add JDK
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        catalog.add(new JdkInfo("temurin-21", "21.0.1", "Eclipse Adoptium", "x86_64", "/usr/lib/jvm/temurin-21", Set.of(), true));
        catalog.save();
        
        ListCommand command = new ListCommand();
        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("--json");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("[");
        assertThat(output).contains("]");
        assertThat(output).contains("\"id\"");
        assertThat(output).contains("\"version\"");
        assertThat(output).contains("temurin-21");
    }

    @Test
    void testListCommandJsonFormatEmptyCatalog() throws Exception {
        ListCommand command = new ListCommand();
        CommandLine cmd = new CommandLine(command);
        int exitCode = cmd.execute("--json");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("No JDKs found");
    }
}
