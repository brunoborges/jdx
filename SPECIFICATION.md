# JDK Management CLI — Product Specification

## 1. Summary and Vision

A cross platform CLI that discovers all JDKs on a machine, lets users switch the shell's active JDK, and keeps builds reproducible by wiring Maven and Gradle to the right Java versions. It removes ambiguity between the JDK that runs the build tool and the JDK used to compile code, and makes the setup obvious to people, IDEs, and CI.

**Working name:** `jdx`

## 2. Goals

- Find and catalog all JDKs installed locally across Windows, macOS, and Linux.
- Show, set, and persist the runtime JDK for shells, tools, and IDEs.
- Generate and maintain Maven Toolchains and Gradle Toolchains so projects compile with the intended Java version.
- Make the active JDK obvious and consistent across terminal sessions and repos.
- Provide project local pinning and global defaults, without taking over machine wide settings.
- Be safe to try, easy to undo, and transparent about changes.

## 3. Non Goals

- Hosting or downloading JDKs at first release. We can integrate with vendors later.
- Replacing package managers like winget, Homebrew, apt.
- Managing non Java SDKs.
- IDE specific deep configuration. We only print deterministic instructions or minimal files IDEs can read.

## 4. Target Users and Personas

- **SRE or Build Engineer** balancing multiple repos that require different Java levels.
- **Java Developer** switching between projects with JDK 8, 11, 17, 21, 25.
- **Consultant** who needs per repo Java pinning without breaking global PATH.
- **CI Owner** wanting a single command to assert the correct JDKs and toolchains.

## 5. Core Concepts

- **Catalog**: a structured database of discovered JDKs with version, vendor, architecture, location, capabilities.
- **Active JDK**: the JDK that the current shell uses to run `java`, `javac`, `mvn`, `gradle`.
- **Compile JDK**: the JDK versions pinned for building a given project. Managed by toolchains, not by PATH.
- **Scopes**:
  - **Global**: default for the machine or current user.
  - **Project**: settings stored in the repository to ensure reproducible builds.
- **State files**:
  - **Global**: `~/.jdx/config.yaml`
  - **Project**: `./.jdxrc` in the repo root, committed.

## 6. User Stories

1. As a dev, I want to run `jdx scan` and see all JDKs on my machine with clear versions and paths.
2. As a dev, I want `jdx use 17` to make `java -version` show 17 in my shell, without breaking other shells.
3. As a build owner, I want `jdx pin --project --compile 8` to configure Maven and Gradle to compile for 8 even if I run them on 21.
4. As a lead, I want a repo to declare its requirements so new contributors run `jdx apply` and are ready in seconds.
5. As a CI owner, I want `jdx verify` to fail fast when the environment is wrong.
6. As a Windows user, I want this to work in PowerShell and cmd the same way it works in bash and zsh.

## 7. Functional Requirements

### 7.1 Discovery

