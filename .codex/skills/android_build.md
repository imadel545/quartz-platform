# Android Build Skill

## Objective

Bootstrap a serious Android foundation for Quartz Platform that can open, sync, build, and evolve cleanly in Android Studio.

## Target stack
- Kotlin
- Jetpack Compose
- MVVM
- Clean Architecture
- Hilt
- Room
- WorkManager
- Coroutines / Flow

## Mandatory expectations
- Kotlin-first setup
- Android Studio compatible structure
- clear package namespace
- Gradle coherence
- minimal but runnable app shell
- application class
- main activity
- theme and navigation baseline
- DI baseline
- local persistence baseline scaffold
- sync scaffold
- test baseline

## Architecture discipline
- no business logic in composables
- no networking inside ViewModels
- no persistence code directly in UI
- package structure must reflect documented architecture
- avoid fake enterprise complexity without runnable substance

## Validation
Whenever feasible:
- run a relevant Gradle build or verification command
- report actual commands executed
- state clearly what could not be validated