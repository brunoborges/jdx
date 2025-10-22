package com.jdx;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main entry point for the jdx CLI application.
 */
@Command(
    name = "jdx",
    mixinStandardHelpOptions = true,
    version = "jdx 0.1.0",
    description = "JDK Management CLI - Discover, manage, and switch JDKs",
    subcommands = {
        // Subcommands will be added here as we implement them
    }
)
public class JdxMain implements Runnable {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JdxMain()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("jdx - JDK Management CLI");
        System.out.println("Use --help to see available commands");
    }
}
