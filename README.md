# 🚀 jdx - Your JDK Management Companion

**Stop fighting with Java versions. Start building.**

`jdx` is a powerful, cross-platform CLI that takes the pain out of managing multiple JDKs. Whether you're juggling projects on Java 8, 11, 17, 21, or 25, `jdx` makes switching between them effortless—while ensuring your builds always use the correct Java version, every time.

## Why jdx?

**The Problem:** Modern Java development means working with multiple JDK versions across different projects. Manually managing `JAVA_HOME`, PATH, Maven toolchains, and Gradle configurations is error-prone and time-consuming. One wrong setting and your build breaks or produces incorrect bytecode.

**The Solution:** `jdx` automatically discovers all JDKs on your system, lets you switch between them with a single command, and configures your build tools to use the right Java version—making your builds reproducible and your workflow smooth.

### Key Benefits

✅ **Auto-Discovery** - Finds all JDKs on Windows, macOS, and Linux  
✅ **Instant Switching** - Change your active JDK with one command  
✅ **Build Tool Integration** - Automatically configures Maven and Gradle toolchains  
✅ **Project-Level Pinning** - Lock projects to specific Java versions  
✅ **Reproducible Builds** - Ensure consistent compilation across machines and CI  
✅ **Safe & Transparent** - Shows exactly what it changes, easy to undo  
✅ **Zero Dependencies** - Self-contained distribution works out of the box

## What is jdx?

`jdx` is a JDK management CLI that:

1. **Discovers** all JDK installations on your machine (including those installed by other tools)
2. **Catalogs** them with version, vendor, architecture, and capabilities
3. **Switches** the active JDK in your current shell without affecting other terminals
4. **Configures** Maven and Gradle to compile with the correct Java target
5. **Verifies** your environment matches project requirements
6. **Pins** project-specific JDK versions in a `.jdxrc` file

Unlike JDK installers, `jdx` doesn't download JDKs—it manages the ones you already have, making them work together harmoniously.

📖 **For complete technical specifications**, see [SPECIFICATION.md](SPECIFICATION.md).

## 📦 Installation

> **Note:** `jdx` requires at least one JDK already installed on your system. It manages existing JDKs—it doesn't install them.

### Option 1: Download Pre-built Distribution (Recommended)

**Coming Soon**: Pre-built distributions will be available for download.

### Option 2: Build from Source

If you want to build `jdx` yourself:

1. **Prerequisites**
   - JDK 25 or later
   - Maven 3.9+
   - Git

2. **Clone and Build**
   ```bash
   git clone https://github.com/brunoborges/jdx.git
   cd jdx
   mvn clean package -Pjlink-runtime
   ```

3. **Extract and Install**

   **macOS/Linux:**
   ```bash
   cd target
   tar -xzf jdx-0.1.0-SNAPSHOT-*.tar.gz
   cd jdx-0.1.0-SNAPSHOT
   
   # Install to /opt/jdx
   sudo mkdir -p /opt/jdx
   sudo cp -r . /opt/jdx/
   
   # Add to PATH (add to ~/.bashrc or ~/.zshrc)
   export PATH="/opt/jdx/bin:$PATH"
   ```

   **Windows:**
   ```powershell
   # Extract the ZIP file to C:\Program Files\jdx
   # Add C:\Program Files\jdx\bin to your PATH
   ```

4. **Verify Installation**
   ```bash
   jdx --version
   jdx --help
   ```

📘 **For detailed build instructions and development setup**, see [CONTRIBUTING.md](CONTRIBUTING.md).

## 🚀 Quick Start

### 1. Discover Your JDKs

First, let `jdx` find all JDKs on your system:

```bash
# Standard scan (checks common locations)
jdx scan

# Deep scan (also checks user directories and non-standard locations)
jdx scan --deep
```

### 2. List Available JDKs

```bash
jdx list
```

Output example:
```
ID         VERSION    VENDOR       PATH
-------------------------------------------------------------------------
java-21    21.0.1     Temurin      /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
java-17    17.0.9     Temurin      /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
java-11    11.0.21    Microsoft    /Library/Java/JavaVirtualMachines/microsoft-11.jdk/Contents/Home
```

### 3. Switch JDK in Current Shell

```bash
# Switch to Java 17
eval "$(jdx use java-17)"

# Verify
java -version
```

### 4. Configure a Project

Navigate to your Java project and pin JDK versions:

