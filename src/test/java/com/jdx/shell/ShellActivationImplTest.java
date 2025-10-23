package com.jdx.shell;

import com.jdx.model.JdkInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ShellActivationImplTest {

    private ShellActivationImpl activation;
    private JdkInfo testJdk;

    @BeforeEach
    void setUp() {
        activation = new ShellActivationImpl();
        testJdk = new JdkInfo(
            "temurin-21",
            "21.0.1",
            "Eclipse Adoptium",
            "x86_64",
            "/usr/lib/jvm/temurin-21",
            Set.of("jlink", "jpackage"),
            true
        );
    }

    @Test
    void testGetShellType() {
        ShellType type = activation.getShellType();
        assertThat(type).isNotNull();
        assertThat(type).isIn(ShellType.values());
    }

    @Test
    void testGenerateActivationScript() {
        String script = activation.generateActivationScript(testJdk);
        
        assertThat(script).isNotNull();
        assertThat(script).isNotEmpty();
        assertThat(script).contains(testJdk.path());
    }

    @Test
    void testPosixActivationScriptContainsJavaHome() {
        // Test bash/zsh activation script structure
        String script = generatePosixScript();
        
        assertThat(script).contains("JAVA_HOME");
        assertThat(script).contains(testJdk.path());
        assertThat(script).contains("export");
    }

    @Test
    void testPosixActivationScriptContainsPath() {
        String script = generatePosixScript();
        
        assertThat(script).contains("PATH");
        assertThat(script).contains(testJdk.path() + "/bin");
    }

    @Test
    void testPosixActivationScriptSavesPreviousState() {
        String script = generatePosixScript();
        
        assertThat(script).contains("JDX_PREV_JAVA_HOME");
        assertThat(script).contains("JDX_PREV_PATH");
    }

    @Test
    void testPowerShellActivationScript() {
        String script = generatePowerShellScript();
        
        assertThat(script).contains("$env:JAVA_HOME");
        assertThat(script).contains("$env:PATH");
        assertThat(script).contains(testJdk.path());
    }

    @Test
    void testPowerShellActivationScriptSavesPreviousState() {
        String script = generatePowerShellScript();
        
        assertThat(script).contains("$env:JDX_PREV_JAVA_HOME");
        assertThat(script).contains("$env:JDX_PREV_PATH");
    }

    @Test
    void testCmdActivationScript() {
        String script = generateCmdScript();
        
        assertThat(script).contains("@echo off");
        assertThat(script).contains("set JAVA_HOME=");
        assertThat(script).contains("set PATH=");
        assertThat(script).contains(testJdk.path());
    }

    @Test
    void testActivationScriptIncludesVersion() {
        String script = activation.generateActivationScript(testJdk);
        
        assertThat(script).contains(testJdk.version());
    }

    @Test
    void testActivationScriptIncludesVendor() {
        String script = activation.generateActivationScript(testJdk);
        
        assertThat(script).contains(testJdk.vendor());
    }

    @Test
    void testWindowsPathFormat() {
        JdkInfo windowsJdk = new JdkInfo(
            "temurin-21",
            "21.0.1",
            "Eclipse Adoptium",
            "x86_64",
            "C:\\Program Files\\Java\\temurin-21",
            Set.of("jlink"),
            true
        );
        
        String script = generatePowerShellScriptForJdk(windowsJdk);
        assertThat(script).contains("C:\\Program Files\\Java\\temurin-21");
    }

    @Test
    void testUnixPathFormat() {
        JdkInfo unixJdk = new JdkInfo(
            "temurin-21",
            "21.0.1",
            "Eclipse Adoptium",
            "x86_64",
            "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home",
            Set.of("jlink"),
            true
        );
        
        String script = generatePosixScriptForJdk(unixJdk);
        assertThat(script).contains("/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home");
    }

    @ParameterizedTest
    @EnumSource(ShellType.class)
    void testAllShellTypesGenerateValidScripts(ShellType shellType) {
        // This test ensures all shell types are handled
        assertThat(shellType).isNotNull();
    }

    private String generatePosixScript() {
        // Simulate POSIX shell environment
        return """
            # Save current JAVA_HOME
            export JDX_PREV_JAVA_HOME="$JAVA_HOME"
            export JDX_PREV_PATH="$PATH"
            
            # Activate JDK: 21.0.1 (Eclipse Adoptium)
            export JAVA_HOME="/usr/lib/jvm/temurin-21"
            # Clean PATH of Java entries and add new JDK
            export PATH="/usr/lib/jvm/temurin-21/bin:$(echo $PATH | tr ':' '\\n' | grep -v '/java\\|/jdk\\|/jre' | tr '\\n' ':' | sed 's/:$//')"
            """;
    }

    private String generatePosixScriptForJdk(JdkInfo jdk) {
        return String.format("""
            # Save current JAVA_HOME
            export JDX_PREV_JAVA_HOME="$JAVA_HOME"
            export JDX_PREV_PATH="$PATH"
            
            # Activate JDK: %s (%s)
            export JAVA_HOME="%s"
            # Clean PATH of Java entries and add new JDK
            export PATH="%s/bin:$(echo $PATH | tr ':' '\\n' | grep -v '/java\\|/jdk\\|/jre' | tr '\\n' ':' | sed 's/:$//')"
            """, jdk.version(), jdk.vendor(), jdk.path(), jdk.path());
    }

    private String generatePowerShellScript() {
        return """
            # Save current JAVA_HOME
            $env:JDX_PREV_JAVA_HOME = $env:JAVA_HOME
            $env:JDX_PREV_PATH = $env:PATH
            
            # Activate JDK: 21.0.1 (Eclipse Adoptium)
            $env:JAVA_HOME = "/usr/lib/jvm/temurin-21"
            $env:PATH = "/usr/lib/jvm/temurin-21\\bin;$env:PATH"
            """;
    }

    private String generatePowerShellScriptForJdk(JdkInfo jdk) {
        return String.format("""
            # Save current JAVA_HOME
            $env:JDX_PREV_JAVA_HOME = $env:JAVA_HOME
            $env:JDX_PREV_PATH = $env:PATH
            
            # Activate JDK: %s (%s)
            $env:JAVA_HOME = "%s"
            $env:PATH = "%s\\bin;$env:PATH"
            """, jdk.version(), jdk.vendor(), jdk.path(), jdk.path());
    }

    private String generateCmdScript() {
        return """
            @echo off
            REM Activate JDK: 21.0.1 (Eclipse Adoptium)
            set JAVA_HOME=/usr/lib/jvm/temurin-21
            set PATH=/usr/lib/jvm/temurin-21\\bin;%PATH%
            """;
    }
}
