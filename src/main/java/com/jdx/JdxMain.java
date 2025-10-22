package com.jdx;

import com.jdx.commands.*;
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
        ScanCommand.class,
        ListCommand.class,
        InfoCommand.class,
        UseCommand.class,
        DeactivateCommand.class,
        PinCommand.class,
        ApplyCommand.class,
        VerifyCommand.class,
        UndoCommand.class,
        DetectForeignCommand.class,
        ConfigCommand.class,
        DoctorCommand.class
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
