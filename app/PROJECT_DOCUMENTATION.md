# VocaAI Project Documentation

## 1. Project overview

VocaAI is a single-module Android application generated from Google AI Studio and implemented with Kotlin, Jetpack Compose, Room, Retrofit/Moshi, and the Google Gemini REST API. The app is positioned as a personal AI vocal coach: it walks users through onboarding and mock authentication, simulates singing-session pitch tracking, stores session analytics locally, offers an AI coach chat backed by Gemini when a valid API key is present, and lets users customize an animated coach avatar.

The current implementation is intentionally hybrid:

- **Production-ready local architecture:** Compose UI, Android Navigation, ViewModel state management, Room persistence, dependency cataloging, Gradle configuration, and tests are wired together.
- **Simulated vocal analysis:** recording, pitch points, notes, scores, and post-session feedback are generated in `VocaViewModel` rather than from microphone DSP.
- **Live AI chat path:** coach chat calls Gemini through Retrofit when `GEMINI_API_KEY` is configured; otherwise it falls back to deterministic offline coaching replies.
- **Future backend placeholders:** Retrofit endpoints and data models exist for a hypothetical VocaAI vocal-analysis backend.

## 2. How the app is organized

```text
.
├── app/                         Android application module
│   ├── src/main/java/com/example
│   │   ├── MainActivity.kt       Compose entry point and navigation graph
│   │   ├── database/             Room DAO and database singleton
│   │   ├── models/               Room entities and API DTOs
│   │   ├── network/              Retrofit/Gemini API clients
│   │   ├── repository/           Data and AI-coach repository layer
│   │   ├── ui/                   Compose screens and visual components
│   │   └── viewmodel/            App state, auth mock, recording simulator
│   ├── src/main/res/             Android resources and launcher assets
│   ├── src/test/                 JVM/Robolectric/screenshot tests
│   └── src/androidTest/          Instrumented Android test
├── gradle/                       Version catalog
├── build.gradle.kts              Root Gradle plugin declarations
├── settings.gradle.kts           Repository and module settings
└── metadata.json                 AI Studio app metadata
```

## 3. Runtime architecture

### 3.1 UI and navigation flow

`MainActivity` creates the Compose content tree, applies `MyApplicationTheme`, owns the `NavHost`, and wires a shared `VocaViewModel` into every screen. The main navigation destinations are:

1. `splash` → animated launch screen.
2. `onboarding` → feature walkthrough.
3. `login` → mock email/social authentication screen.
4. `dashboard` → app hub with score cards and feature shortcuts.
5. `singing_session` → simulated pitch-tracking session.
6. `ai_coach` → Gemini/offline AI coach chat.
7. `performance_analysis` → newest session breakdown.
8. `progress` → historical analytics charts and session history.
9. `profile` → user/vocal profile settings.

When `authState` becomes signed out, `MainActivity` redirects back to `login` and clears the previous back stack.

### 3.2 State management

`VocaViewModel` is the central state holder. It exposes `StateFlow` values for:

- authentication state,
- session history,
- chat history,
- avatar preferences,
- recording state,
- active pitch/frequency/note values,
- loading/analyzing flags,
- the latest completed `UserSession`.

It also contains the mock behavior for login/register/reset flows, recording simulation, pitch stream updates, generated score analysis, chat submission, and history clearing.

### 3.3 Persistence

Room stores three tables:

- `user_sessions`: historical vocal-coaching session summaries.
- `chat_messages`: AI coach conversation history.
- `avatar_preferences`: one-row avatar customization settings.

`VocaDatabase` builds a singleton `RoomDatabase` named `voca_database`, while `VocaRepository` wraps DAO calls on `Dispatchers.IO` and exposes reactive flows to the ViewModel.

### 3.4 AI integration

The coach chat path in `VocaRepository.askGeminiCoach`:

1. Reads `BuildConfig.GEMINI_API_KEY`, injected by the Secrets Gradle Plugin from `.env`.
2. Falls back to offline coaching text when the key is missing or left as `MY_GEMINI_API_KEY`.
3. Sends the latest user input plus the last eight chat messages to Gemini.
4. Builds a personalized system instruction from the avatar name, theme, style, and coaching persona.
5. Returns the first generated candidate text, or a safe offline response on network/API failure.

