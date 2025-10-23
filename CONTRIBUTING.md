# Contributing to jdx

Thank you for your interest in contributing to jdx! This guide will help you set up your development environment and understand the project structure.

## Development Setup

### Prerequisites

- **JDK 25 or later** (required to build jdx)
- **Maven 3.9+**
- **Git**

### Getting Started

1. **Fork the repository** on GitHub

2. **Clone your fork:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/jdx.git
   cd jdx
   ```

3. **Build the project:**
   ```bash
   mvn clean install
   ```

4. **Run tests:**
   ```bash
   mvn test
   ```

5. **Run the application:**
   
   **From JAR (Development):**
   ```bash
   java -jar target/jdx-0.1.0-SNAPSHOT.jar --help
   ```
   
   **Or build a distribution:**
   ```bash
   mvn clean package -Pjlink-runtime
   cd target
   tar -xzf jdx-0.1.0-SNAPSHOT-*.tar.gz
   cd jdx-0.1.0-SNAPSHOT
   ./bin/jdx --help
   ```

## Building

### Standard Build (JAR only)

For development and testing:

```bash
mvn clean package
```

This produces a shaded JAR at `target/jdx-0.1.0-SNAPSHOT.jar`.

**Note:** Running with `java -jar` requires Java to be available in your PATH and will break if you switch to an incompatible JDK version.

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

**Benefits of jlink Distribution:**
- **No JDK Required**: The `jdx` command works independently of system Java installation
- **Immune to JDK Switching**: Even when you switch JDKs with `jdx use`, the jdx tool itself keeps working
- **Smaller Size**: Only includes required Java modules (~50-80MB vs full JDK)
- **Faster Startup**: Optimized runtime with compressed modules
- **Platform-Specific**: Each platform gets its own optimized runtime

### Running from Distribution

After building with the jlink profile:

**Unix/Linux/macOS:**
```bash
cd target/jdx-0.1.0-SNAPSHOT
./bin/jdx --help
```

**Windows:**
```cmd
cd target\jdx-0.1.0-SNAPSHOT
bin\jdx.bat --help
```

## Project Structure

```
jdx/
├── pom.xml                      # Maven project configuration
├── SPECIFICATION.md             # Complete product specification
├── README.md                    # User-facing documentation
├── CONTRIBUTING.md              # This file - contributor guide
├── LICENSE                      # Apache 2.0 license
├── build-distribution.sh        # Build script for distributions
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/jdx/
    │   │       ├── JdxMain.java          # Main entry point and CLI
    │   │       ├── catalog/              # JDK catalog management
    │   │       │   ├── JdkCatalog.java   # Catalog operations
    │   │       │   └── CatalogStorage.java
    │   │       ├── discovery/            # JDK discovery logic
    │   │       │   ├── JdkDiscovery.java      # Main discovery coordinator
    │   │       │   ├── WindowsJdkDiscovery.java
    │   │       │   ├── MacOSJdkDiscovery.java
    │   │       │   └── LinuxJdkDiscovery.java
    │   │       ├── model/                # Data models
    │   │       │   ├── JdkInfo.java      # JDK metadata
    │   │       │   ├── ProjectConfig.java # .jdxrc format
    │   │       │   └── JdxConfig.java    # Global config
    │   │       ├── shell/                # Shell activation
    │   │       │   ├── ShellActivation.java
    │   │       │   ├── BashActivation.java
    │   │       │   ├── ZshActivation.java
    │   │       │   └── PowerShellActivation.java
    │   │       └── toolchain/            # Build tool integration
    │   │           ├── MavenToolchainManager.java
    │   │           └── GradleToolchainManager.java
    │   └── resources/
    └── test/
        └── java/
            └── com/jdx/
                ├── JdxMainTest.java      # CLI integration tests
                ├── catalog/              # Catalog tests
                ├── discovery/            # Discovery tests
                └── ...
```

## Architecture

The project is organized into several key packages:

### Core Packages

- **catalog** (`com.jdx.catalog`)
  - Manages the database of discovered JDKs
  - Handles persistence to `~/.jdx/catalog.json`
  - Provides search and filtering operations

- **discovery** (`com.jdx.discovery`)
  - Platform-specific JDK discovery implementations
  - Scans standard and non-standard JDK locations
  - Parses JDK metadata from `release` files

- **model** (`com.jdx.model`)
  - Core data models: `JdkInfo`, `ProjectConfig`, `JdxConfig`
  - Immutable records for type safety
  - JSON/YAML serialization support

- **shell** (`com.jdx.shell`)
  - Shell-specific activation script generation
  - PATH manipulation and environment variables
  - Support for Bash, Zsh, PowerShell, CMD

- **toolchain** (`com.jdx.toolchain`)
  - Maven toolchains.xml generation and management
  - Gradle toolchain configuration
  - Build tool integration logic

### Key Design Decisions

1. **Immutable Data Models**: Using Java records for thread-safe, immutable data
2. **Platform Abstraction**: Strategy pattern for OS-specific discovery
3. **No Background Processes**: All operations are synchronous and explicit
4. **Safe File Operations**: Atomic writes with backups for user files
5. **CLI Framework**: Using Picocli for command parsing and help generation

## Coding Guidelines

### General Principles

- **Use Java 25 language features** where appropriate (pattern matching, records, etc.)
- **Follow standard Java naming conventions**
- **Write comprehensive unit tests** for new functionality
- **Keep methods focused** - single responsibility principle
- **Prefer immutability** - use records and final fields
- **Document public APIs** with Javadoc
- **Handle errors gracefully** - provide clear error messages

### Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Maximum 120 characters
- **Braces**: Opening brace on same line (Java standard)
- **Naming**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: lowercase

### Documentation

- **Public classes and methods**: Must have Javadoc
- **Complex logic**: Add inline comments explaining the "why"
- **Parameters and return values**: Document in Javadoc
- **Examples**: Include usage examples in Javadoc where helpful

### Error Handling

- **User errors**: Exit with code 1 and clear message
- **Environment issues**: Exit with code 2 and suggestions
- **IO/permission errors**: Exit with code 3 and explain what failed
- **Verification failures**: Exit with code 4 and show what's wrong

### Example Code Style

```java
/**
 * Discovers JDK installations on the current platform.
 * 
 * @param deepScan if true, searches non-standard locations
 * @return list of discovered JDKs, never null
 * @throws DiscoveryException if scanning fails
 */
