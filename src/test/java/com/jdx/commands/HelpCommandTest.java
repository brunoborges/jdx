package com.jdx.commands;

import com.jdx.JdxMain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class HelpCommandTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void testHelpCommandWithNoArgument() throws Exception {
        JdxMain main = new JdxMain();
        CommandLine mainCmd = new CommandLine(main);
        
        int exitCode = mainCmd.execute("help");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("jdx");
        assertThat(output).contains("Commands:");
    }

    @Test
    void testHelpCommandWithValidCommand() throws Exception {
        JdxMain main = new JdxMain();
        CommandLine mainCmd = new CommandLine(main);
        
        int exitCode = mainCmd.execute("help", "scan");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("scan");
        assertThat(output).contains("JDK");
    }

    @Test
    void testHelpCommandWithInvalidCommand() throws Exception {
        JdxMain main = new JdxMain();
        CommandLine mainCmd = new CommandLine(main);
        
        int exitCode = mainCmd.execute("help", "nonexistent");
        
        assertThat(exitCode).isEqualTo(1);
        String errorOutput = errContent.toString();
        assertThat(errorOutput).contains("Unknown command");
    }

    @Test
    void testHelpCommandShowsAvailableCommands() throws Exception {
        JdxMain main = new JdxMain();
        CommandLine mainCmd = new CommandLine(main);
        
        int exitCode = mainCmd.execute("help");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).containsAnyOf("scan", "list", "use", "pin", "verify");
    }

    @Test
    void testHelpCommandForSpecificCommand() throws Exception {
        JdxMain main = new JdxMain();
        CommandLine mainCmd = new CommandLine(main);
        
        int exitCode = mainCmd.execute("help", "list");
        
        assertThat(exitCode).isEqualTo(0);
        String output = outContent.toString();
        assertThat(output).contains("list");
    }
}
