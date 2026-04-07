# Tox Location Share (for Android)

<img src="https://raw.githubusercontent.com/zoff99/ToLoShare/refs/heads/master/fastlane/metadata/android/en-US/icon.png" width="120">

This is a simplified version of [TRIfA](https://github.com/zoff99/ToxAndroidRefImpl) (the Tox Message Application for Android) that focuses on stable location sharing.<br>

ToLoShare is a specialized Android application for secure, peer-to-peer location sharing using the Tox protocol.<br>
It enables real-time GPS coordinate broadcasting between friends through a decentralized network,<br>
displaying locations on an integrated OpenStreetMap view without relying on central servers.<br>
<br>
It includes live map visualization, robust background operation,<br>
multi-layer security including PIN protection, screenshot prevention and Tor proxy support.<br>

This is a work in progress, feel free to help out.

<a href="https://f-droid.org/app/com.zoffcc.applications.toloshare"><img src="https://raw.githubusercontent.com/zoff99/ToLoShare/master/images/f-droid.png" width="200"></a>

Status
=
[![Android CI](https://github.com/zoff99/ToLoShare/actions/workflows/app_startup.yml/badge.svg?branch=master)](https://github.com/zoff99/ToLoShare/actions/workflows/app_startup.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
[![Liberapay](https://img.shields.io/liberapay/goal/zoff.svg?logo=liberapay)](https://liberapay.com/zoff/donate)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zoff99/ToLoShare)

&nbsp;&nbsp;&nbsp;&nbsp;Looking for ToLoShare Desktop version? [follow me](https://github.com/zoff99/toloshare_material/)

Latest Automated Screenshots
=

<img src="https://github.com/zoff99/ToLoShare/releases/download/nightly/screen_shot_android_29_11.png" width="150">
<br>

<img src="https://github.com/zoff99/ToLoShare/releases/download/nightly/android_screen01_21.png" width="120">&nbsp;<img src="https://github.com/zoff99/ToLoShare/releases/download/nightly/android_screen01_29.png" width="120">&nbsp;<img src="https://github.com/zoff99/ToLoShare/releases/download/nightly/android_screen01_33.png" width="120">&nbsp;<img src="https://github.com/zoff99/ToLoShare/releases/download/nightly/android_screen01_35.png" width="120">



ToLoShare - Main Screen Usage Manual
=

## 🔘 The Three Toggle Switches (Top Bar)

The three toggle switches are located in the **toolbar at the top** of the screen.

### 1. Map / Friend-List View Switch

**Toggles between the friend list and the live map.**

| Position | Effect |
|----------|--------|
| **ON** | Shows the **friend list** (main view). The map is paused to save resources. |
| **OFF** | Shows the **interactive map** with live GPS overlays for you and your friends. |

When switched to map mode, the screen-keep-on flag is set (if configured), location overlays are re-added, and all friend positions are refreshed on the map.

### 2. Own GPS Smoothing Switch

**Toggles position smoothing for _your own_ GPS track.**

| Position | Effect |
|----------|--------|
| **ON** | Your location marker moves smoothly between GPS fixes by interpolating intermediate steps. |
| **OFF** | Your location marker jumps directly to each raw GPS fix. |

This preference is saved and restored across sessions.

### 3. Friend GPS Smoothing Switch

**Toggles position smoothing for _your friends'_ GPS tracks.**

| Position | Effect |
|----------|--------|
| **ON** | Friends' location markers move smoothly on the map between location updates. |
| **OFF** | Friends' location markers jump directly to each newly received position. |

## 🗺️ Map Control Buttons (Main Screen)

These buttons appear in the **upper panel of the map view** and control which position the map follows. The currently active button is highlighted in **dark red** at full opacity; inactive buttons are dimmed.

### ➤ Follow My Own Position

Tapping this button **centres the map on your own current location** and keeps it locked there as you move.

### ➤ Follow Friend #1

Tapping this button **centres the map on the first friend's position** (sorted by public-key index) and keeps it locked as that friend moves.

A companion **route button** next to it calculates and draws a route from your position to Friend #1 using the currently selected travel mode (car or walking).

### ➤ Follow Friend #2

Tapping this button **centres the map on the second friend's position** and keeps it locked as that friend moves.

A companion **route button** calculates and draws a route to Friend #2.

### 🚗 Travel Mode Button

Taps toggle the **routing mode** used for the route buttons:

| Icon | Mode |
|------|------|
| 🚗 Car icon | Route calculated for driving |
| 🚶 Walking icon | Route calculated for walking |

### ✖ Unfollow / Stop

Tapping this button **unpins the map** from any position and **clears all drawn routes**. The map can then be scrolled and zoomed freely.

## 🧭 Compass Icon - Toggling North-Up Mode

A **compass widget** is rendered in the **top-left corner of the map**.

- **Tap the compass** to toggle between two orientations:

| State | Compass Appearance | Map Behaviour |
|-------|--------------------|---------------|
| **North-up ON** | Shows the letter **"N"** | Map is locked with North pointing up |
| **North-up OFF** | Shows a **red/black needle** that rotates with the map | Map can rotate |

Tapping anywhere within the compass circle area (approximately the top-left corner of the map) triggers the toggle.


🚀 Featured Applications
=

Join a growing community of security-conscious people. Check out these featured applications:

*   **[TRIfA](https://github.com/zoff99/ToxAndroidRefImpl)**: The Tox flagship secure messenger for Android.
*   **[TRIfA for Desktop](https://github.com/Zoxcore/trifa_material)**: The feature rich Tox Desktop Messaging Client.
*   **[Tox Push Msgs](https://github.com/zoff99/tox_push_msg_app)**: The Companion App for TRIfA and TRIfA for Desktop to enable Push Messages.
*   **[ToxProxy](https://github.com/zoff99/ToxProxy)**: Offline message relay functionality for TRIfA and TRIfA for Desktop.
*   **[ToLoShare](https://github.com/zoff99/ToLoShare)**: A specialized Android application for secure, peer-to-peer real-time location sharing.
*   **[ToLoShare for Desktop](https://github.com/zoff99/ToLoShare_material)**: A cross-platform desktop application for secure peer-to-peer real-time location sharing.
*   **[ToFShare](https://github.com/zoff99/ToFShare)**: Secure decentralized file sharing for Android using the Tox protocol.
*   **[tox_videoplayer](https://github.com/zoff99/tox_videoplayer)**: A command-line application that streams video and audio content over the Tox network.

<br>
Any use of this project's code by GitHub Copilot, past or present, is done
without our permission.  We do not consent to GitHub's use of this project's
code in Copilot.
<br>
No part of this work may be used or reproduced in any manner for the purpose of training artificial intelligence technologies or systems.
