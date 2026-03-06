# FantasyFudge

This is an app that can connect to various gadgets and devices, serving as an alternative to the vendor-provided app.

This app includes a Javascript module system. Like the Linux kernel module system, modules implement support for controlling various devices, and can be installed on the fly.
A module can connect to a device through WiFi or Bluetooth, and implement any proprietary protocols needed to communicate with and control it.

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
