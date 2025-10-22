package com.jdx.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jdx.model.ProjectConfig;
import com.jdx.toolchain.ToolchainManagerImpl;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@Command(
    name = "verify",
    description = "Verify JDK and toolchain configuration"
)
public class VerifyCommand implements Callable<Integer> {
    
    @Option(names = {"--maven"}, description = "Only verify Maven configuration")
    private boolean maven;

    @Option(names = {"--gradle"}, description = "Only verify Gradle configuration")
    private boolean gradle;

    @Option(names = {"--ide"}, description = "Verify IDE configuration")
    private boolean ide;

    @Override
    public Integer call() throws Exception {
        boolean allOk = true;
        
        System.out.println("Verifying JDK configuration...\n");
        
        // Check java -version
        allOk &= verifyJavaVersion();
        
        // Check javac -version
        allOk &= verifyJavacVersion();
        
        // If .jdxrc exists, verify against it
        Path jdxrcPath = Paths.get(".jdxrc");
        if (Files.exists(jdxrcPath)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ProjectConfig config = mapper.readValue(jdxrcPath.toFile(), ProjectConfig.class);
            
            if (!gradle) {
                allOk &= verifyMaven(config);
            }
            
            if (!maven) {
                allOk &= verifyGradle(config);
            }
            
            if (ide) {
                verifyIDE();
            }
        } else {
            System.out.println("ℹ  No .jdxrc found, skipping project verification");
        }
        
        System.out.println();
        if (allOk) {
            System.out.println("✓ All checks passed");
            return 0;
        } else {
            System.out.println("✗ Some checks failed");
            return 4; // verify failed exit code
        }
    }

    private boolean verifyJavaVersion() {
        try {
            Process process = new ProcessBuilder("java", "-version")
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null) {
                    System.out.println("✓ java -version: " + firstLine);
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("✗ java not found: " + e.getMessage());
            return false;
        }
        return false;
    }

    private boolean verifyJavacVersion() {
        try {
            Process process = new ProcessBuilder("javac", "-version")
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null) {
                    System.out.println("✓ javac -version: " + firstLine);
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("✗ javac not found: " + e.getMessage());
            return false;
        }
        return false;
    }

    private boolean verifyMaven(ProjectConfig config) {
        try {
            Process process = new ProcessBuilder("mvn", "-version")
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String firstLine = reader.readLine();
                if (firstLine != null) {
                    System.out.println("✓ Maven found: " + firstLine);
                    
                    // Check toolchains.xml
                    Path toolchainsPath = Paths.get(System.getProperty("user.home"), ".m2", "toolchains.xml");
                    if (Files.exists(toolchainsPath)) {
                        System.out.println("✓ Maven toolchains.xml exists");
                    } else {
                        System.out.println("✗ Maven toolchains.xml not found");
                        System.out.println("  Run 'jdx pin --project --compile " + config.project().compile().release() + "'");
                        return false;
                    }
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.out.println("ℹ  Maven not installed (skipping Maven checks)");
        }
        return true;
    }

    private boolean verifyGradle(ProjectConfig config) {
        try {
            Process process = new ProcessBuilder("gradle", "-version")
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                boolean found = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Gradle")) {
                        System.out.println("✓ Gradle found: " + line.trim());
                        found = true;
                        break;
                    }
                }
                
                if (found) {
                    // Check for gradle/jdx.gradle
                    Path jdxGradle = Paths.get("gradle", "jdx.gradle");
                    if (Files.exists(jdxGradle)) {
                        System.out.println("✓ Gradle toolchain configuration exists");
                    } else {
                        System.out.println("ℹ  Gradle toolchain not configured");
                        System.out.println("  Run 'jdx pin --project --compile " + config.project().compile().release() + "'");
                    }
                    return found;
                }
            }
        } catch (Exception e) {
            System.out.println("ℹ  Gradle not installed (skipping Gradle checks)");
        }
        return true;
    }

    private void verifyIDE() {
        System.out.println("\nIDE Configuration:");
        System.out.println("  IntelliJ IDEA: Configure Project SDK and Maven/Gradle JDK in Settings");
        System.out.println("  VS Code: Configure java.configuration.runtimes in settings.json");
        System.out.println("  Eclipse: Configure Installed JREs in Preferences");
    }
}