- `jdx scan`
  - **Windows**: search registry `HKLM\Software\JavaSoft\JDK`, `HKCU`, common install dirs under `C:\Program Files\Java\`, `C:\Program Files\Microsoft\jdk\`, checks PATH hits like `where java`, inspects `java -XshowSettings:properties`.
  - **macOS**: query `/usr/libexec/java_home -V` and canonical locations in `/Library/Java/JavaVirtualMachines/*.jdk/Contents/Home`.
  - **Linux**: check `/usr/lib/jvm/*`, `update-alternatives --display java`, PATH hits via `which -a java`.
  - Parse release file to capture `JAVA_VERSION`, `IMPLEMENTOR`, `OS_ARCH`.
  - Persist results to the Catalog. Never auto modify PATH.

### 7.2 Listing and Inspecting

- `jdx list` shows a table:
  - id, version, vendor, arch, path, capabilities (jlink, jpackage), status (valid, broken).
- `jdx info <id|version>` prints detailed metadata and the exact environment exports needed.

### 7.3 Switching the Runtime JDK

- `jdx use <version|id> [--shell]`
  - Outputs shell specific exports so the current shell uses that JDK.
  - For bash/zsh/fish: prints `export JAVA_HOME=...` and a sanitized PATH fragment. Supports `eval "$(jdx use 21 --shell)"`.
  - For PowerShell: prints setx suggestions and a transient `$env:JAVA_HOME` update. Supports `jdx use 21 | Invoke-Expression`.
  - Never mutates system wide PATH by default. Offer `--persist` to write user profile snippets:
    - bash/zsh: `~/.jdx/activate.sh` and note to source in `~/.zshrc`.
    - PowerShell: add to CurrentUser profile after confirmation.
- `jdx deactivate` restores previous `JAVA_HOME` and PATH for this shell.

### 7.4 Project Pinning

- `jdx pin --project --runtime <ver>` writes `.jdxrc` with the JDK to run tools.
- `jdx pin --project --compile <ver>` updates:
  - **Maven**: writes `~/.m2/toolchains.xml` entry if missing and injects minimal POM plugin config with `<release>` suggestion to user. Project local: optional `.mvn/jdx.toolchains.xml` plus a `jdx-maven-toolchains.xml` include pattern, with instructions. We will not overwrite existing toolchains, we append and create if absent.
  - **Gradle**: adds or updates `gradle.properties` with `org.gradle.java.home` for runtime, and `build.gradle[.kts]` toolchain block for compile language level. If editing source files is not allowed, we generate a `gradle/jdx.gradle` applied from `settings.gradle`.
- `jdx apply` reads `.jdxrc` and sets the current shell and toolchains accordingly.
- `jdx unpin` removes project settings created by jdx, leaving user edits intact.

### 7.5 Verification

- `jdx verify`
  - Checks `java -version`, `javac -version`.
  - Validates Maven runtime JDK and effective `maven-compiler-plugin` `<release>`.
  - Validates Gradle daemon JDK and Toolchain target.
  - Fails with clear guidance and the exact commands to fix.

### 7.6 Dry Run and Diff

- All commands support `--dry-run` to show intended changes.
- `--diff` prints unified diffs for files that would be created or changed.

### 7.7 Safety and Rollback

- Writes are idempotent and confined:
  - **Global**: `~/.jdx/*` and optional `~/.m2/toolchains.xml` append.
  - **Project**: `.jdxrc`, `.mvn/jdx.*`, `gradle/jdx.gradle`, `gradle.properties` edits guarded by markers.
- `jdx undo` reverts last operation using a small change journal in `~/.jdx/journal`.

### 7.8 IDE Hints

- `jdx ide --print` emits a short set of instructions for IntelliJ, VS Code, and Eclipse to align their Project SDK and Gradle/Maven JDK. No direct IDE automation initially.

### 7.9 Interop

- jenv, SDKMAN, mise/asdf:
  - `jdx detect-foreign` lists shims found on PATH and warns about conflicts.
  - `jdx respect` mode avoids writing shell activation if a manager is detected, and limits itself to toolchains and per shell exports.

## 8. Non Functional Requirements

- Cross platform binaries with near zero runtime dependencies.
- Fast. `jdx list` should complete under 100 ms after first scan. Scan under 1 second on typical dev machines.
- Transparent. Every change printed, with a path to undo.
- No always on background services.
- Telemetry off by default. If enabled, only aggregate command counts and error codes, never paths.

## 9. CLI Design

```
jdx
  scan                         # discover JDKs
  list [--json]                # list catalog
  info <id|version>
  use <id|version> [--shell] [--persist] [--dry-run]
  deactivate
  pin --project [--runtime <ver>] [--compile <ver>] [--vendor <name>]
  apply [--strict]             # apply .jdxrc
  verify [--maven] [--gradle] [--ide]
  undo
  detect-foreign               # jenv/sdkman/etc
  config [get|set] <key> [val] # global config in ~/.jdx/config.yaml
  doctor                       # common problems and fixes
```

**Exit codes:**

- `0` success
- `1` user error
- `2` environment conflict
- `3` IO/permissions
- `4` verify failed

## 10. File Formats

### 10.1 Project file `.jdxrc`

```yaml
version: 1
project:
  runtime:
    require: "21"          # JDK to run tools
    vendor: "Temurin|Microsoft|any"
  compile:
    release: 17            # javac --release target
    enforce: true          # fail verify if mismatched
tooling:
  maven:
    manage_toolchains: true
  gradle:
    manage_toolchain_block: true
  ide_hint: true
notes: "This file is maintained by jdx."
```

### 10.2 Global config `~/.jdx/config.yaml`

```yaml
catalog:
  autorefresh_days: 7
defaults:
  runtime: "21"
  vendor_preference: ["Microsoft", "Temurin", "any"]
safety:
  require_confirmation_on_persist: true
telemetry:
  enabled: false
```

## 11. Algorithms and Behaviors

### 11.1 Version Resolution

- Accepts semver like and GA forms: `8`, `1.8`, `8u372`, `17.0.11`, `21`, `25-ea`.
- Prefer exact match. If not found, pick highest patch within the requested feature. Respect vendor preference list.
- If multiple architectures exist, prefer host arch.

### 11.2 Shell Activation

- Print, do not mutate, unless `--persist` is set.
- Export sequence:
  - Set `JAVA_HOME`.
  - Prepend `$JAVA_HOME/bin` ahead of any existing Java path segments.
  - Strip duplicated Java entries to avoid PATH growth.

### 11.3 Maven Toolchains

- Ensure `~/.m2/toolchains.xml` exists. Insert a `<toolchain>` block per discovered version if missing. Do not remove user entries.
- For project scope, generate `.mvn/jdx.toolchains.xml` and update POM only inside markers:

```xml
<!-- jdx:begin -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.13.0</version>
  <configuration>
    <release>${maven.compiler.release}</release>
  </configuration>
</plugin>
<!-- jdx:end -->
```

plus a `maven.compiler.release` property if absent.

### 11.4 Gradle Toolchains

- Append a small include `apply from: 'gradle/jdx.gradle'` in `settings.gradle` if safe, or instruct the user.
- The included file contains:

```groovy
java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(17)
  }
}
tasks.withType(JavaCompile).configureEach {
  options.release = 17
}
```

- For runtime JDK of the daemon, write `org.gradle.java.home` to `gradle.properties`.

### 11.5 Verification Rules

- **Runtime**: `java -version` equals requested feature.
- **Maven**: `mvn -v` JVM equals runtime JDK if pinned, effective-pom shows expected release.
- **Gradle**: `gradle -version` shows JVM equals runtime JDK, `--scan` optional check of toolchain target.
- **Bytecode**: parse compiled classes under `target` or `build/classes` to confirm major version matches release.

## 12. OS Specifics

### Windows

- PowerShell and cmd stubs generated at `~\AppData\Local\jdx\activate.ps1` and `activate.cmd`.
- Respect per user PATH size. Avoid editing System PATH.
- Registry reads for discovery. Support spaces in paths and `javaw.exe` quirks.

### macOS

- Prefer `/usr/libexec/java_home -v <feature>` for compatibility with Apple tooling.
- Handle `.jdk` bundle paths and quarantine bits on unzipped vendors.

### Linux

- Do not rely on `update-alternatives` by default. Read it for discovery, do not mutate.
- Coexist with distro packaged JDKs and user provided ones under `~/jdks`.

## 13. Security and Privacy

- No elevation required for normal operations.
- Never send file paths or environment to servers.
- When telemetry is opted in, include only command name and exit code.

## 14. Packaging and Distribution

- Single static binary per platform: `jdx.exe` (Windows), `jdx` (macOS/Linux).
- Homebrew tap, winget, and generic tar/zip releases.
- Checksums and optional code signing.

## 15. Compatibility and Coexistence

- Detect jenv, SDKMAN, asdf/mise. In "respect" mode, do not write shell activation and warn about overlapping shims.
- Offer a one time shim bypass: `jdx exec -- mvn -version` runs with the selected `JAVA_HOME` for the child process only.

## 16. Observability

- `scan` is always verbose and shows every decision and file touched.
- `JDX_LOG=debug` env var for support cases.
- `jdx doctor` prints an actionable report with red or green checks.

## 17. Performance Targets

- First scan: under 1 second on a machine with up to 10 JDKs.
- Subsequent list: under 100 ms.
- verify: under 2 seconds for Maven or Gradle projects without running full builds.

## 18. Error Handling

- Clear error for version not found with suggestions of closest matches.
- If file edits are blocked, print exact patch snippets the user can paste.
- If conflicts detected, exit with code 2 and list the conflicting managers and paths.

## 19. Example Flows

### 19.1 New contributor

```bash
git clone repo
cd repo
jdx apply          # reads .jdxrc, sets JAVA_HOME in this shell
jdx verify         # confirms toolchains are aligned
mvn -q -DskipTests package
```

### 19.2 Add compile level to an existing repo

```bash
jdx pin --project --compile 8
jdx verify
```

### 19.3 Switch runtime JDK for troubleshooting

```bash
eval "$(jdx use 21 --shell)"
mvn -v
```

### 19.4 CI assert

```bash
jdx apply --strict
jdx verify || exit 1
```

## 20. Test Plan (high level)

- Unit tests for version parsing, catalog merges, path rewriting, toolchain XML and Gradle snippet generation.

14) Packaging and Distribution
	•	Single static binary per platform: jdx.exe (Windows), jdx (macOS/Linux).
	•	Homebrew tap, winget, and generic tar/zip releases.
	•	Checksums and optional code signing.

15) Compatibility and Coexistence
	•	Detect jenv, SDKMAN, asdf/mise. In “respect” mode, do not write shell activation and warn about overlapping shims.
	•	Offer a one time shim bypass: jdx exec -- mvn -version runs with the selected JAVA_HOME for the child process only.

16) Observability
    •	scan shows every decision and file touched.
	•	JDX_LOG=debug env var for support cases.
	•	jdx doctor prints an actionable report with red or green checks.

17) Performance Targets
	•	First scan: under 1 second on a machine with up to 10 JDKs.
	•	Subsequent list: under 100 ms.
	•	verify: under 2 seconds for Maven or Gradle projects without running full builds.

18) Error Handling
	•	Clear error for version not found with suggestions of closest matches.
	•	If file edits are blocked, print exact patch snippets the user can paste.
	•	If conflicts detected, exit with code 2 and list the conflicting managers and paths.

19) Example Flows

19.1 New contributor

git clone repo
cd repo
jdx apply          # reads .jdxrc, sets JAVA_HOME in this shell
jdx verify         # confirms toolchains are aligned
mvn -q -DskipTests package

19.2 Add compile level to an existing repo

jdx pin --project --compile 8
jdx verify

19.3 Switch runtime JDK for troubleshooting

eval "$(jdx use 21 --shell)"
mvn -v

19.4 CI assert

jdx apply --strict
jdx verify || exit 1

20) Test Plan (high level)
	
- Unit tests for version parsing, catalog merges, path rewriting, toolchain XML and Gradle snippet generation.
- Integration tests on each OS in clean containers or VMs with multiple vendors installed.
- Golden file tests for `.jdxrc`, `toolchains.xml` append, `gradle.properties` updates.
- Collision tests with jenv, SDKMAN, mise.
- Ide hint snapshot tests for IntelliJ, VS Code.

**Acceptance criteria for MVP:**

- `scan`, `list`, `use --shell`, `pin --project --compile`, `verify` working on Windows, macOS, Linux.
- Maven Toolchains generated or appended safely.
- Gradle Toolchains configured without breaking existing builds.
- At least one Windows shell (PowerShell) and one POSIX shell (zsh) persisted activation working.

## 21. Risks and Mitigations

- **Editing user files**: Only within markers, offer dry run and undo.
- **Conflicts with managers**: Detect and switch to respect mode by default.
- **Vendor differences**: Provide vendor preference options and warn when mixing.
- **IDE drift**: Keep IDE changes opt in and documented, not automatic.

## 22. Roadmap

### MVP

- Discovery, list, use in current shell.
- Project pin compile level for Maven and Gradle.
- Verify and doctor.
- Respect mode for existing managers.

### v1.0

- Persisted activation, undo journal.
- CI friendly apply `--strict`.
- Bytecode verification parser.
- Simple plugin interface for new build tools.

### v1.1

- Optional download integration with trusted vendors, with checksums.
- Repo templates and Git hooks to auto run `jdx apply`.
- Minimal IDE automation through exported `.sdkmanrc` or `.idea` hints only when requested.

---

If you want, I can convert this into a README style document with command examples and copy ready snippets for `toolchains.xml`, `build.gradle.kts`, and PowerShell or zsh activation scripts.
	•	Integration tests on each OS in clean containers or VMs with multiple vendors installed.
	•	Golden file tests for .jdxrc, toolchains.xml append, gradle.properties updates.
	•	Collision tests with jenv, SDKMAN, mise.
	•	Ide hint snapshot tests for IntelliJ, VS Code.

Acceptance criteria for MVP:
	•	scan, list, use --shell, pin --project --compile, verify working on Windows, macOS, Linux.
	•	Maven Toolchains generated or appended safely.
	•	Gradle Toolchains configured without breaking existing builds.
	•	At least one Windows shell (PowerShell) and one POSIX shell (zsh) persisted activation working.

21) Risks and Mitigations
	•	Editing user files: Only within markers, offer dry run and undo.
	•	Conflicts with managers: Detect and switch to respect mode by default.
	•	Vendor differences: Provide vendor preference options and warn when mixing.
	•	IDE drift: Keep IDE changes opt in and documented, not automatic.

22) Roadmap

MVP
	•	Discovery, list, use in current shell.
	•	Project pin compile level for Maven and Gradle.
	•	Verify and doctor.
	•	Respect mode for existing managers.

v1.0
	•	Persisted activation, undo journal.
	•	CI friendly apply --strict.
	•	Bytecode verification parser.
	•	Simple plugin interface for new build tools.

v1.1
	•	Optional download integration with trusted vendors, with checksums.
	•	Repo templates and Git hooks to auto run jdx apply.
	•	Minimal IDE automation through exported .sdkmanrc or .idea hints only when requested.

⸻

If you want, I can convert this into a README style document with command examples and copy ready snippets for toolchains.xml, build.gradle.kts, and PowerShell or zsh activation scripts.