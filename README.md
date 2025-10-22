# jdx - JDK Management CLI

A cross-platform CLI that discovers all JDKs on a machine, lets users switch the shell's active JDK, and keeps builds reproducible by wiring Maven and Gradle to the right Java versions.

## Project Status

🚧 **Under Development** - This project is in its initial bootstrap phase. The specification is complete, and the basic project structure has been created.

## Building

This project requires JDK 25 to build.

### Standard Build (JAR only)

```bash
mvn clean package
```

The build will produce a shaded JAR with all dependencies included in `target/jdx-0.1.0-SNAPSHOT.jar`.

### Build with Custom Runtime (jlink)

To create a distribution with a custom Java runtime embedded:

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
- `runtime/` - Custom Java runtime (when using `-Pjlink-runtime`)
- Documentation files

### Benefits of jlink Distribution

- **No JDK Required**: Users don't need Java installed
- **Smaller Size**: Only includes required Java modules (~50-80MB vs full JDK)
- **Faster Startup**: Optimized runtime with compressed modules
- **Platform-Specific**: Each platform gets its own optimized runtime

## Running

### From JAR

```bash
java -jar target/jdx-0.1.0-SNAPSHOT.jar --help
```

### From Distribution

After extracting the ZIP/tar.gz:

**Unix/Linux/macOS:**
```bash
./bin/jdx --help
```

**Windows:**
```cmd
bin\jdx.bat --help
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

## License

TBD
