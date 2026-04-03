# Runtime Debug Visibility Policy (Android)

This policy defines what is visible in normal operator/reviewer runtime versus debug-only paths.

## Principles

- Runtime mission flow must stay operator/reviewer-first.
- Debug/simulation panels are never primary UI.
- Advanced technical controls require explicit disclosure.
- No fake telecom automation is exposed as runtime behavior.

## Current enforcement

- `ReportDraft` developer sync tools are disclosure-gated and only shown when debug simulation control is actually available.
- QoS mission runtime keeps diagnostics, script editing, and timeline internals in advanced disclosure sections.
- Control Tower keeps queue urgency/actions primary and moves tuning/advanced controls behind disclosure.

## Implementation references

- `mobile-android/app/src/main/java/com/quartz/platform/presentation/report/draft/ReportDraftViewModel.kt`
- `mobile-android/app/src/main/java/com/quartz/platform/presentation/report/draft/ReportDraftScreen.kt`
- `mobile-android/app/src/main/java/com/quartz/platform/presentation/performance/session/PerformanceSessionScreen.kt`
- `mobile-android/app/src/main/java/com/quartz/platform/presentation/reviewer/controltower/ReviewerControlTowerScreen.kt`

