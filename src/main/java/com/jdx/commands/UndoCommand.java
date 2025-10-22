package com.jdx.commands;

import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
    name = "undo",
    description = "Undo the last jdx operation"
)
public class UndoCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("Undo functionality not yet implemented.");
        System.out.println("This feature will revert the last jdx operation using a change journal.");
        return 0;
    }
}
