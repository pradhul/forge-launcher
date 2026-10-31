# Forge

A minimal Android launcher with a twist: user-generated icon packs, **on-device by default**, with optional [Google AI Studio](https://aistudio.google.com/) (Gemini) generation if you have a paid key.

Forge is a fork of [Slim Launcher](https://github.com/sduduzog/slim-launcher) by Beautus Sduduzo Gumede, used under the MIT License. Slim’s original copyright and permission notice are preserved in `LICENSE`. This project is a separate app (`com.pradhul.forge`) and does not reuse Slim’s name or logo.

## Why Forge

Slim’s idea is *all you need is less* — a short home list, no feed, no widgets. Forge keeps that restraint and adds one feature: restyle every installed app’s icon with a bundled neural style-transfer model, or optionally with Gemini.

## Features

- Minimal home screen with a short app list
- App icons next to labels on the home list and the full app list (toggle in Options)
- Icon packs stored on the device: name, style prompt, and per-package PNGs
- Create, list, apply, and delete packs; revert to stock icons at any time
- Missing pack icons fall back to the system icon
- Paste a Google AI Studio API key (stored locally with EncryptedSharedPreferences; never logged or committed)
- **On-device styles (default):** Magenta arbitrary-image-stylization TFLite (~3 MB, Apache-2.0) plus six bundled presets (neon, watercolor, pastel, retro pixel, ink, gold) or a photo from your gallery. Style-strength slider, cohesive circle/squircle backplates, and a light post-pass (palette, neon glow, posterize).
- **Gemini (optional):** paste a paid Google AI Studio API key. Free-tier keys have no image quota and are rejected with a clear message. Paid keys get paced requests, `retryDelay` honoring, progress, cancel, and per-app regenerate.

## How to use

1. Build and install the debug APK (`./gradlew assembleDebug`). Forge can sit next to Slim because the application id is different.
2. Set **Forge** as the Home app (long-press the Home button or open device home settings).
3. Open **Options → Icon packs**.
4. Tap **Create pack**. Mode defaults to **On-device styles** — pick a preset (or your own photo) and a strength, then Start. No API key required.
5. Wait while Forge stylises each installed launcher app on the phone (typically well under a minute for ~60 apps). You can cancel; a partial pack is kept.
6. Apply the pack (generation applies it when it finishes with at least one icon). Apps not in the pack keep their stock icons.
7. Optional: paste a [Google AI Studio API key](https://aistudio.google.com/apikey) and switch Mode to **Gemini** if you have billing enabled.
8. Open a pack to regenerate a single app, or choose **Use stock icons** to revert.

On-device generation uses the Magenta `arbitrary-image-stylization-v1-256` int8 TFLite models (Apache-2.0; see `THIRD_PARTY_NOTICES.md`). Gemini mode calls `gemini-2.5-flash-image` and is billed to your AI Studio account.

## Building and tests

- JDK 17 or newer and Android SDK 36+
- Set the SDK path in `local.properties` or with `ANDROID_SDK_ROOT`
- `./gradlew assembleDebug testDebugUnitTest`
- Existing Slim tests are kept; new unit tests cover pack storage, apply/fallback, post-process, and Gemini error handling

## License

MIT. See `LICENSE`.

Copyright (c) 2018 Beautus Sduduzo Gumede  
Copyright (c) 2026 Pradhul
