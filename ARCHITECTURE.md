# Flow Tasks — Architecture & Engineering Specification

## 1. Overview & Core Philosophy
Flow Tasks is an AI-powered, local-first productivity and task management platform built for Android. 

### Why Local-First?
- **Zero Latency**: Read and write operations execute directly on the local SQLite/Room engine.
- **Offline Reliability**: The application functions without an internet connection.
- **Data Ownership & Privacy**: User tasks, notes, schedules, and priorities remain on the user's device.
- **Zero Seed Data Policy**: Flow Tasks contains **no hardcoded demo data, fake lists, or simulated metrics**. The system starts in a clean empty state, and all visual state and statistics derive strictly from the database.

---

## 2. Architecture & Data Flow

Flow Tasks adheres to Clean Architecture principles with unidirectional data flow (UDF):

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  Jetpack Compose Screens & M3 Components (Theme, Widgets)   │
└──────────────────────────────▲──────────────────────────────┘
                               │ StateFlow<UiState> / Events
┌──────────────────────────────┴──────────────────────────────┐
│                      ViewModel Layer                        │
│  State management, UI logic, Coroutine Scopes, Mappers      │
└──────────────────────────────▲──────────────────────────────┘
                               │ Flow<Data> / Suspend calls
┌──────────────────────────────┴──────────────────────────────┐
│                       Domain Layer                          │
│  Pure Kotlin Models (Task, TaskList), UseCases, AI Contract │
└──────────────────────────────▲──────────────────────────────┘
                               │ Repositories
┌──────────────────────────────┴──────────────────────────────┐
│                        Data Layer                           │
│  Room DB (TaskDao, TaskListDao), Entities, Converters,       │
│  DataStore Preferences (Theme, Defaults), Mappers           │
└─────────────────────────────────────────────────────────────┘
```

### Unidirectional Data Flow
1. **User Action**: The user performs an action in Compose (e.g., adds task, checks box, sets date).
2. **ViewModel Event**: The Composable invokes a method on the `ViewModel`.
3. **Use Case Execution**: The `ViewModel` delegates execution to a dedicated Domain `UseCase`.
4. **Repository & DAO**: The `Repository` coordinates persistence to Room SQLite and/or DataStore.
5. **Reactive Flow**: Room emits updated dataset via Kotlin `Flow`.
6. **State Emission**: `ViewModel` transforms updates into an immutable `UiState` exposed as a `StateFlow`.
7. **UI Recomposition**: Jetpack Compose renders the updated state.

---

## 3. Layer Responsibilities

### Presentation Layer (`com.flowtasks.app.feature.*`, `com.flowtasks.app.core.designsystem.*`)
- Pure Jetpack Compose UI utilizing Material Design 3.
- Testable components tagged with explicit test tags.
- Dynamic theme switching (Light, Dark, System default).
- Empty state rendering when the database contains no items.

### Domain Layer (`com.flowtasks.app.domain.*`)
- **Entities & Models**: Pure, platform-agnostic representations (`Task`, `TaskList`, `TaskPriority`, `TaskFilter`, `TaskSortOrder`).
- **Use Cases**: Single-responsibility domain interactions (`GetTasksUseCase`, `CreateTaskUseCase`, `ToggleTaskCompletionUseCase`, etc.).
- **Contracts**: Repository definitions (`TaskRepository`, `TaskListRepository`, `SettingsRepository`) and future AI abstractions (`AIProvider`, `AIService`).

### Data Layer (`com.flowtasks.app.data.*`, `com.flowtasks.app.core.database.*`, `com.flowtasks.app.core.datastore.*`)
- **AppDatabase (Room)**: Manages SQLite database versioning, schema, tables, and relationships.
- **TaskDao & TaskListDao**: Query contracts for reactive and direct persistence operations.
- **Mappers (`TaskMapper`)**: Converts between raw database entities (`TaskEntity`) and domain models (`Task`).
- **UserPreferencesDataStore**: Manages lightweight configuration (theme, default priority, display preferences).

---

## 4. Future AI Architecture & BYOK Strategy

Flow Tasks is architected for extensible Bring-Your-Own-Key (BYOK) AI features in subsequent phases:

- **Abstractions (`com.flowtasks.app.domain.ai`)**:
  - `AIProvider`: Interface for AI backends (Gemini, OpenAI, Anthropic, local on-device SLMs).
  - `AIService`: Manages active providers and task augmentation requests.
- **Decoupled Architecture**: AI components consume standard domain models (`Task`) and return suggested structured outputs (`AISuggestedTask`) without coupling domain business logic to third-party SDKs.
- **Security**: Sensitive keys are loaded at runtime or securely via Android Keystore / encrypted DataStore, never hardcoded in source.

---

## 5. Future Cloud Sync

- The Room schema includes `created_at`, `updated_at`, and clean relational keys (`parent_task_id`, `list_id`), providing a foundation for differential timestamp-based conflict resolution (CRDT / Last-Write-Wins) when cloud synchronization is introduced.
