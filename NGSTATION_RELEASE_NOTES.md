## NGStation

A PPSSPP-style N-Gage launcher for Android: set it up once, then pick a game and play. Built on the [EKA2L1](https://github.com/EKA2L1/EKA2L1) core; the emulation itself is unchanged.

**Download:** `NGStation.apk` below. Install it over the previous version; installed firmware and games are kept.

- Target: Android, first-generation N-Gage games (S60v1), as folders or `.zip`
- Firmware: bring your own dump (N-Gage / N-Gage QD); none is bundled. Put the firmware pack ZIP in `Games/N-Gage/Firmware` and it installs automatically
- Installs side by side with the official EKA2L1 (app id `com.github.eka2l1.ngstation`)

## Release history

Newest first.

### Build 8 — English UI (5 November 2026)
- **Changed:** the whole app is now in English, whatever language the phone uses (launcher screens, stock EKA2L1 screens and system buttons)

### Build 7 — N-Gage QD look (5 November 2026)
- **New:** setup redesigned in the QD palette: charcoal body, orange primary and silver secondary buttons, step badges like the QD's black centre keys, the handheld artwork on top and a 3-step progress bar
- **New:** finished steps fold into a one-line status, with "Edit" to reopen them
- **Fixed:** the stock EKA2L1 theme (purple bar, white dialogs) is replaced by a dark theme with orange accents across the app
- **Fixed:** the stock EKA2L1 storage warning dialog no longer appears; setup explains where data lives instead
- Library and loading screen use the same charcoal and orange palette

### Build 6 — App icon (4 November 2026)
- **New:** new app icon (a simplified handheld) that fits every launcher icon shape, plus a 512×512 Play Store icon

### Build 5 — N-Gage QD layout (3 November 2026)
- **New:** QD-style on-screen controls: a cross pad with orange dots in a recessed disc (pressed parts glow orange), the keypad fanned along an arc on a charcoal plate, soft-left and call under the pad, soft-right above the keypad, end key bottom right
- **New:** QD key colours: orange legends, black centre column (2/5/8/0), silver for the other digits, charcoal function keys, orange when pressed
- **New:** grey body around the game screen, with a black bezel
- **Fixed:** keys were always drawn translucent because the keyboard treated the whole screen as the game screen; the game area is now worked out with the same maths as the core

### Build 4 — Automatic firmware (3 November 2026)
- **Fixed:** the app closed itself after installing firmware; it now only copies the files and restarts cleanly
- **New:** automatic firmware install: a firmware pack ZIP or folder in `Games/N-Gage/Firmware`, `Games/N-Gage` or `Download` is picked up without asking
- **New:** `Games/N-Gage/Firmware` is created with the games folder; firmware ZIPs are not listed as games
- **Fixed:** `roms/<code>` is copied in lower case, as the core expects

### Build 3 — Theme, loading screen, landscape (3 November 2026)
- **New:** themed setup and library screens
- **New:** `Games/N-Gage` (with a README) is created once file access is granted and used as the default games folder
- **New:** loading screen while a game starts; it goes away when the game draws its first frame, or on tap
- **New:** per-game defaults: landscape, game screen centred, an on-screen layout that keeps clear of rounded screen corners

### Build 2 — Firmware packs (3 November 2026)
- **Fixed:** the app closed right away on first launch; setup now waits for the emulator core
- **New:** EKA2L1 firmware packs (a `Data` folder with `devices.yml`, `drives`, `roms`) install from a folder or a `.zip`, merged without removing existing devices

### Build 1 — First release (3 November 2026)
- **New:** 3-step setup: file access, firmware from one folder (`.ROM`/`.RPKG`/`.VPL` detected automatically), games folder
- **New:** PPSSPP-style library: every game folder (`System/Apps`) or `.zip` is a tile; the first tap installs the game, later taps start it
- **New:** the N-Gage device is selected automatically; long-press a game for settings, reinstall, or linking it to an installed app
- **New:** APK built automatically with GitHub Actions, published on the `ngstation-latest` release

## Known limitations
- **Save states:** not available yet; the EKA2L1 core does not support them
- **Upscaling:** available as per-game shaders (xBR, 4xHQ, LCD, CRT and more) in Game settings; these are filters, not higher-resolution rendering
- **Saved layouts:** games whose layout or settings were saved before keep them; pick the "N-Gage" layout in the on-screen layout editor
