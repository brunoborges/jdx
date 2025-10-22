package com.jdx.commands;

import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "detect-foreign",
    description = "Detect other JDK managers (jenv, SDKMAN, mise/asdf)"
)
public class DetectForeignCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        List<String> detected = new ArrayList<>();
        
        System.out.println("Detecting other JDK managers...\n");
        
        // Check for jenv
        if (checkJenv()) {
            detected.add("jenv");
            System.out.println("✓ jenv detected");
            String jenvPath = System.getenv("JENV_ROOT");
            if (jenvPath != null) {
                System.out.println("  JENV_ROOT: " + jenvPath);
            }
        }
        
        // Check for SDKMAN
        if (checkSdkman()) {
            detected.add("SDKMAN");
            System.out.println("✓ SDKMAN detected");
            String sdkmanPath = System.getenv("SDKMAN_DIR");
            if (sdkmanPath != null) {
                System.out.println("  SDKMAN_DIR: " + sdkmanPath);
            }
        }
        
        // Check for mise/asdf
        if (checkMise()) {
            detected.add("mise");
            System.out.println("✓ mise detected");
        } else if (checkAsdf()) {
            detected.add("asdf");
            System.out.println("✓ asdf detected");
            String asdfPath = System.getenv("ASDF_DIR");
            if (asdfPath != null) {
                System.out.println("  ASDF_DIR: " + asdfPath);
            }
        }
        
        System.out.println();
        
        if (detected.isEmpty()) {
            System.out.println("No other JDK managers detected.");
            System.out.println("jdx can safely manage your JDKs.");
        } else {
            System.out.println("Detected managers: " + String.join(", ", detected));
            System.out.println("\n⚠  Warning: Multiple JDK managers may conflict.");
            System.out.println("Consider using jdx in 'respect' mode to avoid conflicts.");
            System.out.println("jdx will manage toolchains but not shell activation.");
        }

        return 0;
    }

    private boolean checkJenv() {
        try {
            // Check if jenv command exists
            Process process = new ProcessBuilder("which", "jenv")
                .redirectErrorStream(true)
                .start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }
        
        // Check JENV_ROOT
        return System.getenv("JENV_ROOT") != null;
    }

    private boolean checkSdkman() {
        // Check SDKMAN_DIR
        String sdkmanDir = System.getenv("SDKMAN_DIR");
        if (sdkmanDir != null) {
            Path sdkmanPath = Paths.get(sdkmanDir);
            return Files.exists(sdkmanPath);
        }
        
        // Check default location
        Path defaultSdkman = Paths.get(System.getProperty("user.home"), ".sdkman");
        return Files.exists(defaultSdkman);
    }

    private boolean checkMise() {
        try {
            Process process = new ProcessBuilder("which", "mise")
                .redirectErrorStream(true)
                .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkAsdf() {
        // Check ASDF_DIR
        String asdfDir = System.getenv("ASDF_DIR");
        if (asdfDir != null) {
            Path asdfPath = Paths.get(asdfDir);
            return Files.exists(asdfPath);
        }
        
        // Check default location
        Path defaultAsdf = Paths.get(System.getProperty("user.home"), ".asdf");
        return Files.exists(defaultAsdf);
    }
}
