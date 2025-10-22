package com.jdx.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import com.jdx.model.ProjectConfig;
import com.jdx.toolchain.ToolchainManagerImpl;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
    name = "pin",
    description = "Pin JDK versions for project scope"
)
public class PinCommand implements Callable<Integer> {
    
    @Option(names = {"--project"}, description = "Apply to project scope (.jdxrc)", required = true)
    private boolean project;

    @Option(names = {"--runtime"}, description = "Runtime JDK version")
    private String runtime;

    @Option(names = {"--compile"}, description = "Compile target version")
    private String compile;

    @Option(names = {"--vendor"}, description = "Preferred vendor")
    private String vendor;

    @Option(names = {"--dry-run"}, description = "Show what would be done")
    private boolean dryRun;

    @Override
    public Integer call() throws Exception {
        if (runtime == null && compile == null) {
            System.err.println("Error: Specify at least --runtime or --compile");
            return 1;
        }

        JdkCatalogImpl catalog = new JdkCatalogImpl();
        
        // Validate versions exist
        if (runtime != null) {
            List<JdkInfo> matches = catalog.findByVersion(runtime);
            if (matches.isEmpty()) {
                System.err.println("Error: No JDK found for runtime version: " + runtime);
                return 1;
            }
        }

        if (compile != null) {
            List<JdkInfo> matches = catalog.findByVersion(compile);
            if (matches.isEmpty()) {
                System.err.println("Error: No JDK found for compile version: " + compile);
                return 1;
            }
        }

        if (dryRun) {
            System.out.println("[DRY RUN] Would create/update .jdxrc:");
            if (runtime != null) {
                System.out.println("  Runtime version: " + runtime);
            }
            if (compile != null) {
                System.out.println("  Compile target:  " + compile);
            }
            if (vendor != null) {
                System.out.println("  Vendor:          " + vendor);
            }
            return 0;
        }

        // Create or update .jdxrc
        Path jdxrcPath = Paths.get(".jdxrc");
        ProjectConfig config = loadOrCreateProjectConfig(jdxrcPath);
        
        // Update config
        if (runtime != null) {
            config = new ProjectConfig(
                1,
                new ProjectConfig.ProjectSettings(
                    new ProjectConfig.RuntimeSettings(runtime, vendor != null ? vendor : "any"),
                    config.project() != null && config.project().compile() != null ? 
                        config.project().compile() : 
                        new ProjectConfig.CompileSettings(compile != null ? Integer.parseInt(compile) : 17, true)
                ),
                new ProjectConfig.ToolingSettings(true, true, true),
                "This file is maintained by jdx."
            );
        }
        
        if (compile != null) {
            int compileVersion = Integer.parseInt(compile);
            config = new ProjectConfig(
                1,
                new ProjectConfig.ProjectSettings(
                    config.project() != null && config.project().runtime() != null ?
                        config.project().runtime() :
                        new ProjectConfig.RuntimeSettings("21", "any"),
                    new ProjectConfig.CompileSettings(compileVersion, true)
                ),
                new ProjectConfig.ToolingSettings(true, true, true),
                "This file is maintained by jdx."
            );
        }

        // Save .jdxrc
        saveProjectConfig(jdxrcPath, config);
        System.out.println("Created/updated .jdxrc");

        // Configure toolchains if compile is set
        if (compile != null) {
            ToolchainManagerImpl toolchainManager = new ToolchainManagerImpl();
            toolchainManager.configure(config);
            System.out.println("Toolchains configured for compile target: " + compile);
        }

        return 0;
    }

    private ProjectConfig loadOrCreateProjectConfig(Path path) throws IOException {
        if (Files.exists(path)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(path.toFile(), ProjectConfig.class);
        }
        
        // Create default config
        return new ProjectConfig(
            1,
            new ProjectConfig.ProjectSettings(
                new ProjectConfig.RuntimeSettings("21", "any"),
                new ProjectConfig.CompileSettings(17, true)
            ),
            new ProjectConfig.ToolingSettings(true, true, true),
            "This file is maintained by jdx."
        );
    }

    private void saveProjectConfig(Path path, ProjectConfig config) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(path.toFile(), config);
    }
}
