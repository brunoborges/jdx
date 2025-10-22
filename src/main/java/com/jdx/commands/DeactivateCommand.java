package com.jdx.commands;

import com.jdx.shell.ShellActivationImpl;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
    name = "deactivate",
    description = "Restore previous JAVA_HOME and PATH"
)
public class DeactivateCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        ShellActivationImpl activation = new ShellActivationImpl();
        String script = activation.generateDeactivationScript();
        
        System.out.println(script);
        
        return 0;
    }
}
