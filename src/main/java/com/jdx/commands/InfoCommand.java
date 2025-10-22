package com.jdx.commands;

import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
    name = "info",
    description = "Show detailed information about a specific JDK"
)
public class InfoCommand implements Callable<Integer> {
    
    @Parameters(index = "0", description = "JDK ID or version")
    private String idOrVersion;

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
        
        System.out.println("JDK Information:");
        System.out.println("================");
        System.out.println("ID:           " + jdk.id());
        System.out.println("Version:      " + jdk.version());
        System.out.println("Vendor:       " + jdk.vendor());
        System.out.println("Architecture: " + jdk.arch());
        System.out.println("Path:         " + jdk.path());
        System.out.println("Status:       " + (jdk.valid() ? "✓ Valid" : "✗ Broken"));
        
        if (!jdk.capabilities().isEmpty()) {
            System.out.println("Capabilities: " + String.join(", ", jdk.capabilities()));
        }
        
        System.out.println("\nTo use this JDK:");
        System.out.println("  eval \"$(jdx use " + jdk.id() + " --shell)\"");
        
        System.out.println("\nEnvironment variables:");
        System.out.println("  export JAVA_HOME=\"" + jdk.path() + "\"");
        System.out.println("  export PATH=\"" + jdk.path() + "/bin:$PATH\"");

        return 0;
    }
}
