package com.jdx.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import com.jdx.model.ProjectConfig;
import com.jdx.shell.ShellActivationImpl;
import com.jdx.toolchain.ToolchainManagerImpl;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "apply",
    description = "Apply .jdxrc configuration to current environment"
)
public class ApplyCommand implements Callable<Integer> {
    
    @Option(names = {"--strict"}, description = "Fail if configuration cannot be applied exactly")
    private boolean strict;

    @Override
    public Integer call() throws Exception {
        Path jdxrcPath = Paths.get(".jdxrc");
        
        if (!Files.exists(jdxrcPath)) {
            System.err.println("Error: No .jdxrc file found in current directory");
            if (strict) {
                return 1;
            }
            System.out.println("Run 'jdx pin --project --runtime <version> --compile <version>' to create one.");
            return 0;
        }

        // Load project config
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ProjectConfig config = mapper.readValue(jdxrcPath.toFile(), ProjectConfig.class);
        
        System.out.println("Applying .jdxrc configuration...");
        
        // Find runtime JDK
        String runtimeVersion = config.project().runtime().require();
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        List<JdkInfo> matches = catalog.findByVersion(runtimeVersion);
        
        if (matches.isEmpty()) {
            System.err.println("Error: No JDK found for runtime version: " + runtimeVersion);
            if (strict) {
                return 1;
            }
            System.out.println("Run 'jdx scan' to discover JDKs.");
            return 0;
        }

        JdkInfo runtimeJdk = matches.get(0);
        System.out.println("Runtime JDK: " + runtimeJdk.version() + " at " + runtimeJdk.path());
        
        // Generate activation script
        ShellActivationImpl activation = new ShellActivationImpl();
        String script = activation.generateActivationScript(runtimeJdk);
        System.out.println("\n# Run this command to activate:");
        System.out.println("eval \"$(jdx apply)\"");
        System.out.println("\n# Or manually:");
        System.out.println(script);
        
        // Configure toolchains
        if (config.tooling().maven_manage_toolchains() || config.tooling().gradle_manage_toolchain_block()) {
            ToolchainManagerImpl toolchainManager = new ToolchainManagerImpl();
            toolchainManager.configure(config);
            System.out.println("\nToolchains configured for compile target: " + config.project().compile().release());
        }

        return 0;
    }
}
