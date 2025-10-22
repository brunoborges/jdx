# Contributing to jdx

Thank you for your interest in contributing to jdx!

## Development Setup

### Prerequisites

- JDK 25 or later
- Maven 3.9 or later
- Git

### Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/jdx.git
   cd jdx
   ```

3. Build the project:
   ```bash
   mvn clean install
   ```

4. Run tests:
   ```bash
   mvn test
   ```

5. Run the application:
   ```bash
   java -jar target/jdx-0.1.0-SNAPSHOT.jar
   ```

## Project Structure

```
jdx/
├── src/main/java/com/jdx/
│   ├── JdxMain.java          # CLI entry point
│   ├── catalog/              # JDK catalog management
│   ├── discovery/            # Platform-specific JDK discovery
│   ├── model/                # Data models
│   ├── shell/                # Shell activation scripts
│   └── toolchain/            # Maven/Gradle toolchain management
└── src/test/java/com/jdx/    # Unit tests
```

## Coding Guidelines

- Use Java 25 language features where appropriate
- Follow standard Java naming conventions
- Write unit tests for new functionality
- Keep methods focused and classes small
- Use records for immutable data classes
- Document public APIs with Javadoc

## Testing

All new features should include appropriate unit tests. We use:
- JUnit 5 for test framework
- AssertJ for fluent assertions
- Mockito for mocking

Run tests with:
```bash
mvn test
```

## Submitting Changes

1. Create a new branch for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes and commit:
   ```bash
   git add .
   git commit -m "Description of your changes"
   ```

3. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request

## Code Review Process

All submissions require review. We use GitHub pull requests for this purpose.

## Questions?

Feel free to open an issue for discussion before starting work on major changes.
