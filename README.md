# Forge

A minimal Android launcher with a twist: user-generated icon packs made with [Google AI Studio](https://aistudio.google.com/) (Gemini image generation).

Forge is a fork of [Slim Launcher](https://github.com/sduduzog/slim-launcher) by Beautus Sduduzo Gumede, used under the MIT License. Slim’s original copyright and permission notice are preserved in `LICENSE`. This project is a separate app (`com.pradhul.forge`) and does not reuse Slim’s name or logo.

## Why Forge

Slim’s idea is *all you need is less* — a short home list, no feed, no widgets. Forge keeps that restraint and adds one feature: restyle every installed app’s icon from a text prompt, on device, with your own API key.

## Features

- Minimal home screen with a short app list
- App icons next to labels on the home list and the full app list (toggle in Options)
- Icon packs stored on the device: name, style prompt, and per-package PNGs
- Create, list, apply, and delete packs; revert to stock icons at any time
- Missing pack icons fall back to the system icon
- Paste a Google AI Studio API key (stored locally with EncryptedSharedPreferences; never logged or committed)
- Generate a pack from a style prompt such as `neon cyberpunk`, with progress, cancel, rate-limit retries, and per-app regenerate

## How to use

1. Build and install the debug APK (`./gradlew assembleDebug`). Forge can sit next to Slim because the application id is different.
2. Set **Forge** as the Home app (long-press the Home button or open device home settings).
3. Open **Options → Icon packs**.
4. Paste a [Google AI Studio API key](https://aistudio.google.com/apikey) and save it.
5. Tap **Create pack**, enter a name and a style prompt, then start generation.
6. Wait while Gemini draws a square icon for each installed launcher app. You can cancel; a partial pack is kept.
7. Apply the pack (generation applies it when it finishes with at least one icon). Apps not in the pack keep their stock icons.
8. Open a pack to regenerate a single app, or choose **Use stock icons** to revert.

Generation calls Gemini’s image model (`gemini-2.5-flash-image`) at `https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`, sending the app name, your style prompt, and the original icon as a reference image when it can. Each image is billed to your AI Studio account.

## Building and tests

- JDK 17 or newer and Android SDK 36+
- Set the SDK path in `local.properties` or with `ANDROID_SDK_ROOT`
- `./gradlew assembleDebug testDebugUnitTest`
- Existing Slim tests are kept; new unit tests cover pack storage and apply/fallback logic

## License

MIT. See `LICENSE`.

Copyright (c) 2018 Beautus Sduduzo Gumede  
Copyright (c) 2026 Pradhul
