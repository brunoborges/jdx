# jdx - JDK Management CLI

A cross-platform CLI that discovers all JDKs on a machine, lets users switch the shell's active JDK, and keeps builds reproducible by wiring Maven and Gradle to the right Java versions.

## Get Started

### Prerequisites

- **JDK 25** (required to build jdx)
- **Maven 3.9+** 
- **Git**

### Quick Start

1. **Clone the Repository**

```bash
git clone https://github.com/brunoborges/jdx.git
cd jdx
```

2. **Build the Distribution with Embedded Runtime**

```bash
mvn clean package -DskipTests -Pjlink-runtime
```

This creates a platform-specific distribution with an embedded Java runtime in:
- `target/jdx-0.1.0-SNAPSHOT-{platform}.zip`
- `target/jdx-0.1.0-SNAPSHOT-{platform}.tar.gz`

3. **Extract and Install**

**macOS/Linux:**
```bash
# Extract the distribution
cd target
tar -xzf jdx-0.1.0-SNAPSHOT-*.tar.gz
cd jdx-0.1.0-SNAPSHOT

# Install to a standard location
sudo mkdir -p /opt/jdx
sudo cp -r . /opt/jdx/

# Add to PATH (add this to your ~/.bashrc or ~/.zshrc)
export PATH="/opt/jdx/bin:$PATH"

# Or create a symlink
sudo ln -s /opt/jdx/bin/jdx /usr/local/bin/jdx
```

**Windows:**
```cmd
# Extract the ZIP file
# Copy to C:\Program Files\jdx
# Add C:\Program Files\jdx\bin to your PATH environment variable
```

4. **Verify Installation**

```bash
jdx --help
jdx --version
```

The `jdx` command now works independently of your system's Java installation!

5. **Discover Your JDKs**

```bash
jdx scan
jdx list
```

6. **Try It on a Maven Project**

Navigate to any Maven project and configure it with jdx:

```bash
cd /path/to/your/maven-project

# Pin a runtime JDK for your project
jdx pin --runtime 21

# Check the generated .jdxrc file
cat .jdxrc

# Apply the configuration (generates Maven toolchains)
jdx apply

# Verify everything is configured correctly
jdx verify

# Build your project with the configured toolchain
mvn clean install
```

### Example Workflow

```bash
# Discover all JDKs on your system
$ jdx scan
Scan complete. Found 5 JDK(s).

# List discovered JDKs
$ jdx list
ID         VERSION    VENDOR       ARCH      STATUS    PATH
-------------------------------------------------------------------------
java-21    21.0.1     Temurin      aarch64   ✓ valid   /Library/Java/...
java-17    17.0.9     Temurin      aarch64   ✓ valid   /Library/Java/...
java-11    11.0.21    Microsoft    aarch64   ✓ valid   /Library/Java/...

# Get detailed info about a specific JDK
$ jdx info java-21
JDK Information:
================
ID:           java-21
Version:      21.0.1
Vendor:       Temurin
Architecture: aarch64
Path:         /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
Status:       ✓ Valid
Capabilities: jlink, jpackage

# Switch to a different JDK in your current shell
$ eval "$(jdx use java-17)"
$ java -version
openjdk version "17.0.9"

# Pin runtime and compile targets for a project
$ cd my-project
$ jdx pin --runtime 21 --compile 17
Created/updated .jdxrc

# Apply project configuration
$ jdx apply
Applying .jdxrc configuration...
Runtime JDK: 21 at /Library/Java/...
Updated Maven toolchains at: ~/.m2/toolchains.xml
Toolchains configured for compile target: 17

# Check for issues
$ jdx doctor
jdx doctor - Checking system configuration...
✓ ~/.jdx directory exists
✓ Catalog contains 5 JDK(s)
✓ java found in PATH
✓ JAVA_HOME is set
✓ Maven toolchains.xml exists
✓ All checks passed
```

## Project Status

✅ **Fully Implemented** - All core features from the specification are now working!

## Building

This project requires JDK 25 to build.

### Recommended: Build with Custom Runtime (jlink)

The recommended build creates a self-contained distribution with an embedded Java runtime:

```bash
mvn clean package -Pjlink-runtime
```

This will:
1. Create a shaded JAR with all dependencies
2. Use jlink to create a minimal Java runtime (only required modules)
3. Package everything into platform-specific distributions:
   - `jdx-0.1.0-SNAPSHOT-{platform}.zip`
   - `jdx-0.1.0-SNAPSHOT-{platform}.tar.gz`

The resulting distribution includes:
- `bin/` - Launcher scripts (`jdx` for Unix, `jdx.bat` for Windows)
- `lib/` - Application JAR
- `runtime/` - Custom Java runtime
- Documentation files

### Benefits of jlink Distribution