## 4. Setup and local development

### Prerequisites

- Android Studio with Android Gradle Plugin support.
- JDK compatible with the configured Android Gradle Plugin.
- Android SDK platforms for compile/target SDK 36.
- Optional Gemini API key for live AI coach responses.

### Local run steps

1. Open the repository root in Android Studio.
2. Let Gradle sync the `:app` module.
3. Copy `.env.example` to `.env`.
4. Replace `MY_GEMINI_API_KEY` with a valid Gemini API key if live AI chat is desired.
5. Ensure `debug.keystore` exists or remove/adjust the custom debug signing config in `app/build.gradle.kts`.
6. Run the `app` configuration on an emulator or device.

### Useful Gradle commands

```bash
./gradlew test
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

`connectedDebugAndroidTest` requires a running emulator or attached Android device.

## 5. File-by-file documentation

The list below documents all tracked project files. Build outputs such as `.gradle/`, `.git/`, `build/`, and `app/build/` are generated or local-only folders and are intentionally excluded from source documentation.

### Root files

| File | Purpose |
| --- | --- |
| `.env.example` | Template for local secrets. Defines `GEMINI_API_KEY=MY_GEMINI_API_KEY`, which the Secrets Gradle Plugin uses as a default value when `.env` is not present. |
| `.gitignore` | Excludes IDE metadata, Gradle/Kotlin outputs, local Android configuration, native build outputs, captures, `.env`, and `debug.keystore` from version control. |
| `README.md` | Quickstart generated by AI Studio. Points to the AI Studio app and explains how to open/run the Android project locally. |
| `PROJECT_DOCUMENTATION.md` | This comprehensive project guide, including architecture, setup, and file-purpose documentation. |
| `build.gradle.kts` | Root Gradle build file. Declares Android, Kotlin Compose, KSP, Roborazzi, and Secrets Gradle plugins via aliases, with `apply false` so modules opt in explicitly. |
| `gradle.properties` | Project-wide Gradle/Kotlin/Android settings, including JVM memory, parallelism, build cache, configuration cache, official Kotlin style, non-transitive Android resources, and worker limits. |
| `gradle/libs.versions.toml` | Gradle version catalog. Centralizes dependency/plugin versions and aliases for AndroidX, Compose, Room, Retrofit, Moshi, OkHttp, Coroutines, Robolectric, Roborazzi, Firebase, KSP, AGP, Kotlin, and the Secrets plugin. |
| `metadata.json` | AI Studio metadata naming the app `VocaAI`, describing its AI vocal-coach purpose, and declaring server-side Gemini capability. |
| `settings.gradle.kts` | Gradle settings. Configures plugin repositories, dependency repositories, the Foojay toolchain resolver convention, root project name, and includes the `:app` module. |

### App module configuration

| File | Purpose |
| --- | --- |
| `app/.gitignore` | Ignores the app module's `/build` output directory. |
| `app/build.gradle.kts` | Android app module build script. Applies app/Kotlin/KSP/Roborazzi/Secrets plugins; sets namespace, SDKs, application ID, signing configs, build types, Java 11 compatibility, Compose/buildConfig features, test resource behavior, secrets file names, and all app/test dependencies. |
| `app/proguard-rules.pro` | Placeholder ProGuard/R8 rules file used by the release build type. Currently contains default comments and no active custom keep rules. |

### Android manifest and resources

| File | Purpose |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | Declares package-level application configuration, backup/data-extraction rules, app icon resources, RTL support, app theme, and the exported launcher `MainActivity`. |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Vector background layer for adaptive launcher icons. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Vector foreground layer for adaptive launcher icons. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive launcher icon definition combining foreground/background drawable layers for API 26+. |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | Round adaptive launcher icon definition for API 26+. |
| `app/src/main/res/mipmap-hdpi/ic_launcher.webp` | Density-specific legacy launcher icon bitmap for hdpi devices. |
| `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp` | Density-specific round legacy launcher icon bitmap for hdpi devices. |
| `app/src/main/res/mipmap-mdpi/ic_launcher.webp` | Density-specific legacy launcher icon bitmap for mdpi devices. |
| `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp` | Density-specific round legacy launcher icon bitmap for mdpi devices. |
| `app/src/main/res/mipmap-xhdpi/ic_launcher.webp` | Density-specific legacy launcher icon bitmap for xhdpi devices. |
| `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp` | Density-specific round legacy launcher icon bitmap for xhdpi devices. |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp` | Density-specific legacy launcher icon bitmap for xxhdpi devices. |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp` | Density-specific round legacy launcher icon bitmap for xxhdpi devices. |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` | Density-specific legacy launcher icon bitmap for xxxhdpi devices. |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp` | Density-specific round legacy launcher icon bitmap for xxxhdpi devices. |
| `app/src/main/res/values/colors.xml` | Legacy XML color resources generated by the Android template. Most runtime colors are defined in Compose theme Kotlin files. |
| `app/src/main/res/values/strings.xml` | String resources. Defines `app_name` as `VocaAI`. |
| `app/src/main/res/values/themes.xml` | XML theme resource. Defines `Theme.MyApplication` as a no-action-bar DeviceDefault theme used by the manifest before Compose takes over styling. |
| `app/src/main/res/xml/backup_rules.xml` | Android full-backup rules placeholder, currently with no custom include/exclude rules enabled. |
| `app/src/main/res/xml/data_extraction_rules.xml` | Android 12+ cloud/device-transfer data extraction rules placeholder, currently with TODO comments and no custom rules enabled. |

### Main Kotlin source

| File | Purpose |
| --- | --- |
| `app/src/main/java/com/example/MainActivity.kt` | Android entry point. Enables edge-to-edge drawing, starts Compose, applies the app theme, creates the shared `VocaViewModel`, watches auth state, and declares the full Compose Navigation graph. |
| `app/src/main/java/com/example/database/VocaDatabase.kt` | Room persistence layer. Defines `VocaDao` CRUD/query methods for sessions, chat, and avatar preferences; defines `VocaDatabase` with three entities; builds the singleton database instance. |
| `app/src/main/java/com/example/models/DataModels.kt` | Shared data models. Contains Room entities (`UserSession`, `ChatMessage`, `AvatarPreferences`) plus DTOs for future audio upload responses, analysis results, coaching recommendations, performance reports, and daily score chart data. |
| `app/src/main/java/com/example/network/RetrofitClient.kt` | Network layer. Defines Moshi-compatible Gemini request/response DTOs, Retrofit interfaces for the placeholder VocaAI backend and the live Gemini `generateContent` endpoint, and singleton Retrofit services backed by OkHttp/Moshi. |
| `app/src/main/java/com/example/repository/VocaRepository.kt` | Repository layer. Exposes DAO flows, performs Room writes on the IO dispatcher, initializes default avatar preferences, sends AI coach requests to Gemini, and provides offline fallback coach replies. |
| `app/src/main/java/com/example/viewmodel/VocaViewModel.kt` | App state and business logic. Owns authentication mock state, Room-backed flows, avatar updates, chat sending/clearing, AI loading flags, simulated recording state, generated pitch stream data, post-session scoring, and history clearing. |
| `app/src/main/java/com/example/ui/VocaScreens.kt` | Main Compose screen collection. Implements splash, onboarding, auth, dashboard, singing session, pitch graph canvas, coach chat, chat bubbles, avatar customizer dialog, performance analysis, score widgets, progress tracking, analytics chart, history rows, profile, and reusable dashboard cards/rows. |
| `app/src/main/java/com/example/ui/components/AvatarVisual.kt` | Custom animated avatar component. Draws the AI companion using Compose layout, gradients, Canvas shapes, preferences-driven colors/styles, and a glow modifier for listening/idle states. |
| `app/src/main/java/com/example/ui/theme/Color.kt` | Compose color palette constants for dark, light, and template Material colors used by the app theme and UI components. |
| `app/src/main/java/com/example/ui/theme/Theme.kt` | Compose Material 3 theme setup. Defines dark/light color schemes and `MyApplicationTheme`, currently using custom static palettes instead of dynamic color. |
| `app/src/main/java/com/example/ui/theme/Type.kt` | Compose Material typography setup. Overrides `bodyLarge` and leaves other Material typography defaults available for future customization. |

### Tests and test assets

| File | Purpose |
| --- | --- |
| `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt` | Device/emulator instrumentation smoke test verifying the target app package name. |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt` | JVM Robolectric test that loads Android resources and verifies the `app_name` string is `VocaAI`. |
| `app/src/test/java/com/example/ExampleUnitTest.kt` | Basic JVM unit-test template confirming the local test runner works. |
| `app/src/test/java/com/example/GreetingScreenshotTest.kt` | Roborazzi/Compose screenshot test that renders `AvatarCompanion` with default-like preferences and captures `src/test/screenshots/greeting.png`. |
| `app/src/test/screenshots/greeting.png` | Baseline/output screenshot asset for the avatar companion screenshot test. |

