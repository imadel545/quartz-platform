# Quartz Mobile Android

Fondation Android Quartz (Kotlin + Compose + MVVM + Clean Architecture + Hilt + Room + WorkManager).

## Structure

- `app/src/main/java/com/quartz/platform/presentation`: UI Compose, navigation, ViewModel
- `app/src/main/java/com/quartz/platform/domain`: modèles métier, repositories, use cases
- `app/src/main/java/com/quartz/platform/data`: Room, repositories offline-first, sync worker
- `app/src/main/java/com/quartz/platform/device`: intégrations device (network monitor)
- `app/src/main/java/com/quartz/platform/core`: dispatchers, logging, DI core

## Décisions de baseline

- Pas de seed de faux sites en production: la base locale reste vide tant que la synchro n'a pas injecté de données réelles.
- La queue de sync persiste des références d'agrégats (pas de payload JSON arbitraire) pour éviter le drift de données.

## Validation locale

1. Ouvrir `mobile-android/` dans Android Studio.
2. Lancer sync Gradle.
3. Exécuter `app` puis `testDebugUnitTest`.
