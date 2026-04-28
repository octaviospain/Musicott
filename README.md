# Musicott

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![JDK 24+](https://img.shields.io/badge/JDK-24%2B-orange.svg)](https://openjdk.org/projects/jdk/24/)
[![CI](https://github.com/octaviospain/Musicott/actions/workflows/master.yml/badge.svg)](https://github.com/octaviospain/Musicott/actions/workflows/master.yml)
[![Latest release](https://img.shields.io/github/v/release/octaviospain/Musicott)](https://github.com/octaviospain/Musicott/releases/latest)

A cross-platform desktop music player built with JavaFX and Spring Boot.
Manage your local music library, organize playlists, play tracks with waveform visualization, and import from iTunes — all without leaving a native desktop app.

![Musicott main window](docs/images/hero-screenshot.png)

## Download

**[Latest release →](https://github.com/octaviospain/Musicott/releases/latest)**

| Platform | Installer |
|----------|-----------|
| Linux    | `Musicott-X.Y.Z-x86_64.AppImage` (or `yay -S musicott` from the AUR) |
| Windows  | `Musicott-X.Y.Z.exe` |
| macOS    | `Musicott-X.Y.Z.dmg` |

> **Note:** Installers are unsigned. See the [Install guide](https://github.com/octaviospain/Musicott/wiki/Install) for the one-time security workaround on macOS and Windows.

## User Guide

Full documentation lives in the **[GitHub wiki](https://github.com/octaviospain/Musicott/wiki)**:

- [Importing music](https://github.com/octaviospain/Musicott/wiki/Import) (including iTunes XML)
- [Playback](https://github.com/octaviospain/Musicott/wiki/Playback)
- [Playlists](https://github.com/octaviospain/Musicott/wiki/Playlists)
- [Search](https://github.com/octaviospain/Musicott/wiki/Search)

## Build from source

Requires JDK 24+ and Gradle 9.4+ (or use the wrapper).

```bash
git clone https://github.com/octaviospain/Musicott.git
cd Musicott
gradle run                  # launch the app
gradle jpackage             # build a native installer for the current OS
```

The native installer lands under `build/jpackage/` — drop the resulting `.AppImage`, `.exe`, or `.dmg` into `Applications`/`Program Files`/anywhere on `$PATH`.

## License

[GPL v3](https://www.gnu.org/licenses/gpl-3.0) — © Octavio Calleya
