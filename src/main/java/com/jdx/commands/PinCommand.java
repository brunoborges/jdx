package com.jdx.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import com.jdx.model.ProjectConfig;
import com.jdx.toolchain.ToolchainManagerImpl;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Command(
    name = "pin",
    description = "Create or update a .jdxrc file pinning runtime and/or compile JDK versions."
)
public class PinCommand implements Callable<Integer> {

    @Option(names = {"--project-dir"}, paramLabel = "DIR", description = "Project directory containing .jdxrc (default: current working directory)")
    private Path projectDir = Paths.get(".");

    @Option(names = {"--runtime"}, description = "Runtime JDK major version (used to RUN the app, tests, tools). Example: 21")
    private String runtime;

    @Option(names = {"--compile"}, description = "Compile target JDK major version (sets language level / bytecode). Example: 17. Should be <= runtime for forward compatibility.")
    private String compile;

    @Option(names = {"--vendor"}, description = "Preferred runtime vendor (defaults to existing or 'any').")
    private String vendor;

    @Option(names = {"--dry-run"}, description = "Show intended changes without writing .jdxrc or toolchains.xml")
    private boolean dryRun;

    @Override
    public Integer call() throws Exception {
        if (runtime == null && compile == null) {
            System.err.println("Error: specify at least one of --runtime or --compile");
            return 1;
        }

        JdkCatalogImpl catalog = new JdkCatalogImpl();

        // Validate provided runtime JDK exists in catalog (if given)
        if (runtime != null) {
            List<JdkInfo> matches = catalog.findByVersion(runtime);
            if (matches.isEmpty()) {
                System.err.println("Error: no JDK found matching runtime version: " + runtime);
                return 1;
            }
        }

        Integer compileVersion = null;
        if (compile != null) {
            try {
                compileVersion = Integer.parseInt(compile);
            } catch (NumberFormatException nfe) {
                System.err.println("Error: --compile must be a numeric major version (e.g. 8, 11, 17, 21). Value: " + compile);
                return 1;
            }
            List<JdkInfo> matches = catalog.findByVersion(compile);
            if (matches.isEmpty()) {
                System.err.println("Error: no JDK found matching compile version: " + compile);
                return 1;
            }
        }

        // Warn if compile target higher than runtime (usually not desired)
        if (runtime != null && compileVersion != null) {
            try {
                int runtimeMajor = Integer.parseInt(runtime);
                if (compileVersion > runtimeMajor) {
                    System.out.println("Warning: compile target (" + compileVersion + ") is higher than runtime (" + runtimeMajor + "). Runtime JDK must be >= target to run the built code.");
                }
            } catch (NumberFormatException ignored) {
                // runtime may contain patch notation; ignore
            }
        }

        Path jdxrcPath = projectDir.resolve(".jdxrc");
        ProjectConfig existing = loadOrCreateProjectConfig(jdxrcPath);

        // Derive new runtime settings
        ProjectConfig.RuntimeSettings runtimeSettings = existing.project() != null ? existing.project().runtime() : new ProjectConfig.RuntimeSettings("21", "any");
        if (runtime != null) {
            runtimeSettings = new ProjectConfig.RuntimeSettings(runtime, vendor != null ? vendor : runtimeSettings.vendor());
        }

        // Derive new compile settings
        ProjectConfig.CompileSettings compileSettings = existing.project() != null ? existing.project().compile() : new ProjectConfig.CompileSettings(17, true);
        if (compileVersion != null) {
            compileSettings = new ProjectConfig.CompileSettings(compileVersion, true);
        }

        ProjectConfig updated = new ProjectConfig(
            1,
            new ProjectConfig.ProjectSettings(runtimeSettings, compileSettings),
            new ProjectConfig.ToolingSettings(true, true, true),
            "This file is maintained by jdx."
        );

        if (dryRun) {
            System.out.println("[DRY RUN] .jdxrc changes for " + projectDir.toAbsolutePath());
            System.out.println("  runtime.require: " + runtimeSettings.require());
            System.out.println("  runtime.vendor:  " + runtimeSettings.vendor());
            System.out.println("  compile.release: " + compileSettings.release());
            System.out.println("  tooling.maven_manage_toolchains: " + updated.tooling().maven_manage_toolchains());
            System.out.println("  tooling.gradle_manage_toolchain_block: " + updated.tooling().gradle_manage_toolchain_block());
            return 0;
        }

        saveProjectConfig(jdxrcPath, updated);
        System.out.println("Created/updated " + jdxrcPath.toAbsolutePath());
        System.out.println("Pinned runtime=" + runtimeSettings.require() + " vendor=" + runtimeSettings.vendor() + ", compile=" + compileSettings.release());

        // Configure toolchains only if compile target set (optimization) but allow existing enforce flag
        if (compileVersion != null) {
            ToolchainManagerImpl toolchainManager = new ToolchainManagerImpl();
            toolchainManager.configure(updated);
            System.out.println("Toolchains configured for compile target: " + compileVersion);
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
        // Ensure parent directories exist if using --project-dir outside CWD
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.writeValue(path.toFile(), config);
    }
}
