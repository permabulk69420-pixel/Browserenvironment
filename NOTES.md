# Dev notes

- Meta Spatial SDK 0.14, AGP 8.11, Kotlin 2.1, Gradle 8.14.3. Builds locally and in Actions.
- CI: every push to main -> `latest` release, asset BrowserEnvironment.apk. Fixed signing key (ci-debug.keystore) so updates install over.
- BrowserActivity.kt: scene, environment, panel placement, curve (PanelQuadCylinderAnimation), resize (Scale).
- BrowserPanel.kt: toolbar + WebView, fullscreen via onShowCustomView.
- Viewer at origin facing +Z; screen at z=2.4, y=1.45, rotated 180.
- Unverified on headset: facing direction, curve placement, keyboard, thumbstick scroll, YouTube fullscreen.
- Google sign-in likely blocked in WebView.
