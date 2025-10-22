package com.jdx.discovery;

import com.jdx.model.JdkInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
            
            // Generate ID from path (use last directory name)
            String id = jdkPath.getFileName().toString();
            
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
}
