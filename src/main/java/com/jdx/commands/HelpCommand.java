package com.jdx.commands;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@Command(
    name = "help",
    description = "Display help information about jdx commands"
)
public class HelpCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", paramLabel = "<command>", 
                description = "The command to display help for (optional)")
    private String command;

    @CommandLine.ParentCommand
    private Object parent;

    @Override
    public Integer call() throws Exception {
        CommandLine parentCommandLine = new CommandLine(parent);
        
        if (command == null || command.isEmpty()) {
            // Show general help
            parentCommandLine.usage(System.out);
            return 0;
        }

        // Try to find the subcommand
        CommandLine subcommand = parentCommandLine.getSubcommands().get(command);
        
        if (subcommand == null) {
            System.err.println("Unknown command: " + command);
            System.err.println();
            System.err.println("Available commands:");
            parentCommandLine.getSubcommands().keySet().forEach(cmd -> 
                System.err.println("  " + cmd)
            );
            return 1;
        }

        // Display help for the specific subcommand
        subcommand.usage(System.out);
        return 0;
    }
}