- **No JDK Required**: The `jdx` command works independently of system Java installation
- **Immune to JDK Switching**: Even when you switch JDKs with `jdx use`, the jdx tool itself keeps working
- **Smaller Size**: Only includes required Java modules (~50-80MB vs full JDK)
- **Faster Startup**: Optimized runtime with compressed modules
- **Platform-Specific**: Each platform gets its own optimized runtime

### Alternative: Standard Build (JAR only)

If you just need the JAR for development/testing:

```bash
mvn clean package
```

The build will produce a shaded JAR at `target/jdx-0.1.0-SNAPSHOT.jar`.

**Note:** Running with `java -jar` requires Java to be available in your PATH and will break if you switch to an incompatible JDK version.

## Running

### From Distribution (Recommended)

After extracting the ZIP/tar.gz:

**Unix/Linux/macOS:**
```bash
./bin/jdx --help
```

**Windows:**
```cmd
bin\jdx.bat --help
```

### From JAR (Development Only)

```bash
java -jar target/jdx-0.1.0-SNAPSHOT.jar --help
```

## Project Structure

```
jdx/
├── pom.xml                 # Maven project configuration
├── SPECIFICATION.md        # Complete product specification
├── README.md              # This file
└── src/
    ├── main/
    │   └── java/
    │       └── com/jdx/
    │           ├── JdxMain.java          # Main entry point
    │           ├── catalog/              # JDK catalog management
    │           ├── discovery/            # JDK discovery logic
    │           ├── model/                # Data models
    │           ├── shell/                # Shell activation
    │           └── toolchain/            # Maven/Gradle toolchain management
    └── test/
        └── java/
            └── com/jdx/
                └── JdxMainTest.java      # Basic tests
```

## Architecture

The project is organized into several key packages:

- **catalog**: Manages the database of discovered JDKs
- **discovery**: Platform-specific JDK discovery implementations
- **model**: Core data models (JdkInfo, JdxConfig, ProjectConfig)
- **shell**: Shell-specific activation script generation
- **toolchain**: Maven and Gradle toolchain configuration

## Next Steps

See [SPECIFICATION.md](SPECIFICATION.md) for the complete product specification and roadmap.

## Commands Reference

| Command | Description |
|---------|-------------|
| `jdx scan` | Discover and catalog all JDKs on this machine |
| `jdx list` | List all discovered JDKs (use `--json` for JSON output) |
| `jdx info <id>` | Show detailed information about a specific JDK |
| `jdx use <id>` | Generate shell activation script for a JDK |
| `jdx pin` | Pin runtime and/or compile JDK versions (creates/updates `.jdxrc`) |
| `jdx apply` | Apply `.jdxrc` configuration to current environment |
| `jdx verify` | Verify JDK and toolchain configuration |
| `jdx config` | Get or set global configuration values |
| `jdx doctor` | Check for common problems and suggest fixes |
| `jdx detect-foreign` | Detect other JDK managers (jenv, SDKMAN, mise, asdf) |

## Testing Your Maven Project

After installing jdx, test it with your existing Maven project:

```bash
# Navigate to your Maven project
cd /path/to/your/maven-project

# Scan for JDKs if you haven't already
jdx scan

# Pin a runtime and compile version
jdx pin --runtime 21 --compile 17

# This creates a .jdxrc file in your project root
cat .jdxrc

# Apply the configuration
jdx apply

# This will:
# - Activate the runtime JDK (21) in your shell
# - Configure Maven toolchains.xml with all discovered JDKs
# - Set up Gradle toolchain block (if build.gradle exists)

# Verify the configuration
jdx verify

# Now build your project - Maven will use the correct JDK for compilation
mvn clean install

# Check which JDK is being used
mvn -version
```

### Understanding .jdxrc

The `.jdxrc` file pins JDK versions for your project:

```yaml
version: 1
project:
  runtime:
    require: "21"
    vendor: "any"
  compile:
    release: 17
    enforce: true
tooling:
  maven_manage_toolchains: true
  gradle_manage_toolchain_block: true
  ide_hint: true
notes: "This file is maintained by jdx."
```

- **runtime.require**: JDK used to RUN the application, tests, and build tools
- **compile.release**: Java feature level to COMPILE TO (javac --release / bytecode)
- **maven_manage_toolchains**: Automatically update `~/.m2/toolchains.xml`
- **gradle_manage_toolchain_block**: Automatically update `gradle/jdx.gradle`

## License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.

### Additional Pin Examples

```bash
# Pin just the compile target (build for 17, run on current active runtime)
jdx pin --compile 17

# Pin only the runtime (keep existing compile target)
jdx pin --runtime 21

# Pin both runtime (21) and compile target (17)
jdx pin --runtime 21 --compile 17

# Pin for a different project directory
jdx pin --project-dir ../another-project --runtime 21

# Dry run (show changes without writing files)
jdx pin --runtime 21 --compile 17 --dry-run
```