## 6. Important classes and responsibilities

### `UserSession`

Represents one completed practice session. It stores timing, overall score, individual metric scores, detected notes, average frequency, and the coach feedback suggestion shown in analysis/history screens.

### `ChatMessage`

Represents a single chat bubble in the coach conversation. `sender` is expected to be `user` or `ai`, and messages are displayed chronologically.

### `AvatarPreferences`

Stores the customization state for the companion avatar. The database uses a single row with primary key `1`.

### `VocaDao`

Groups all Room queries and writes for session history, chat messages, and avatar preferences.

### `VocaRepository`

Keeps the ViewModel independent from raw DAO and Retrofit details. It is the only layer that knows whether an AI coach reply came from Gemini or from the offline fallback generator.

### `VocaViewModel`

Coordinates app behavior for Compose screens. It initializes default chat/avatar content, transforms repository flows into `StateFlow`, and simulates real-time singing session telemetry.

### `AvatarCompanion`

A reusable visual component that renders the coach avatar based on gender, hairstyle, outfit, and theme preferences. It is used by the chat/customization experience and screenshot test.

## 7. Data lifecycle

### Session lifecycle

1. User opens the singing session screen.
2. `toggleRecording()` starts a coroutine that emits simulated pitch values every 350 ms.
3. User stops recording.
4. `analyzeFinishedSession()` waits briefly, generates metrics, inserts a `UserSession`, and updates `lastSessionResult`.
5. Analysis, dashboard, and progress screens consume the saved session data through ViewModel state flows.

