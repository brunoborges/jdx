package com.jdx.commands;

import com.jdx.catalog.JdkCatalogImpl;
import com.jdx.model.JdkInfo;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "doctor",
    description = "Check for common problems and suggest fixes"
)
public class DoctorCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        System.out.println("jdx doctor - Checking system configuration...\n");
        
        boolean allOk = true;
        
        allOk &= checkJdxDirectory();
        allOk &= checkCatalog();
        allOk &= checkJavaInPath();
        allOk &= checkJavaHome();
        allOk &= checkMavenToolchains();
        allOk &= checkConflictingManagers();
        
        System.out.println();
        if (allOk) {
            System.out.println("✓ No issues found");
        } else {
            System.out.println("⚠  Some issues detected. Follow the suggestions above to fix them.");
        }
        
        return allOk ? 0 : 1;
    }

    private boolean checkJdxDirectory() {
        Path jdxPath = Paths.get(System.getProperty("user.home"), ".jdx");
        
        if (Files.exists(jdxPath)) {
            System.out.println("✓ ~/.jdx directory exists");
            return true;
        } else {
            System.out.println("✗ ~/.jdx directory not found");
            System.out.println("  Run 'jdx scan' to create it");
            return false;
        }
    }

    private boolean checkCatalog() {
        JdkCatalogImpl catalog = new JdkCatalogImpl();
        List<JdkInfo> jdks = catalog.getAll();
        
        if (jdks.isEmpty()) {
            System.out.println("✗ No JDKs in catalog");
            System.out.println("  Run 'jdx scan' to discover JDKs");
            return false;
        } else {
            System.out.println("✓ Catalog contains " + jdks.size() + " JDK(s)");
            
            // Check for broken JDKs
            long brokenCount = jdks.stream().filter(jdk -> !jdk.valid()).count();
            if (brokenCount > 0) {
                System.out.println("  ⚠  " + brokenCount + " broken JDK(s) detected");
                System.out.println("  Run 'jdx list' to see details");
            }
            
            return true;
        }
    }

    private boolean checkJavaInPath() {
        try {
            Process process = new ProcessBuilder("which", "java")
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String javaPath = reader.readLine();
                if (javaPath != null && !javaPath.isEmpty()) {
                    System.out.println("✓ java found in PATH: " + javaPath);
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Ignore
        }
        
        System.out.println("✗ java not found in PATH");
        System.out.println("  Set JAVA_HOME and add $JAVA_HOME/bin to PATH");
        System.out.println("  Or run: eval \"$(jdx use <version> --shell)\"");
        return false;
    }

    private boolean checkJavaHome() {
        String javaHome = System.getenv("JAVA_HOME");
        
        if (javaHome == null || javaHome.isEmpty()) {
            System.out.println("✗ JAVA_HOME not set");
            System.out.println("  Run: eval \"$(jdx use <version> --shell)\"");
            return false;
        } else {
            Path javaHomePath = Paths.get(javaHome);
            if (Files.exists(javaHomePath)) {
                System.out.println("✓ JAVA_HOME is set: " + javaHome);
                return true;
            } else {
                System.out.println("✗ JAVA_HOME points to non-existent directory: " + javaHome);
                System.out.println("  Run: eval \"$(jdx use <version> --shell)\"");
                return false;
            }
        }
    }

    private boolean checkMavenToolchains() {
        Path toolchainsPath = Paths.get(System.getProperty("user.home"), ".m2", "toolchains.xml");
        
        if (Files.exists(toolchainsPath)) {
            System.out.println("✓ Maven toolchains.xml exists");
            return true;
        } else {
            System.out.println("ℹ  Maven toolchains.xml not found (optional)");
            System.out.println("  Create with: jdx pin --project --compile <version>");
            return true; // Not a failure
        }
    }

    private boolean checkConflictingManagers() {
        boolean hasConflicts = false;
        
        // Check for jenv
        if (System.getenv("JENV_ROOT") != null) {
            System.out.println("⚠  jenv detected (JENV_ROOT is set)");
            hasConflicts = true;
        }
        
        // Check for SDKMAN
        if (System.getenv("SDKMAN_DIR") != null) {
            System.out.println("⚠  SDKMAN detected (SDKMAN_DIR is set)");
            hasConflicts = true;
        }
        
        // Check for asdf
        if (System.getenv("ASDF_DIR") != null) {
            System.out.println("⚠  asdf detected (ASDF_DIR is set)");
            hasConflicts = true;
        }
        
        if (hasConflicts) {
            System.out.println("  Multiple JDK managers may conflict");
            System.out.println("  Run 'jdx detect-foreign' for more details");
            return false;
        } else {
            System.out.println("✓ No conflicting JDK managers detected");
            return true;
        }
    }
}
