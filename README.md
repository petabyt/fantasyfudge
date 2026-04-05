# FantasyFudge

This is an app that can connect to various gadgets and devices, serving as an alternative to the vendor-provided app.

<img src='https://s1.danielc.dev/screenshots/Screenshot_20260402-224446.png' width='250'><img src='https://s1.danielc.dev/screenshots/Screenshot_20260128-140111.png' width='250'>

This app includes a Javascript module system. Like the Linux kernel module system, modules use low level Bluetooth or WiFi APIs to directly communicate with devices
through their proprietary protocols. Modules can be installed on the fly through a QR code without recompiling the app.

Modules can control the UI and handle switching between various 'screens', such as a dashboard, photo gallery, or liveview.

## Roadmap
- [ ] WiFi and Bluetooth bindings for Android+Linux
  - libpak: https://github.com/petabyt/pak
- [ ] Runtime/Module interop + thread model
- [ ] Develop initial set of modules
  - Use [libfuji](https://github.com/petabyt/libfuji) for connecting to Fujifilm cameras
  - Use/adapt [furble](https://github.com/gkoh/furble) for camera bluetooth
  - TODO: Reverse engineer more dashcams
  - TODO: Reverse engineer smart TVs
- [ ] Dashboard with interactive grid of cards
- [ ] File gallery