```bash
cd /path/to/your/project

# Pin runtime (JDK to run the build) and compile target (bytecode version)
jdx pin --runtime 21 --compile 17

# Apply configuration (updates Maven toolchains, Gradle settings)
jdx apply

# Verify setup
jdx verify

# Build with correct JDK
mvn clean install
```

This creates a `.jdxrc` file that can be committed to your repository, ensuring all developers and CI use the same Java versions.

## 💡 Real-World Usage Examples

### Switching JDKs for Different Projects

```bash
# Working on a legacy Java 8 project
cd ~/projects/legacy-app
eval "$(jdx use java-8)"
mvn clean package

# Switch to modern Java 21 project
cd ~/projects/modern-app
eval "$(jdx use java-21)"
mvn clean package
```

### Setting Up a New Team Member

```bash
# New contributor clones the repo
git clone https://github.com/company/project.git
cd project

# Project has .jdxrc committed - just apply it!
jdx apply

# Verify environment is correct
jdx verify

# Start building
mvn clean install
```

### Ensuring Reproducible Builds

```bash
# Developer creates project configuration
jdx pin --runtime 21 --compile 17

# Commit .jdxrc to version control
git add .jdxrc
git commit -m "Add JDK configuration"

# CI server applies the same config
jdx apply --strict
jdx verify || exit 1
```

### Building for Different Java Versions

```bash
# Compile for Java 8 while running on Java 17
jdx pin --runtime 17 --compile 8
jdx apply
mvn clean package

# Result: bytecode compatible with Java 8
```

### Checking Your Setup

```bash
# Run diagnostics
jdx doctor

# Output shows potential issues:
# ✓ ~/.jdx directory exists
# ✓ Catalog contains 5 JDK(s)
# ✓ java found in PATH
# ✓ JAVA_HOME is set
# ✓ Maven toolchains.xml exists
# ✓ All checks passed
```

## 📚 Command Reference

### Core Commands

| Command | Description |
|---------|-------------|
| `jdx scan [--deep]` | Discover and catalog all JDKs on your machine |
| `jdx list [--json]` | List all discovered JDKs |
| `jdx info <id>` | Show detailed information about a specific JDK |
| `jdx use <id>` | Generate shell activation script for a JDK |
| `jdx doctor` | Check system configuration and diagnose issues |

### Project Configuration

| Command | Description |
|---------|-------------|
| `jdx pin --runtime <ver> [--compile <ver>]` | Pin JDK versions for a project (creates `.jdxrc`) |
| `jdx apply` | Apply `.jdxrc` configuration to current environment |
| `jdx verify` | Verify JDK and toolchain configuration |

### Advanced Commands

| Command | Description |
|---------|-------------|
| `jdx config [get\|set] <key> [value]` | Get or set global configuration |
| `jdx detect-foreign` | Detect other JDK managers (jenv, SDKMAN, etc.) |
| `jdx help [command]` | Display help for any command |

### Command Examples

**Scan for JDKs:**
```bash
jdx scan              # Standard scan
jdx scan --deep       # Deep scan (includes user directories)
```

**Get JDK Information:**
```bash
jdx info java-21      # Show detailed info about Java 21
```

**Switch JDK:**
```bash
eval "$(jdx use java-17)"        # Bash/Zsh
jdx use java-17 | Invoke-Expression  # PowerShell
```

**Pin Project Versions:**
```bash
jdx pin --runtime 21             # Pin runtime only
jdx pin --compile 17             # Pin compile target only
jdx pin --runtime 21 --compile 17   # Pin both
jdx pin --dry-run --runtime 21   # Preview changes without writing
```

**Configuration:**
```bash
jdx config get defaults.runtime
jdx config set defaults.runtime 21
```

