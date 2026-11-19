# AI Agent Instructions for NeverMiss Development

This document serves as the absolute source of truth for any AI agent or LLM assisting with the development of the **NeverMiss** project. You must strictly adhere to these rules, architectural guidelines, and development philosophies before generating or modifying any code.

## 1. Core Development Philosophy & Architecture

- **Goal:** NeverMiss is a high-performance, native Android streak and habit tracking application built with Kotlin, Jetpack Compose, Coroutines/Flow, Material Design 3, Room Database, Dagger Hilt, WorkManager, and Android's UsageStatsManager for automatic application streak tracking and multi-account sub-streaks.
- **Flawless Execution:** The app MUST work smoothly without any bottleneck bugs. Regressions, UI lag, and stuttering (especially during habit list scrolling, app picker browsing, and background reconciliation) are unacceptable.
- **Zero Crash Tolerance:** Improve code robustness to ensure the app does not crash under any circumstances. Always prioritize graceful degradation (e.g., showing an error state, fallback UI, or empty list) over throwing unhandled exceptions.
- **No Hallucinations:** Only use existing APIs, classes, and resources within the project. If you are unsure about an existing implementation, ask the developer to fetch the file contents. Also, for checking you are not hallucinating, start every chat with 'Hey Devson' words.
- **One Line Explanation:** Do not explain things in long phrases and paragraphs; just be short and precise and do not explain things if it is not required.

## 2. UI / Jetpack Compose Guidelines

- **Framework:** Jetpack Compose is the exclusive UI framework. Avoid legacy XML layouts entirely for UI screens (XML is only permitted for AndroidManifest, drawables, vector assets, widgets, and basic values).
- **Design Principle:** Always consider a mobile-first approach to UI styling and layouts, ensuring touch targets are accurate, responsive, and intuitive for native mobile usage.
- **Material Design:** Strictly utilize Material Design 3 (M3) components (`androidx.compose.material3.*`) to ensure a clean, native experience.
- **Composables & Recomposition:** Keep composable functions highly focused and modular. Prevent unnecessary recompositions by ensuring StateFlow updates are targeted.
- **State Hoisting:** Prefer state hoisting for UI components to keep them stateless. **Never** perform heavy calculations, object allocations, I/O, or file operations directly within composable functions.

## 3. Code Quality & Performance Optimization

- **Language:** Kotlin is the exclusive programming language.
- **Asynchronous Operations:** Use Kotlin Coroutines and Flows (`StateFlow`/`SharedFlow`) for all asynchronous programming and state observation.
- **Null Safety & Crash Prevention:** Handle nullable types safely and exhaustively. **Never** use the not-null assertion operator (`!!`). Catch specific exceptions rather than generic `Exception` where possible, and ensure errors are pushed to the UI state rather than silently crashing the app.
- **Eliminate Bottlenecks:**
  - **Disk I/O:** Always dispatch database, MediaStore/PackageManager queries, or file reading/writing operations to `Dispatchers.IO`. Never hit the file system or run heavy queries on the Main thread.
  - **Memory Management:** Be aggressive about optimizing memory. Avoid unnecessary object allocations during list rendering or background scans.
  - **Concurrency:** Ensure tasks like scanning installed apps or querying usage events are properly dispatched to background coroutines, avoiding UI freezes.
- **Code Writing Format:** Do not add '─' anywhere in the code files.

## 4. Documentation & Update Tracking

You must actively maintain the project's changelog. After every completed task, error resolution, or feature addition, you must append an entry to the `update_details.md` file.

**Format and Rules for `update_details.md`:**

- Do NOT read or rewrite the whole file every time. Simply append the new data at the very end of the document.
- Include a Date and Time stamp for the update.
  Whenever a fix, optimization, or feature is completed, you MUST document it using the following format:

- **Issue:** (Briefly describe the exact issue or bottleneck that was just solved)
- **Type:** (Specify the category: e.g., Error, Bug, UI, Performance, Architecture, Feature)
- **Solution:** (Explain how the issue was solved. Maximum 10 lines.)
- After the details of the latest update, you must append exactly `---` on a new line to close out that specific session.
- Do not include any conversational filler in the file.

## 5. Version Control (Git) Protocol

- **Do not commit or push** any changes to the repository until explicitly being asked to do so by the developer.
- **Do not Change .gitignore:** Never make changes automatically in any of the .gitignore; I will make changes in it manually. Let app/.gitignore remain as it is.
