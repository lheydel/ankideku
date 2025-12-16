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

**Migrations** are in `src/main/sqldelight/migrations/` (e.g., `001.sqm`, `002.sqm`).

**IMPORTANT: When modifying database schema, BOTH are needed:**
1. Update the `.sq` file with the new schema → used by `Schema.create()` for NEW users
2. Create a migration file (next number, e.g., `003.sqm`) → used by `Schema.migrate()` for EXISTING users

New users don't run migrations - they get the schema directly from .sq files. Existing users only run migrations. Both paths must result in the same final schema.

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
- **Reusability is a must** - Always reuse existing components, especially generic ones like `AppButton`, `DialogContent`, `AppDialog`. Check `ui/components/` before creating new UI elements. Never duplicate button styles, dialog layouts, or common patterns.

**Key Files:**
- `MainViewModel.kt` - Central state management with action delegation
- `MainUiState.kt` - Immutable UI state data class
- `AppModule.kt` - All DI bindings in one place
- `SyncDeckFeature.kt` - Deck sync workflow with progress emission

## UI Component Organization

**Directory Structure for Large Components:**
When a UI component grows beyond ~300-400 LOC, split it into a subdirectory:
```
ui/components/
├── ComponentName/
│   ├── ComponentName.kt    # Main composable (routing/orchestration only)
│   ├── SubComponent1.kt    # Extracted sub-component
│   ├── SubComponent2.kt    # Extracted sub-component
│   └── Helpers.kt          # Shared utilities, styles, builders
```

**Current Component Directories:**
- `queue/` - QueuePanel and related components (tabs, cards, content)
- `sidebar/` - SidebarPanel, header, deck selector, chat areas
- `comparison/` - ComparisonPanel, cards, styles, copy builders
- `batch/` - Batch conflict dialog

**Reusable Patterns:**
- `DialogContent.kt` - Standard dialog layout (title, message, content slot, buttons)
- `ActionHelpers.kt` - Loading state wrappers (`withActionLoading`, `withBatchProcessing`), `resetEditState`
- `SyncProgressMapper.kt` - Extension function `SyncProgress.toUi()` for progress display

**When Refactoring UI:**
1. Split files >400 LOC into focused components
2. Extract duplicated patterns into shared helpers/composables
3. Use extension functions for domain → UI mappings
4. Keep main component as thin routing/orchestration layer
5. Verify build with `./gradlew compileKotlin` after each change

**File Structure Matters:**
- Never dump many files in a single folder - organize into logical subdirectories
- Related components belong together (e.g., `queue/QueuePanel.kt`, `queue/QueueCard.kt`)
- Generic reusable components stay in `ui/components/` root (e.g., `AppButton.kt`, `DialogContent.kt`)
- Feature-specific components go in subdirectories (e.g., `sidebar/`, `comparison/`, `queue/`)
- When a folder grows beyond 8-10 files, consider further organization

## Important Reminders

1. **Java 17+ required** - Project uses JVM toolchain 17
2. **Anki must be running** - App connects to AnkiConnect on port 8765
3. **SQLDelight codegen** - Run `./gradlew generateSqlDelightInterface` after schema changes
4. **Clean Architecture** - Respect layer boundaries (UI -> Domain <- Data)
5. **NEVER run the app yourself** - Always ask the user to run `./gradlew run` to test changes
6. **"Memorize"** - When the user asks to "memorize" something, it means update this CLAUDE.md file with the information
7. **No automatic backward compatibility** - Always ask the user before adding backward compatibility code (aliases, shims, re-exports, etc.). Many cases in this project don't need it.
8. **Code quality over speed** - Always prioritize maintainability and clean code. If implementing a feature properly requires refactoring existing code, do it. If the refactoring is consequent (multi-file, architectural changes), ask the user for confirmation before proceeding.
