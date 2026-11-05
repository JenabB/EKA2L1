# NGStation

A PPSSPP-style N-Gage launcher for Android, built on the [EKA2L1](https://github.com/EKA2L1/EKA2L1) core.
Set it up once, then pick a game and play.

## What you need

1. **N-Gage firmware**: a dump from your own N-Gage / N-Gage QD, either an EKA2L1 firmware pack
   (a `Data` folder or its `.zip`) or `.ROM` + `.RPKG` files (or a `.VPL`). Firmware is not bundled.
2. **Games**: one folder per game containing `System/Apps/...` (an MMC card dump), or a `.zip` of that folder.

Example layout:

```
Games/N-Gage/
├── Firmware/
│   └── firmware-pack.zip
├── Asphalt Urban GT/
│   └── System/Apps/Asphalt/...
├── Pocket Kingdom/
│   └── System/Apps/...
└── Rayman 3.zip
```

## Getting started

1. Install `NGStation.apk` from the Releases page (tag `ngstation-latest`).
2. On first launch, allow file access. The app creates `Games/N-Gage` and `Games/N-Gage/Firmware`.
3. Put the firmware pack in `Games/N-Gage/Firmware` and reopen the app: it installs automatically.
   You can also choose the firmware yourself in setup.
4. Copy games into `Games/N-Gage`. In the library, tap a game: the first tap installs it (the app
   restarts once), later taps start it straight away.
5. Long-press a game for its settings (controls, screen, upscaling shaders), to reinstall it, or to link it
   to an installed app.

The menu in the top-right has Refresh, Change games folder, Firmware / devices, Emulator settings,
Run setup again and **Full EKA2L1 mode** (the stock EKA2L1 screens, for advanced options).

## Build

The APK is built by GitHub Actions (`.github/workflows/ngstation.yml`) on every push to `ngstation`,
`master` or `main`. It is uploaded as an artifact and to the `ngstation-latest` release, whose text comes
from `NGSTATION_RELEASE_NOTES.md`.

Launcher code lives in `src/emu/android/app/src/main/java/com/github/eka2l1/ngage/`.
The APK is signed with `src/emu/android/app/ngstation.jks`, kept in the repository on purpose for personal
builds so each new build installs over the last one. Use a private key for any store release.