public List<JdkInfo> discoverJdks(boolean deepScan) throws DiscoveryException {
    List<JdkInfo> jdks = new ArrayList<>();
    
    // Scan standard locations first
    jdks.addAll(scanStandardLocations());
    
    // Optionally scan deeper
    if (deepScan) {
        jdks.addAll(scanDeepLocations());
    }
    
    return Collections.unmodifiableList(jdks);
}
```

## Testing

All new features should include appropriate unit tests.

### Test Framework

We use:
- **JUnit 5** for test framework
- **AssertJ** for fluent assertions
- **Mockito** for mocking dependencies

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JdkDiscoveryTest

# Run with verbose output
mvn test -X

# Skip tests during build
mvn clean package -DskipTests
```

### Writing Tests

**Test Structure:**
```java
@Test
void shouldDiscoverJdkInStandardLocation() {
    // Given
    Path jdkPath = setupTestJdk();
    
    // When
    List<JdkInfo> jdks = discovery.discoverJdks(false);
    
    // Then
    assertThat(jdks)
        .isNotEmpty()
        .anyMatch(jdk -> jdk.path().equals(jdkPath));
}
```

**Test Naming:**
- Use descriptive names: `shouldDoSomethingWhenCondition()`
- Avoid: `test1()`, `testDiscovery()`

**Test Coverage:**
- Cover happy paths and error cases
- Test boundary conditions
- Mock external dependencies (filesystem, registry, etc.)
- Test platform-specific code when possible

### Integration Tests

For end-to-end testing:

```bash
# Build distribution
mvn clean package -Pjlink-runtime

# Extract and test
cd target
tar -xzf jdx-0.1.0-SNAPSHOT-*.tar.gz
cd jdx-0.1.0-SNAPSHOT

# Run integration tests
./bin/jdx scan
./bin/jdx list
./bin/jdx doctor
```

## Development Workflow

### Making Changes

1. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes:**
   - Write code following the guidelines above
   - Add/update tests as needed
   - Update documentation if needed

3. **Test your changes:**
   ```bash
   mvn clean test
   ```

4. **Commit your changes:**
   ```bash
   git add .
   git commit -m "Add feature: description of your changes"
   ```
   
   **Commit Message Guidelines:**
   - Use present tense: "Add feature" not "Added feature"
   - First line should be concise (50 chars or less)
   - Add detailed description if needed
   - Reference issue numbers: "Fixes #123"

5. **Push to your fork:**
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request** on GitHub

### Pull Request Guidelines

**Before submitting:**
- [ ] Tests pass (`mvn test`)
- [ ] Code follows style guidelines
- [ ] Documentation is updated
- [ ] Commit messages are clear
- [ ] Branch is up to date with main

**PR Description should include:**
- What changes were made
- Why the changes were needed
- How to test the changes
- Any breaking changes or migration notes
- Screenshots for UI changes (if applicable)

### Code Review Process

- All submissions require review
- Reviewers will provide feedback
- Address feedback and push updates
- Once approved, maintainers will merge

## Common Development Tasks

### Adding a New Command

1. Add command to `JdxMain.java` using Picocli annotations
2. Implement command logic in appropriate package
3. Add unit tests
4. Update README.md with command documentation
5. Update SPECIFICATION.md if needed

### Adding Platform Support

1. Create new discovery class (e.g., `FreeBSDJdkDiscovery.java`)
2. Implement platform-specific logic
3. Update `JdkDiscovery.java` to detect and use new platform
4. Add tests with mocked filesystem
5. Document in SPECIFICATION.md

### Updating Dependencies

1. Update version in `pom.xml`
2. Test thoroughly
3. Document breaking changes
4. Update CONTRIBUTING.md if build process changes

## Debugging

### Debug Logging

Set environment variable for verbose output:
```bash
export JDX_LOG=debug
jdx scan
```

### Running with Debugger

```bash
# Run with debug port
mvn exec:java -Dexec.mainClass="com.jdx.JdxMain" -Dexec.args="scan" \
  -Dexec.classpathScope=runtime \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005
```

Then attach your IDE debugger to port 5005.

### Testing on Different Platforms

- **Windows**: Use Windows Subsystem for Linux (WSL) or VM
- **macOS**: Use macOS VM or GitHub Actions
- **Linux**: Use Docker containers or VMs

## Resources

- [Picocli Documentation](https://picocli.info/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [SPECIFICATION.md](SPECIFICATION.md) - Complete product specification

## Questions?

Feel free to:
- Open an issue for discussion before starting major changes
- Ask questions in pull request comments
- Check [SPECIFICATION.md](SPECIFICATION.md) for technical details
- Review existing code for examples and patterns

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0, the same license as this project.

---

**Thank you for contributing to jdx! 🎉**
