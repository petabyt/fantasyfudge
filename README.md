# FantasyFudge (temp name)

This is an Android app that can connect to mirrorless cameras and various other devices, serving as an alternative to the vendor-provided app.

| <img src='https://s1.danielc.dev/screenshots/Screenshot_20260128-140111.png' width='350'> | <img src='https://s1.danielc.dev/screenshots/Screenshot_20260430-204144.png' width='350'> | <img src='https://s1.danielc.dev/screenshots/Screenshot_20260430-205138.png' width='350'> | <img src='https://s1.danielc.dev/screenshots/Screenshot_20260804-005001.png' width='350'> |
|-|-|-|-|

[Obtainium nightly build](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22dev.danielc.fantasyfudge.nightly%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fpetabyt%2Ffantasyfudge%22%2C%22author%22%3A%22petabyt%22%2C%22name%22%3A%22FantasyFudge%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Afalse%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22Nightly%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22none%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Atrue%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Afalse%2C%5C%22releaseDateAsVersion%5C%22%3Atrue%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22includeTarballs%5C%22%3Afalse%2C%5C%22tarballedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D%0A)

## Goals
- Connect to and control a wide range of devices and gadgets - such as earbuds, mirrorless cameras, dashcams, smart TVs, diagnostic systems
- Deep integration with the wireless features of each device, adjusting UI depending on the circumstance
- Serve as a functional alternative to (often privacy-invasive, buggy, or outdated) vendor-provided apps
- Separate the logic between the frontend and reverse-engineered protocol code (similar to [Grayjay](https://grayjay.app/))

## Features
- Material 3 UI with Jetpack Compose
- WiFi and Bluetooth scripting APIs for Android+Linux
- Diverse set of scriptable screens and UI
- Supports many devices out of the box
- Uses new companion APIs to connect - location permission is not required

## Roadmap
- [x] Fujifilm support (libfuji)
- [ ] Canon/Nikon/Sony support (libgphoto2)
- [ ] Dashcam support (top 5 brands)
- [x] QuickJS runtime
- [ ] WebAssembly runtime
- [ ] Sandbox with permissions

## Modules
Reverse-engineered protocol logic is separated from the frontend through a runtime and module system.

Like the Linux kernel module system, modules use low level Bluetooth or WiFi APIs to directly communicate with devices
through their proprietary protocols. Modules can be installed on the fly without recompiling the app.

- Loaded as WebAssembly executable (.wasm) or JS script (QuickJS)
- Connect to a single device through Bluetooth or WiFi
- Directly control and manipulate the UI and respond to user interaction
  - Respond to triggers such as remote shutter, or changing device settings
  - Fill the gallery with metadata/thumbnails
  - Update real time data such as battery, storage, guages
  - Add/remove support for screens, features, and user settings
  - Handle screen navigation and block until device is ready
