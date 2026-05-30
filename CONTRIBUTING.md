# Contributing

## Requirements

- JDK 17
- Android SDK with API 36

## Project Layout

- `visuals`: publishable Android library module
- `android-demo`: demo application that exercises the public API

## Build

```bash
./gradlew :visuals:assembleRelease :android-demo:assembleDebug
```

## Test

```bash
./gradlew :visuals:testDebugUnitTest
```

## Publish Locally

```bash
./gradlew :visuals:publishReleasePublicationToMavenLocal
```

## Pull Requests

- Keep changes focused.
- Update `README.md` when the public API or setup changes.
- Update `CHANGELOG.md` for user-visible library changes.
