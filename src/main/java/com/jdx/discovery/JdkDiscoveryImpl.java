package com.jdx.discovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import com.jdx.model.JdkInfo;

/**
 * Implementation of JDK discovery across different operating systems.
 */
public class JdkDiscoveryImpl implements JdkDiscovery {
    
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS.contains("win");
    private static final boolean IS_MAC = OS.contains("mac");
    private static final boolean IS_LINUX = OS.contains("nux");

    @Override
    public List<JdkInfo> scan() {
        Set<JdkInfo> jdks = new LinkedHashSet<>();
        
        if (IS_MAC) {
            jdks.addAll(scanMacOS());
        } else if (IS_WINDOWS) {
            jdks.addAll(scanWindows());
        } else if (IS_LINUX) {
            jdks.addAll(scanLinux());
        }
        
        // Also check JAVA_HOME
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isEmpty()) {
            Path javaHomePath = Paths.get(javaHome);
            if (Files.exists(javaHomePath)) {
                parseJdkInfo(javaHomePath).ifPresent(jdks::add);
            }
        }
        
        return new ArrayList<>(jdks);
    }

    private List<JdkInfo> scanMacOS() {
        List<JdkInfo> jdks = new ArrayList<>();
        
        // Use /usr/libexec/java_home -V
        try {
            Process process = new ProcessBuilder("/usr/libexec/java_home", "-V")
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Parse lines like: "    21.0.5 (arm64) "OpenJDK 21.0.5" - "OpenJDK 21.0.5" /Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home"
                    if (line.trim().isEmpty() || line.contains("Matching")) continue;
                    
                    String[] parts = line.split("\"");
                    if (parts.length >= 2) {
                        String pathPart = line.substring(line.lastIndexOf("\"") + 1).trim();
                        if (Files.exists(Paths.get(pathPart))) {
                            parseJdkInfo(Paths.get(pathPart)).ifPresent(jdks::add);
                        }
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Fall back to standard locations
        }
        
        // Also check standard locations
        Path jvmPath = Paths.get("/Library/Java/JavaVirtualMachines");
        if (Files.exists(jvmPath)) {
            try (Stream<Path> paths = Files.list(jvmPath)) {
                paths.filter(Files::isDirectory)
                    .forEach(jdkDir -> {
                        Path homePath = jdkDir.resolve("Contents/Home");
                        if (Files.exists(homePath)) {
                            parseJdkInfo(homePath).ifPresent(jdks::add);
                        }
                    });
            } catch (IOException e) {
                // Ignore
            }
        }
        
        return jdks;
    }

    private List<JdkInfo> scanWindows() {
        List<JdkInfo> jdks = new ArrayList<>();
        
        // Check common install directories
        String[] programFilesDirs = {
            System.getenv("ProgramFiles"),
            System.getenv("ProgramFiles(x86)"),
            "C:\\Program Files",
            "C:\\Program Files (x86)"
        };
        
        for (String programFiles : programFilesDirs) {
            if (programFiles == null) continue;
            
            // Check Java subdirectory
            Path javaPath = Paths.get(programFiles, "Java");
            if (Files.exists(javaPath)) {
                try (Stream<Path> paths = Files.list(javaPath)) {
                    paths.filter(Files::isDirectory)
                        .forEach(jdkDir -> parseJdkInfo(jdkDir).ifPresent(jdks::add));
                } catch (IOException e) {
                    // Ignore
                }
            }
            
            // Check Microsoft JDK
            Path msJdkPath = Paths.get(programFiles, "Microsoft", "jdk");
            if (Files.exists(msJdkPath)) {
                try (Stream<Path> paths = Files.list(msJdkPath)) {
                    paths.filter(Files::isDirectory)
                        .forEach(jdkDir -> parseJdkInfo(jdkDir).ifPresent(jdks::add));
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        
        // Check PATH for java.exe
        try {
            Process process = new ProcessBuilder("where", "java").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Path javaExe = Paths.get(line.trim());
                    if (Files.exists(javaExe)) {
                        // Go up two directories: bin -> jdk_home
                        Path jdkHome = javaExe.getParent().getParent();
                        parseJdkInfo(jdkHome).ifPresent(jdks::add);
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Ignore
        }
        
        return jdks;
    }

    private List<JdkInfo> scanLinux() {
        List<JdkInfo> jdks = new ArrayList<>();
        
        // Check /usr/lib/jvm
        Path jvmPath = Paths.get("/usr/lib/jvm");
        if (Files.exists(jvmPath)) {
            try (Stream<Path> paths = Files.list(jvmPath)) {
                paths.filter(Files::isDirectory)
                    .forEach(jdkDir -> parseJdkInfo(jdkDir).ifPresent(jdks::add));
            } catch (IOException e) {
                // Ignore
            }
        }
        
        // Check ~/jdks
        String home = System.getProperty("user.home");
        Path userJdksPath = Paths.get(home, "jdks");
        if (Files.exists(userJdksPath)) {
            try (Stream<Path> paths = Files.list(userJdksPath)) {
                paths.filter(Files::isDirectory)
                    .forEach(jdkDir -> parseJdkInfo(jdkDir).ifPresent(jdks::add));
            } catch (IOException e) {
                // Ignore
            }
        }
        
        // Check PATH for java
        try {
            Process process = new ProcessBuilder("which", "-a", "java").start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Path javaExe = Paths.get(line.trim());
                    if (Files.exists(javaExe)) {
                        // Resolve symlinks
                        try {
                            javaExe = javaExe.toRealPath();
                            // Go up two directories: bin -> jdk_home
                            Path jdkHome = javaExe.getParent().getParent();
                            parseJdkInfo(jdkHome).ifPresent(jdks::add);
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            // Ignore
        }
        
        return jdks;
    }

    private Optional<JdkInfo> parseJdkInfo(Path jdkPath) {
        if (!Files.exists(jdkPath)) {
            return Optional.empty();
        }
        
        // Check for release file
        Path releasePath = jdkPath.resolve("release");
        if (!Files.exists(releasePath)) {
            return Optional.empty();
        }
        
        try {
            Properties props = new Properties();
            List<String> lines = Files.readAllLines(releasePath);
            for (String line : lines) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^\"|\"$", "");
                    props.setProperty(key, value);
                }
            }
            
            String version = props.getProperty("JAVA_VERSION", "unknown");
            String vendor = props.getProperty("IMPLEMENTOR", "Unknown");
            String arch = props.getProperty("OS_ARCH", System.getProperty("os.arch"));
            
            // Check for jlink and jpackage capabilities
            boolean hasJlink = Files.exists(jdkPath.resolve("bin/jlink")) || 
                             Files.exists(jdkPath.resolve("bin/jlink.exe"));
            boolean hasJpackage = Files.exists(jdkPath.resolve("bin/jpackage")) || 
                                Files.exists(jdkPath.resolve("bin/jpackage.exe"));
            
            Set<String> capabilities = new HashSet<>();
            if (hasJlink) capabilities.add("jlink");
            if (hasJpackage) capabilities.add("jpackage");
            
            // Generate ID from path - create a meaningful unique identifier
            String id = generateId(jdkPath, version, vendor);
            
            return Optional.of(new JdkInfo(
                id,
                version,
                vendor,
                arch,
                jdkPath.toString(),
                capabilities,
                true
            ));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Generate a unique, meaningful ID for a JDK.
     * Examples:
     * - java-21 (for JDK 21)
     * - java-17.0.9 (for JDK 17.0.9)
     * - java-11-temurin (if multiple JDK 11s exist)
     */
    private String generateId(Path jdkPath, String version, String vendor) {
        // Extract major version for cleaner IDs
        String majorVersion = extractMajorVersion(version);
        
        // Check for common patterns in path that indicate a unique identifier
        String pathStr = jdkPath.toString();
        
        // For SDKMAN paths like ~/.sdkman/candidates/java/21.0.1-tem
        if (pathStr.contains("/.sdkman/")) {
            String[] parts = pathStr.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("java") && i + 1 < parts.length) {
                    return parts[i + 1]; // Return the SDKMAN identifier
                }
            }
        }
        
        // For jenv paths like ~/.jenv/versions/21.0.1
        if (pathStr.contains("/.jenv/")) {
            String[] parts = pathStr.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("versions") && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        }
        
        // For standard paths, use vendor-version pattern
        // Extract short vendor name
        String shortVendor = vendor.toLowerCase()
            .replace("microsoft build of openjdk", "microsoft")
            .replace("eclipse adoptium", "temurin")
            .replace("oracle corporation", "oracle")
            .split(" ")[0];
        
        // Create ID: vendor-majorVersion (e.g., microsoft-21, temurin-17)
        return shortVendor + "-" + majorVersion;
    }
    
    /**
     * Extract major version from full version string.
     * Examples: "21.0.1" -> "21", "17.0.9" -> "17", "1.8.0_392" -> "8"
     */
    private String extractMajorVersion(String version) {
        // Remove quotes if present
        version = version.replaceAll("^\"|\"$", "");
        
        // Handle 1.8 format
        if (version.startsWith("1.8")) {
            return "8";
        }
        
        // Extract first number
        int dotIndex = version.indexOf('.');
        if (dotIndex > 0) {
            return version.substring(0, dotIndex);
        }
        
        // If no dot, return as-is (handle cases like "21")
        return version.split("[^0-9]")[0];
    }
}