📖 **For complete command details**, see [SPECIFICATION.md](SPECIFICATION.md#9-cli-design).

## 🔧 Understanding `.jdxrc`

When you run `jdx pin`, a `.jdxrc` file is created in your project root:

```yaml
version: 1
project:
  runtime:
    require: "21"          # JDK used to run build tools (Maven, Gradle)
    vendor: "any"          # Prefer specific vendor or "any"
  compile:
    release: 17            # Java bytecode target (javac --release)
    enforce: true          # Fail if misconfigured
tooling:
  maven_manage_toolchains: true
  gradle_manage_toolchain_block: true
  ide_hint: true
notes: "This file is maintained by jdx."
```

**Key Concepts:**

- **runtime.require**: The JDK version used to *run* Maven/Gradle and your application
- **compile.release**: The Java *bytecode version* your code is compiled to
- **Example**: `runtime: 21, compile: 17` means "run the build on Java 21, but compile bytecode compatible with Java 17"

**Commit `.jdxrc` to your repository** so all developers and CI use the same JDK configuration.

📖 **For complete file format details**, see [SPECIFICATION.md](SPECIFICATION.md#10-file-formats).

## 🔍 How It Works

### JDK Discovery

`jdx scan` searches for JDKs in platform-specific locations:

- **Windows**: Registry keys, `C:\Program Files\Java\`, PATH entries
- **macOS**: `/usr/libexec/java_home`, `/Library/Java/JavaVirtualMachines/`
- **Linux**: `/usr/lib/jvm`, `update-alternatives`, PATH entries

**Deep Scan** (`--deep`) additionally searches:
- User directories: `~/.sdkman`, `~/.jenv`, `~/jdks`
- System directories: `/opt`, `/usr/local`, `/usr/java`

### Shell Activation

`jdx use` outputs shell-specific commands to:
1. Set `JAVA_HOME` to the selected JDK
2. Prepend `$JAVA_HOME/bin` to PATH
3. Remove conflicting Java path entries

You must use `eval` to apply these changes to your current shell.

### Build Tool Configuration

`jdx apply` configures your build tools:

**Maven:**
- Updates `~/.m2/toolchains.xml` with discovered JDKs
- Creates backup before modifying
- Configures `maven-toolchains-plugin` for compilation

**Gradle:**
- Sets `org.gradle.java.home` in `gradle.properties` (runtime JDK)
- Configures Java toolchain in `gradle/jdx.gradle` (compile target)

📖 **For detailed algorithms and behaviors**, see [SPECIFICATION.md](SPECIFICATION.md#11-algorithms-and-behaviors).

## ❓ Troubleshooting

### Common Issues

**JDK not found after installation**
```bash
# Try a deep scan to search non-standard locations
jdx scan --deep

# Check if PATH includes the JDK
which java
```

**"No JDKs in catalog" error**
```bash
# Run scan first
jdx scan

# Verify JDKs were found
jdx list
```

**Maven not using the correct JDK**
```bash
# Verify configuration
jdx verify

# Check Maven is using toolchains
mvn -version
mvn help:active-profiles

# Re-apply configuration
jdx apply
```

**Changes not taking effect in shell**
```bash
# Make sure you're using eval
eval "$(jdx use java-17)"

# Not just:
jdx use java-17  # This won't work!

# Verify JAVA_HOME is set
echo $JAVA_HOME
java -version
```

**Conflicts with other JDK managers**
```bash
# Detect conflicts
jdx detect-foreign

# jdx will show warnings if it finds jenv, SDKMAN, etc.
# Consider removing other managers or using them exclusively
```

**Permission issues**
```bash
# Check catalog directory exists and is writable
ls -la ~/.jdx/

# If needed, recreate it
rm -rf ~/.jdx
jdx scan
```

### Getting Help

If you encounter issues:

1. Run `jdx doctor` to diagnose common problems
2. Check [SPECIFICATION.md](SPECIFICATION.md) for detailed technical information
3. Open an issue on [GitHub](https://github.com/brunoborges/jdx/issues)

## 🤝 Contributing

Interested in contributing to `jdx`? We'd love your help!

See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Development setup and prerequisites
- Build instructions and testing
- Coding guidelines and best practices
- Project structure and architecture
- How to submit pull requests

## 📄 License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.

## 🗺️ Roadmap

### Current Status

✅ **MVP Complete** - All core features are implemented and working:
- JDK discovery on Windows, macOS, and Linux
- Shell activation and JDK switching
- Maven and Gradle toolchain configuration
- Project-level pinning with `.jdxrc`
- Verification and diagnostics

### Coming Soon

- 📦 Pre-built distributions (Homebrew, winget)
- 🔄 Automatic catalog refresh
- 🎯 IDE integration helpers
- 🔐 Enhanced security features
- 🌐 Support for additional build tools

📖 **For detailed roadmap and future plans**, see [SPECIFICATION.md](SPECIFICATION.md#22-roadmap).

---

**Made with ❤️ for Java developers who juggle multiple JDK versions.**
