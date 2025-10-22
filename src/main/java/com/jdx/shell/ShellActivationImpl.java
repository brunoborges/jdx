package com.jdx.shell;

import com.jdx.model.JdkInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of shell activation for different shell types.
 */
public class ShellActivationImpl implements ShellActivation {
    
    private static final String JDX_DIR = System.getProperty("user.home") + "/.jdx";

    @Override
    public String generateActivationScript(JdkInfo jdk) {
        ShellType shellType = getShellType();
        
        return switch (shellType) {
            case BASH, ZSH, FISH -> generatePosixActivation(jdk);
            case POWERSHELL -> generatePowerShellActivation(jdk);
            case CMD -> generateCmdActivation(jdk);
        };
    }

    @Override
    public ShellType getShellType() {
        String shell = System.getenv("SHELL");
        if (shell == null) {
            // Windows
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Check if PowerShell
                if (System.getenv("PSModulePath") != null) {
                    return ShellType.POWERSHELL;
                }
                return ShellType.CMD;
            }
            return ShellType.BASH; // Default
        }
        
        if (shell.contains("zsh")) {
            return ShellType.ZSH;
        } else if (shell.contains("fish")) {
            return ShellType.FISH;
        } else {
            return ShellType.BASH;
        }
    }

    private String generatePosixActivation(JdkInfo jdk) {
        StringBuilder sb = new StringBuilder();
        
        // Save current state
        sb.append("# Save current JAVA_HOME\n");
        sb.append("export JDX_PREV_JAVA_HOME=\"$JAVA_HOME\"\n");
        sb.append("export JDX_PREV_PATH=\"$PATH\"\n");
        sb.append("\n");
        
        // Set new JAVA_HOME
        sb.append("# Activate JDK: ").append(jdk.version()).append(" (").append(jdk.vendor()).append(")\n");
        sb.append("export JAVA_HOME=\"").append(jdk.path()).append("\"\n");
        
        // Update PATH - remove old Java paths and add new one
        sb.append("# Clean PATH of Java entries and add new JDK\n");
        sb.append("export PATH=\"").append(jdk.path()).append("/bin");
        
        // Add back non-Java PATH entries
        sb.append(":$(echo $PATH | tr ':' '\\n' | grep -v '/java\\|/jdk\\|/jre' | tr '\\n' ':' | sed 's/:$//')");
        sb.append("\"\n");
        
        return sb.toString();
    }

    private String generatePowerShellActivation(JdkInfo jdk) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# Save current JAVA_HOME\n");
        sb.append("$env:JDX_PREV_JAVA_HOME = $env:JAVA_HOME\n");
        sb.append("$env:JDX_PREV_PATH = $env:PATH\n");
        sb.append("\n");
        
        sb.append("# Activate JDK: ").append(jdk.version()).append(" (").append(jdk.vendor()).append(")\n");
        sb.append("$env:JAVA_HOME = \"").append(jdk.path()).append("\"\n");
        sb.append("$env:PATH = \"").append(jdk.path()).append("\\bin;$env:PATH\"\n");
        
        return sb.toString();
    }

    private String generateCmdActivation(JdkInfo jdk) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("@echo off\n");
        sb.append("REM Activate JDK: ").append(jdk.version()).append(" (").append(jdk.vendor()).append(")\n");
        sb.append("set JAVA_HOME=").append(jdk.path()).append("\n");
        sb.append("set PATH=").append(jdk.path()).append("\\bin;%PATH%\n");
        
        return sb.toString();
    }

    public void persistActivation(JdkInfo jdk) throws IOException {
        Path jdxPath = Paths.get(JDX_DIR);
        if (!Files.exists(jdxPath)) {
            Files.createDirectories(jdxPath);
        }

        ShellType shellType = getShellType();
        String script = generateActivationScript(jdk);
        
        if (shellType == ShellType.BASH || shellType == ShellType.ZSH || shellType == ShellType.FISH) {
            Path activateScript = Paths.get(JDX_DIR, "activate.sh");
            Files.writeString(activateScript, script);
        } else if (shellType == ShellType.POWERSHELL) {
            Path activateScript = Paths.get(JDX_DIR, "activate.ps1");
            Files.writeString(activateScript, script);
        }
    }
}
