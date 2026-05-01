# FantasyFudge

This is an app that can connect to various gadgets and devices, serving as an alternative to the vendor-provided app.

<img src='https://s1.danielc.dev/screenshots/Screenshot_20260128-140111.png' width='250'><img src='https://s1.danielc.dev/screenshots/Screenshot_20260430-205138.png' width='250'><img src='https://s1.danielc.dev/screenshots/Screenshot_20260402-224446.png' width='250'><img src='https://s1.danielc.dev/screenshots/Screenshot_20260430-204144.png' width='250'>

## Goals
- Connect to and control a wide range of devices and gadgets - such as earbuds, mirrorless cameras, smart TVs, diagnostic systems
- Serve as a valid alternative to (often privacy-invasive, buggy, or outdated) vendor-provided apps
- Deep integration with the wireless features of each device, adjust UI depending on circumstance
- Separate the logic between the frontend and reverse-engineered protocol code (for legal reasons among others)

## Roadmap
- [ ] WiFi and Bluetooth bindings for Android+Linux
  - Based on libpak: https://github.com/petabyt/pak
- [x] Develop a runtime + reliable thread model
- [x] Develop initial [set of modules](https://github.com/petabyt/gadget-libs)
- [x] Dashboard with interactive grid of cards
- [x] FIFO Image gallery
- [ ] Liveview screen
- [ ] Intervalometer screen

## Modules:
Like the Linux kernel module system, modules use low level Bluetooth or WiFi APIs to directly communicate with devices
through their proprietary protocols. Modules can be installed on the fly through a QR code without recompiling the app.

- Loaded as WebAssembly executable (.wasm) or JS script (QuickJS)
- Connect to a single device through Bluetooth or WiFi
- Directly control and manipulate the UI and respond to user interaction
  - Respond to triggers such as remote shutter, or changing device settings
  - fill gallery with metadata/thumbnails
  - update real time data such as battery, storage, guages
  - Add/remove support for screens, features, and user settings

Modules can control the UI and handle switching between various 'screens', such as a dashboard, photo gallery, or liveview.
