package com.jdx;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

class JdxMainTest {

    @Test
    void testMainCommandExists() {
        JdxMain app = new JdxMain();
        CommandLine cmd = new CommandLine(app);
        
        assertThat(cmd.getCommandName()).isEqualTo("jdx");
    }

    @Test
    void testVersionOption() {
        JdxMain app = new JdxMain();
        CommandLine cmd = new CommandLine(app);
        
        int exitCode = cmd.execute("--version");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void testHelpOption() {
        JdxMain app = new JdxMain();
        CommandLine cmd = new CommandLine(app);
        
        int exitCode = cmd.execute("--help");
        assertThat(exitCode).isEqualTo(0);
    }
}
