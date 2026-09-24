# Seyahatname

[![CI](https://github.com/ogzhngms/seyahatname-android/actions/workflows/ci.yml/badge.svg)](https://github.com/ogzhngms/seyahatname-android/actions/workflows/ci.yml)

An Android trip planner built with Jetpack Compose and Claude. The app asks six short questions, one at a time: where you are going, for how long, who you are travelling with, your budget, what you want to do and the pace you like. When you confirm, it turns the answers into a prompt, asks Claude for the plan as JSON that follows a fixed schema, and renders that JSON as a day-by-day itinerary.

The name comes from Evliya Çelebi's *Seyahatname*, the 17th-century Ottoman book of travels.

| Questions | Confirm (with the prompt) | Plan | Raw JSON |
|---|---|---|---|
| ![Question step](docs/screenshots/1-question.png) | ![Confirm screen](docs/screenshots/2-confirm.png) | ![Plan](docs/screenshots/3-plan.png) | ![JSON](docs/screenshots/4-json.png) |

## How it works

```
answers ─► buildPrompt() ─► Claude (structured output) ─► JSON ─► parseItinerary() ─► Compose screens
```

- **Prompt:** `Trip.kt` turns the answers into a short English prompt and asks for the reply in the phone's language.
- **Claude call:** `ClaudePlanner.kt` uses the official Anthropic Java SDK with `claude-opus-5`. `output_config.format` holds `ITINERARY_SCHEMA`, so the reply is always JSON with exactly the fields the app reads. `fallbacks: "default"` lets the API retry on another model if Opus 5 declines a request.
- **Parsing and UI:** `Itinerary.kt` parses the JSON with `org.json`. `TripViewModel` moves through the question, confirm, loading, result and error screens.
- **Demo mode:** without an API key, the app plays back a bundled sample plan (`res/raw`, English and Turkish), so the whole flow works offline.

The interface is in English and Turkish and follows the device language.

## Run it

1. Open the project in Android Studio.
2. Add an Anthropic API key to `local.properties` (this file is git-ignored):
   ```properties
   ANTHROPIC_API_KEY=sk-ant-...
   ```
3. Run the `app` configuration. Without a key, the app starts in demo mode.

> **Note:** the key is compiled into the APK through `BuildConfig`. That is fine for local testing, but anyone who has the APK can extract the key. Before publishing, move the Claude call behind a small backend.

## Tests

```bash
./gradlew testDebugUnitTest            # JVM: prompt, schema, parser, ViewModel flow, SDK contract
./gradlew connectedDebugAndroidTest    # device: Compose UI walk-through of the wizard
```

- `ClaudePlannerTest` runs the real SDK against a local fake of the Messages API. It checks the request (model, schema, fallback) and the refusal, truncation and bad-key paths.
- `ItineraryTest` checks that every schema object is closed and fully required, and that the sample plans match the schema.
- `TripViewModelTest` covers the happy path, a failure followed by a retry, a reply that breaks the schema, and cancelling while the plan is loading.
- `TripFlowTest` taps through all six questions on a device with a stub planner and checks the plan on screen.

CI runs the unit tests and a debug build on every push.

## Stack

Kotlin · Jetpack Compose (Material 3) · ViewModel · Coroutines · Anthropic Java SDK · JUnit · Compose UI Test
