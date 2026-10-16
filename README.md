<div align="center">

# 🎮 GameVault

**One home for your PC and PS5 games. One queue for whatever comes next.**

A beautiful, personal game collection for Android — built for people who play across platforms and want their backlog to feel exciting, not exhausting.

[![Android](https://img.shields.io/badge/Android-native-3DDC84?style=flat-square&logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Firebase](https://img.shields.io/badge/Firebase-sync-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![RAWG](https://img.shields.io/badge/RAWG-game%20metadata-E9484A?style=flat-square)](https://rawg.io/apidocs)
[![SteamGridDB](https://img.shields.io/badge/SteamGridDB-artwork-1877D2?style=flat-square)](https://www.steamgriddb.com/)

**[Get GameVault for Android](https://github.com/NefariousNiru/GameVault/releases)** · **[Connect on LinkedIn](https://www.linkedin.com/in/nbroy/)**

*The first signed Android APK will appear on GitHub Releases once final device testing and signing are complete.*

</div>

---

## Your collection, your way

**PC or PS5? Both.** Track all your games in one place without creating duplicate entries for the same title. Move a game between platforms while keeping its history, notes and rating.

**Always know what's next.** Add any game to your single Play Queue, drag to rearrange your priorities, and remove one or clear the queue with confirmation — without deleting games from your library.

**More than played or unplayed.** Keep release, interest and play statuses independent. Track downloads and installs on PC, or access and library status on PS5. Add a personal rating, notes, and completion date.

**Make it look like your library.** Search game data with RAWG, browse artwork from SteamGridDB, and pick your preferred **Album Art** (portrait) and **Cover Image** (landscape). You can also add games and artwork manually.

**Your games follow you.** Sign in with Google to sync your private library using Firebase. Recently accessed games and pre-cached artwork can remain available offline. Save a JSON backup, restore it later, or undo your most recent import.

## A closer look

| The Collection | The Play Queue |
| :---: | :---: |
| [📸 View screenshot](screenshots/library.png) | [📸 View screenshot](screenshots/queue.png) |

**Screenshots coming with the first release.** To add them yourself, save your actual Android screenshots as `screenshots/library.png` and `screenshots/queue.png`. These are intentional placeholders — no mock screenshots are presented as real app captures.

## Get started

1. **Install the direct `.apk` file** from [GitHub Releases](https://github.com/NefariousNiru/GameVault/releases) once the first release is published—**not** the GitHub source-code ZIP. Each release provides a separate `SHA256SUMS.txt` and shows the APK checksum in the release notes for verification. Android 8.0+ is supported.
2. **Sign in with Google.** Your collection is associated with your account.
3. **Highly recommended: add your free-tier RAWG and SteamGridDB API keys** under **Settings → Game Search & Artwork**. They unlock the best experience with title suggestions, metadata and artwork choices.
4. Add a game to PC or PS5, set its statuses, and start building your Play Queue.

### Get your API keys — free-tier options, quick setup

Both services offer a way to get an API key for personal use. Usually it only takes a few minutes to register and generate one; provider eligibility, quotas and terms may change.

| Service | What it unlocks in GameVault | Get your key |
|---|---|---|
| **RAWG** | Game search, titles, release information, developers, genres and more | **[Create a RAWG API key](https://rawg.io/apidocs)** · [authentication guide](https://rawg.readme.io/reference/authentication) |
| **SteamGridDB** | Choice of beautiful portrait album art and landscape cover images | **[Create a SteamGridDB API key](https://www.steamgriddb.com/profile/preferences/api)** (sign in, then Profile → Preferences → API) |

**For the best GameVault experience, get both keys.** RAWG makes adding games faster; SteamGridDB brings your collection to life. You can still add games manually without either key.

GameVault saves these API keys **on your device**, not in your shared game catalog or JSON exports. Keys do not automatically sync to another phone. Free access is subject to each provider's rules and request limits; no paid plan is required for the intended small personal collection.

## Your library stays yours

- Your personal games are scoped to your signed-in Firebase user account.
- JSON **Export Library** lets you save a portable copy of your titles, notes, statuses, and queue order.
- **Import Backup** validates the whole file before offering Keep Cloud, Merge, or Replace; your previous library can be recovered after an import.
- Previously viewed games and cached images can be available without internet. **Initial sign-in, new-game creation, queue changes and imports require an internet connection.** Artwork needs to have been cached beforehand to show offline.
- GameVault does not upload or store image files in Firebase Storage. Artwork remains hosted by its original providers.

## Credits

Game data is sourced from [RAWG](https://rawg.io/) and optional artwork from [SteamGridDB](https://www.steamgriddb.com/). All game names, artwork, logos and other third-party content belong to their respective owners and are subject to provider terms. GameVault is an independent, unofficial personal-library app and is not affiliated with Sony, PlayStation, Valve, RAWG or SteamGridDB.

Made by **[Nirupom Bose Roy](https://www.linkedin.com/in/nbroy/)** · [GitHub Releases](https://github.com/NefariousNiru/GameVault/releases)
