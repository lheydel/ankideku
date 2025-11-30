# Claude Instructions for AnkiDeku v2

## Overview

AnkiDeku v2 is a **Kotlin Compose Desktop** application that helps users improve their Anki flashcards using AI. This is a complete rewrite from the original TypeScript/React + Express architecture.

**Tech Stack:**
- Kotlin + Compose Desktop (Material3)
- SQLDelight for local database
- Ktor for HTTP client (AnkiConnect)
- Koin for dependency injection
- Coroutines + StateFlow for reactive state

## Project Structure

```
src/main/kotlin/com/ankideku/
├── Main.kt                 # Entry point - window setup
├── App.kt                  # Root Composable with theme
├── di/
│   └── AppModule.kt        # Koin DI configuration
├── data/
│   ├── local/
│   │   ├── database/       # SQLDelight driver setup
│   │   ├── repository/     # SQL-backed repository implementations
│   │   └── service/        # Transaction service
│   ├── mapper/             # Domain <-> DB model mappers
│   └── remote/
│       ├── anki/           # AnkiConnect client
│       └── llm/            # LLM service abstraction (Claude CLI, Mock)
├── domain/
│   ├── model/              # Domain entities (Note, Session, Suggestion, etc.)
│   ├── repository/         # Repository interfaces
│   ├── service/            # Service interfaces
│   └── usecase/            # Business logic features
│       ├── deck/           # DeckFinder, SyncDeckFeature
│       ├── session/        # SessionFinder
│       ├── suggestion/     # SessionOrchestrator, ReviewSuggestionFeature
│       ├── history/        # HistoryFinder, SearchHistoryUseCase
│       └── settings/       # SettingsManager
├── ui/
│   ├── theme/              # Material3 theming (Color, Typography, Spacing)
│   ├── components/         # Reusable Composables
│   └── screens/
│       └── main/
│           ├── MainScreen.kt
│           ├── MainViewModel.kt
│           ├── MainUiState.kt
│           └── actions/    # ViewModel action delegates
└── util/                   # Utilities (TokenEstimator, JsonUtils, etc.)

src/main/sqldelight/        # SQL schema files (.sq)
src/main/resources/icons/   # App icons (ico, icns, png)
```

## Architecture

**Clean Architecture Layers:**
- **Data Layer** (`data/`): Repository implementations, external APIs, mappers
- **Domain Layer** (`domain/`): Business logic, models, interfaces
- **UI Layer** (`ui/`): Compose screens, ViewModel, components

**Key Patterns:**
- Repository pattern with domain interfaces + SQL implementations
- UseCase/Feature pattern for complex operations
- ViewModel with interface delegation to action classes
- Flow-based reactive programming with StateFlow

## Development Commands

```bash
# Run the application
./gradlew run

# Build distribution packages
./gradlew packageDistributionForCurrentOS

# Platform-specific packages
./gradlew packageMsi    # Windows
./gradlew packageDmg    # macOS
./gradlew packageDeb    # Linux

# Regenerate SQLDelight database interface
./gradlew generateMainAnkiDekuDatabaseInterface
```

## Database

**SQLDelight** manages the local SQLite database with schema files in `src/main/sqldelight/`:
- `DeckCache.sq` - Cached deck metadata and notes
- `Session.sq` - LLM processing sessions
- `Suggestion.sq` - AI-generated suggestions
- `History.sq` - Accepted/rejected suggestion records
- `Setting.sq` - User configuration
- `FieldValue.sq` - Note field values

**Storage Location:**
- Windows: `%APPDATA%/AnkiDeku/ankideku.db`
- macOS: `~/Library/Application Support/AnkiDeku/ankideku.db`
- Linux: `~/.config/AnkiDeku/ankideku.db`

## External Integrations

**AnkiConnect** (Port 8765):
- Client: `AnkiConnectClient` using Ktor HTTP
- Monitor: `AnkiConnectionMonitor` polls for availability
- Key operations: `getDeckNamesAndIds()`, `notesInfo()`, `updateNoteFields()`

**LLM Service:**
- Interface: `LlmService` with implementations for Claude CLI and Mock
- Factory: `LlmServiceFactory` selects provider based on settings

## Code Practices

**Compose/Kotlin Standards:**
- Use `StateFlow` for reactive state management
- Delegate ViewModel actions to specialized action classes
- Keep Composables focused and extract reusable components
- Use mappers for clean domain <-> data layer conversion
- Handle errors with sealed classes or Result types
- **NEVER use `Palette` directly in components** - Always use `LocalAppColors.current` for colors to ensure proper light/dark theme support

**Key Files:**
- `MainViewModel.kt` - Central state management with action delegation
- `MainUiState.kt` - Immutable UI state data class
- `AppModule.kt` - All DI bindings in one place
- `SyncDeckFeature.kt` - Deck sync workflow with progress emission

## Important Reminders

1. **Java 17+ required** - Project uses JVM toolchain 17
2. **Anki must be running** - App connects to AnkiConnect on port 8765
3. **SQLDelight codegen** - Run `./gradlew generateMainAnkiDekuDatabaseInterface` after schema changes
4. **Clean Architecture** - Respect layer boundaries (UI -> Domain <- Data)
5. **NEVER run the app yourself** - Always ask the user to run `./gradlew run` to test changes

## Temporary note relevant while migrating from V1 to V2

- When user mentions "V1" or "version 1", they are referring to the original AnkiDeku application built with TypeScript/React and Express. "V2" or "version 2" refers to the new Kotlin Compose Desktop rewrite.
- NEVER mention "V1" or "V2" in user-facing text, documentation or codebase. This is only for internal understanding between you and the user.
- "V1" and "V2" terms are only temporary. The new Kotlin Compose Desktop application will simply be called "AnkiDeku" going forward and V1 will be completely forgotten.