### Chat lifecycle

1. User sends text in the coach chat screen.
2. ViewModel inserts the user `ChatMessage`.
3. ViewModel sets AI loading state and calls `repository.askGeminiCoach`.
4. Repository uses Gemini or offline fallback.
5. ViewModel inserts the AI `ChatMessage`.
6. The chat screen observes the Room-backed history flow and redraws.

### Avatar preference lifecycle

1. App startup reads the one-row avatar config from Room.
2. If missing, repository inserts default preferences.
3. Customizer updates are written through `updateAvatarConfig`.
4. UI observes `avatarConfigFlow` so the avatar and coach personality update reactively.

## 8. Known limitations and future work

- **No real microphone processing yet:** pitch, notes, and scores are simulated in the ViewModel.
- **Mock authentication:** login/register/social auth are local state transitions, not backed by Firebase or a server.
- **Placeholder VocaAI backend:** Retrofit endpoints for audio upload/reports/recommendations are declared but not used by current screens.
- **Release signing needs real secrets:** release builds expect keystore credentials through environment variables or `my-upload-key.jks`.
- **Backup rules are placeholders:** XML backup/data-extraction files should be customized before production release.
- **Package mismatch in instrumented test:** the instrumentation test expects `com.example`, while `applicationId` is `com.aistudio.vocaai.kxmjy2`; this should be revisited before relying on connected tests.

## 9. Quick contributor checklist

- Keep UI-only changes in `ui/` or `ui/components/` when possible.
- Keep persistent schema changes in `models/DataModels.kt` and `database/VocaDatabase.kt`, and increment the Room database version.
- Add repository methods when UI/ViewModel code needs new persisted or network-backed operations.
- Add ViewModel `StateFlow` values for screen state that must survive recomposition.
- Prefer version-catalog aliases in `gradle/libs.versions.toml` for new dependencies.
- Run `./gradlew test` before opening a pull request.
