package com.jdx.commands;

import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import com.jdx.shell.ShellActivationImpl;
import com.jdx.shell.ShellType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
    name = "use",
    description = "Switch the active JDK for the current shell"
)
public class UseCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "JDK ID or version to activate")
    private String idOrVersion;

    @Option(names = {"--shell"}, description = "Output shell-specific activation script")
    private boolean shell = true;

    @Option(names = {"--persist"}, description = "Write activation to shell profile")
    private boolean persist;

    @Option(names = {"--dry-run"}, description = "Show what would be done without making changes")
    private boolean dryRun;

    @Override
    public Integer call() throws Exception {
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        
        // Try to find by ID first
        Optional<JdkInfo> jdkOpt = catalog.findById(idOrVersion);
        
        // If not found, try by version
        if (jdkOpt.isEmpty()) {
            List<JdkInfo> matches = catalog.findByVersion(idOrVersion);
            if (!matches.isEmpty()) {
                jdkOpt = Optional.of(matches.get(0));
            }
        }

        if (jdkOpt.isEmpty()) {
            System.err.println("JDK not found: " + idOrVersion);
            System.err.println("Run 'jdx list' to see available JDKs.");
            return 1;
        }

        JdkInfo jdk = jdkOpt.get();

        if (dryRun) {
            System.err.println("[DRY RUN] Would activate JDK:");
            System.err.println("  ID:      " + jdk.id());
            System.err.println("  Version: " + jdk.version());
            System.err.println("  Path:    " + jdk.path());
            return 0;
        }

        ShellActivationImpl activation = new ShellActivationImpl();
        String script = activation.generateActivationScript(jdk);

        if (persist) {
            activation.persistActivation(jdk);
            System.err.println("Activation script written to ~/.jdx/activate.sh");
            System.err.println("Add 'source ~/.jdx/activate.sh' to your shell profile to make it permanent.");
        }

        // Output the activation script
        System.out.println(script);

        return 0;
    }
}
