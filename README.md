<p align="center">
  <img src="docs/images/banner.png" alt="NGStation: play N-Gage games on Android" width="100%">
</p>

<p align="center">
  <a href="https://github.com/JenabB/EKA2L1/releases/download/ngstation-latest/NGStation.apk"><b>Download the APK</b></a>
  &nbsp;·&nbsp;
  <a href="https://github.com/JenabB/EKA2L1/releases/tag/ngstation-latest">Release notes</a>
  &nbsp;·&nbsp;
  <a href="#getting-started">Getting started</a>
</p>

---

**NGStation** is an N-Gage emulator for Android that works like the console emulators you already know: set it up once, drop your games in a folder, tap one and play. It is built on the [EKA2L1](https://github.com/EKA2L1/EKA2L1) Symbian emulator core and replaces its general-purpose Symbian front end with a launcher made only for first-generation N-Gage games (S60v1).

Everything EKA2L1 can run, NGStation runs the same way: the emulation core is unchanged. What changes is everything around it.

## Screens

| Setup | Library | On-screen controls |
|:---:|:---:|:---:|
| <img src="docs/images/setup.png" width="260"> | <img src="docs/images/library.png" width="260"> | <img src="docs/images/controls.png" width="420"> |

<sub>Images are renders of the current design.</sub>

## Features

### Setup in three steps
- **One guided screen** instead of EKA2L1's device and package menus: file access, firmware, games folder. Finished steps fold into a one-line status.
- **Automatic firmware install.** Put an EKA2L1 firmware pack (`.zip` or `Data` folder) in `Games/N-Gage/Firmware`, open the app, done. Packs are also found in `Games/N-Gage` and `Download`.
- **Every firmware format EKA2L1 accepts**: firmware pack ZIP, `Data` folder (pick the folder, its parent or any folder inside it), `.ROM` + `.RPKG`, or `.VPL`. Packs are merged, so devices already installed stay.
- **`Games/N-Gage` is created for you** (with a `Firmware/` folder and a short README) and used as the games folder.
- **The N-Gage device is selected automatically** when several devices are installed.

### A real game library
- **Game cards** in the shape of the N-Gage's MMC cards: a colour per game, the game's own icon once installed (its initials before), and a play or install badge.
- **Folder or ZIP games.** Each game is one folder holding `System/Apps` (an MMC dump), or a `.zip` of it; dumps wrapped in an extra folder are found too.
- **One tap to play.** The first tap installs the game into the emulator, later taps start it straight away.
- **Continue playing** card for the last game, and recently played games sort first.
- **Clean titles**: dump tags are stripped ("Asphalt Urban GT (USA, Europe) (En,Fr,De)" shows as "Asphalt Urban GT"), with the region shown on its own.
- **Long-press a game** for its settings (controls, screen, upscaling shaders), to reinstall it, or to link it to an already installed app.

### Feels like an N-Gage QD
- **QD-style on-screen controls**, held sideways: a cross pad with orange dots in a recessed disc, the keypad fanned along an arc, soft keys and call/end keys where the QD has them.
- **QD colours**: orange legends, a black centre column (2/5/8/0), silver digits, charcoal function keys, orange when pressed; 5 and 7, the N-Gage's main game buttons, stand out.
- **Landscape by default**, game screen centred in a grey body with a black bezel, controls kept clear of rounded screen corners on 16:9 to 21:9 phones.
- **Loading screen** while the virtual handheld boots, gone as soon as the game draws its first frame.
- **Charcoal and orange theme** across the whole app, including the stock EKA2L1 screens and dialogs.

### Under the hood
- **Fixes over the stock Android front end**: on-screen keys are drawn opaque next to the game (they used to count the whole screen as the game and stay translucent), and setup no longer crashes by querying the core before it has started.
- **English everywhere**, whatever the phone's language.
- **Smaller download** (about 23 MB, down from about 30 MB): unused barcode scanning and camera modules removed, debugging guides no longer bundled.
- **Installs next to the official EKA2L1** (separate app id), and **builds itself**: every push produces a signed APK on the [`ngstation-latest`](https://github.com/JenabB/EKA2L1/releases/tag/ngstation-latest) release.

## Getting started

1. **Install** [`NGStation.apk`](https://github.com/JenabB/EKA2L1/releases/download/ngstation-latest/NGStation.apk) (allow installing from unknown sources if asked).
2. **Open it and allow file access.** The app creates `Games/N-Gage` and `Games/N-Gage/Firmware`.
3. **Add firmware:** copy your firmware pack ZIP into `Games/N-Gage/Firmware` and reopen the app.
4. **Add games:** copy each game into `Games/N-Gage`, as a folder or a `.zip`.
5. **Tap a game.** The first tap installs it (the app restarts once); after that it starts straight away.

```
Games/N-Gage/
├── Firmware/
│   └── firmware-pack.zip
├── Asphalt Urban GT/
│   └── System/Apps/Asphalt/...
├── Pathway to Glory/
│   └── System/Apps/...
└── Rayman 3.zip
```

### What you need
- An Android phone (Android 5.0 or newer; a 64-bit phone is recommended).
- **Your own firmware and games.** Dump them from an N-Gage or N-Gage QD you own. NGStation does not include or download any firmware or games.

## FAQ

**Why do I need firmware?**
N-Gage games are Symbian apps that call into the phone's operating system. EKA2L1 runs that real operating system, so it needs a dump of it, just as other emulators need a BIOS.

**Are save states supported?**
Not yet. The EKA2L1 core cannot save its state; in-game saves work as on the real handheld.

**Can I make the picture sharper?**
Yes: long-press a game, open its settings and turn on an upscaling shader (xBR, 4xHQ, LCD, CRT and more). These are filters; N-Gage games draw at 176×208, so there is no higher resolution to render.

**I changed a game's controls before and don't see the new layout.**
Saved layouts are kept. Open the on-screen layout editor in the game and pick the "N-Gage" layout.

**Will my games survive an update?**
Yes. Installing a new APK over the old one keeps firmware, games and saves. Uninstalling removes installed games and saves; firmware in `Games/N-Gage/Firmware` installs again automatically.

## Building

The APK is built by GitHub Actions ([`.github/workflows/ngstation.yml`](.github/workflows/ngstation.yml)) on every push to `ngstation`, `master` or `main`, and published with the text of [`NGSTATION_RELEASE_NOTES.md`](NGSTATION_RELEASE_NOTES.md).

To build locally, follow EKA2L1's [BUILDING.md](BUILDING.md) for the Android project in `src/emu/android`, then run `./gradlew assembleRelease`.

- Launcher code: `src/emu/android/app/src/main/java/com/github/eka2l1/ngage/`
- QD on-screen controls: `src/emu/android/app/src/main/java/com/github/eka2l1/emu/overlay/VirtualKeyboard.java`
- The repository's signing key (`src/emu/android/app/ngstation.jks`) is public on purpose, for personal builds that install over each other. Use your own private key for any store release.

## Credits and license

NGStation exists thanks to **[EKA2L1](https://github.com/EKA2L1/EKA2L1)** and its developers, who wrote the Symbian/N-Gage emulation this app runs on. Please consider [supporting them](https://eka2l1.github.io/quickstart/donation/). Issues with game compatibility belong to the core; check their [compatibility list](https://github.com/EKA2L1/Compatibility-List).

NGStation is licensed under the **GNU General Public License v3.0**, like EKA2L1. See [LICENSE](LICENSE).

N-Gage and Nokia are trademarks of Nokia Corporation. NGStation is an independent project, not affiliated with or endorsed by Nokia.
