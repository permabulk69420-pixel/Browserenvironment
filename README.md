# Browser Environment

A real web browser on a big screen inside your own VR space, for Meta Quest 3.

It's a native Quest app built with Meta's Spatial SDK. The screen is an Android WebView inside a Spatial SDK panel, so it's a full browser (YouTube, Twitch, logins, fullscreen video), not a video player.

## Install

1. Go to **[Releases → Latest build](../../releases/tag/latest)** and download `BrowserEnvironment.apk`.
2. Install it on the Quest (developer mode on):
   - **SideQuest:** drag the APK onto the window, or
   - **adb:** `adb install -r BrowserEnvironment.apk`
3. On the headset: **App Library → filter "Unknown Sources" → Browser Environment**.

Every push to `main` builds a new APK automatically and replaces the one on the Latest release. All builds use the same signing key, so new ones install straight over the old one (your cookies/logins stay).

## Using it

| Control | What it does |
|---|---|
| Trigger | Click (point the laser at the screen) |
| Grip on the screen | Grab and move it |
| ‹ › ⟳ ⌂ | Back, forward, reload, home |
| Address bar | Type a URL or a search (Quest keyboard pops up) |
| − / + | Shrink / grow the screen |
| Curve / Flat | Bend the screen around you, or flatten it |
| Desktop / Mobile | Switch between desktop sites and mobile sites |
| Hold Meta button | Recenter |

The app reopens on the last page you were on.

## Known limits

- **Google sign-in** is often blocked inside embedded browsers (a Google policy), so logging in to YouTube may fail. Watching logged out works.
- DRM sites like Netflix probably won't play.

## Project layout

- `app/src/main/java/.../BrowserActivity.kt`: VR scene, environment, screen placement, curve/resize
- `app/src/main/java/.../BrowserPanel.kt`: toolbar and WebView
- `app/src/main/assets/home.html`: start page
- `.github/workflows/build.yml`: CI build and release
