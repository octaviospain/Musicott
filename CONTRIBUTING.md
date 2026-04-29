# Contributing to Musicott

Thank you for your interest in contributing to Musicott! This document explains how to file issues, propose changes, and submit pull requests against this JavaFX desktop music player.

Musicott is the GUI layer of a three-project ecosystem — [lirp](https://github.com/octaviospain/lirp) (reactive JSON persistence) and [music-commons](https://github.com/octaviospain/music-commons) (music domain logic) sit underneath it. Changes that involve domain types or persistence usually belong in those projects; this repo focuses on the desktop UX, controllers, and Spring/JavaFX wiring.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
- [Pull Requests](#pull-requests)
- [Development Guidelines](#development-guidelines)
  - [Problem Statement Requirement](#problem-statement-requirement)
  - [Build and Test](#build-and-test)
  - [Test Source Sets](#test-source-sets)
  - [Headless Testing](#headless-testing)
  - [Writing Tests](#writing-tests)
- [Style Guidelines](#style-guidelines)
- [Project Structure](#project-structure)
- [Questions](#questions)

## Code of Conduct

This project follows a simple rule: be respectful, be patient, assume good faith. Harassment, personal attacks, or hostile behavior will not be tolerated. If you experience or witness something that violates this principle, open an issue or contact the maintainer privately.

## How Can I Contribute?

### Reporting Bugs

Before opening a bug report, please search [existing issues](https://github.com/octaviospain/Musicott/issues) to avoid duplicates. When you do file a bug, include:

- **A clear, descriptive title**
- **The exact steps to reproduce**
- **What you expected vs. what happened**
- **Logs**, Musicott uses Logback — check the console for stack traces
- **Your environment**: OS and version, JDK version (`java -version`), Musicott version (visible in *About*), and whether you launched from an installer or `gradle run`
- **A sample audio file** when the bug is metadata- or playback-related, if it can be shared
- **Screenshots or screen recordings** for UI issues

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When opening one:

- **Use a clear, descriptive title**
- **Describe the problem** the enhancement would solve
- **Explain why it matters** to Musicott's users (a personal music library player — not a streaming client, not a DAW)
- **Provide concrete examples** of how the feature would be used
- **List alternatives you considered** and why they fall short

If your idea touches the domain model (audio items, playlists, waveforms, persistence), it likely belongs in [music-commons](https://github.com/octaviospain/music-commons) instead. Open the issue there and link it here so we can track the UI work separately.

## Pull Requests

### Process

1. Fork the repository and clone your fork.
2. Create a branch named `feature/#<issue>-<short-slug>` (e.g., `feature/#42-itunes-incremental-import`). The leading `#<issue>` makes it trivial to associate the work with its tracking issue.
3. Make your changes. Keep the PR focused on a single objective — don't bundle unrelated refactoring.
4. Add or update tests (see [Test Source Sets](#test-source-sets) for which set fits your change).
5. Ensure the build passes locally:
   ```bash
   gradle clean build
   ```
6. Commit using the convention below.
7. Push and open a pull request against `master`.

### Commit Message Convention

- Single commit per PR. If you push follow-ups while addressing review comments, squash them so the final history is one commit per logical change.
- When the work is linked to a GitHub issue, include the issue number: `#<issue> <title>` (e.g., `#57 Waveform Visualization — seek and tests`).
- When there is no issue, use a conventional generic title: `docs: ...`, `fix: ...`, `feat: ...`.

### PR Requirements

All pull requests should:

- **Address a specific issue** or add a specific feature (open an issue first if none exists)
- **Include a problem statement** in the PR description (see below)
- **Include tests** appropriate for the change (unit / integration / UI / e2e — see [Test Source Sets](#test-source-sets))
- **Update documentation** when behavior or build steps change — both this repo's README and the [wiki](https://github.com/octaviospain/Musicott/wiki) when relevant
- **Pass all CI checks** (compile, all four test source sets, SonarQube)
- **Be focused on a single objective** — split unrelated changes into separate PRs

## Development Guidelines

### Problem Statement Requirement

Every PR description should answer:

1. **What problem are you trying to solve?**
2. **Why is this problem meaningful to Musicott's users?**
3. **How does your solution address the problem?**
4. **What alternatives did you consider?**

Example:

```
Problem: The "Add to playlist" context menu freezes the UI for ~2s when the
playlist hierarchy contains more than 200 entries.

Significance: Users with large iTunes libraries hit this on every right-click
and report it as the slowest interaction in the app.

Solution: Build the menu lazily — only the immediate children of the hovered
playlist are populated until the user opens a sub-menu.

Alternatives considered:
- Pre-building the full menu off the FX thread: avoids the freeze but doubles
  memory for users who never open the menu.
- Caching the built menu: invalidates on every playlist mutation, complex to
  keep correct.
```

### Build and Test

Musicott uses gradle and targets **JDK 24** with JavaFX modules. Common commands:

```bash
# Compile Java + Kotlin
gradle clean compileJava compileKotlin

# Run unit tests (headless via TestFX/Monocle)
gradle test

# Run integration tests
gradle integrationTest

# Run UI tests
gradle uiTest

# Run end-to-end tests
gradle e2eTest

# All test suites + JaCoCo coverage report
gradle check

# Full build (compile + all tests + jacoco report)
gradle build

# Launch the application
gradle run

# Run a single test class
gradle test --tests "net.transgressoft.musicott.view.PreferencesControllerTest"
```

### Test Source Sets

Musicott separates tests by intent into four Gradle source sets. Each set is gated by a class-name suffix and runs in its own task:

| Source set        | Location                  | Suffix    | Gradle task            | Purpose                                                      |
| ----------------- | ------------------------- | --------- | ---------------------- | ------------------------------------------------------------ |
| `test`            | `src/test/`               | `*Test`   | `gradle test`          | Unit tests — fast, isolated, no Spring context unless needed |
| `integrationTest` | `src/integrationTest/`    | `*IT`     | `gradle integrationTest` | Spring context wiring, repository round-trips, controller integration |
| `uiTest`          | `src/uiTest/`             | `*UIT`    | `gradle uiTest`        | UI interactions via TestFX (clicks, key events, scene assertions) |
| `e2eTest`         | `src/e2eTest/`            | `*E2E`    | `gradle e2eTest`       | End-to-end flows — boot the application and exercise full user journeys |

A fifth source set, `testFixtures` (in `src/testFixtures/`), holds shared test utilities: `ApplicationTestBase`, `JavaFxSpringTest`, `JavaFxSpringTestConfiguration`, `JavaFxViewTestLauncher`. All four test source sets depend on it via Gradle's `java-test-fixtures` plugin.

Execution order in CI: `test` → `integrationTest` → `uiTest` → `e2eTest`.

When fixing a bug, look in the matching source set first. Most controller-level changes are best tested with an integration test (`*IT`); UI behavior assertions belong in a UI test (`*UIT`).

### Headless Testing

Tests run **headless by default** using OpenJFX Monocle. To watch them execute against a real screen — useful when an assertion is mysteriously timing out — pass `-Dtestfx.headless=false`:

```bash
gradle uiTest -Dtestfx.headless=false
```

On Linux, the headless run still requires GTK libraries and the `xvfb`/Monocle native bits — both are present on `ubuntu-latest` GitHub runners, but you may need to install them on minimal local containers.

### Writing Tests

- **Use `@DisplayName`** on JUnit Jupiter tests. Follow the pattern `<class under test> <verb> <object>` and avoid the word "should":
  - ✅ `@DisplayName("PlayerController plays the queued item on Spacebar")`
  - ❌ `@DisplayName("Should play queued item")`
- **Prefer Kotest's `StringSpec`** when adding new Kotest-based tests in Kotlin.
- **Don't use `private`** for instance variables in test classes — package-private (Java) or default (Kotlin) is the project convention.
- **Modify the closest existing test** when fixing a bug rather than adding a parallel test. Prefer raising assertions in the matching `*IT` or `*UIT` over a brand-new file unless the scenario is genuinely new.
- **Test fixtures live in `src/testFixtures/`** — extend the existing base classes rather than rolling your own JavaFX bootstrapping.

## Style Guidelines

The repository has no `.editorconfig` yet, no Checkstyle, no Spotless, and no ktlint, but it might will.
Use clean, readable code, which in practice means:

- **4-space indentation** for Java and Kotlin
- **K&R braces** (opening brace on the same line as the declaration)
- **camelCase** for variables and methods, **PascalCase** for types, `UPPER_SNAKE_CASE` for constants
- **Wildcard imports are accepted** (`import javafx.beans.property.*;`) — they're used liberally elsewhere in the codebase

### Java

- Constructor injection over field injection where practical
- `@Controller` for FXML controllers (paired with `@FxmlView("/fxml/<name>.fxml")`), `@Service` for business services, `@Component` otherwise
- Class-level **Javadoc on every class** describing the API responsibility — keep it about the contract, not the implementation history
- Javadoc every public **interface method**
- Only document **private methods** when they perform a non-obvious operation
- Add line comments only when explaining **why** — never **what** or **how**

### Documentation

- Update `README.md` and the [wiki](https://github.com/octaviospain/Musicott/wiki) when production behavior or build commands change
- Don't reference internal phases, tickets, or planning docs in source-tree documentation — that context belongs in the PR or commit message

## Project Structure

```
src/
├── main/
│   ├── java/net/transgressoft/musicott/   # Controllers, events, services, view layer
│   ├── kotlin/net/transgressoft/          # Configuration, settings repository, event subscribers
│   └── resources/
│       ├── fxml/                          # FXML view definitions
│       ├── css/                           # JavaFX stylesheets
│       └── application.yml                # Spring Boot config (web stack excluded)
├── test/                                  # Unit tests (*Test)
├── integrationTest/                       # Spring context tests (*IT)
├── uiTest/                                # TestFX UI tests (*UIT)
├── e2eTest/                               # End-to-end tests (*E2E)
└── testFixtures/                          # Shared test base classes and helpers
```

## Questions

If you have any questions or need help with the contribution process, please don't hesitate to [open an issue](https://github.com/octaviospain/Musicott/issues/new) asking for guidance.
