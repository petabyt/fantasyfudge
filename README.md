# FantasyFudge

This is an app that can connect to various gadgets and devices, serving as an alternative to the vendor-provided app.

This app includes a Javascript module system, which allows loading third-party 'modules'. A module
can connect to a device through WiFi or Bluetooth, and implement its proprietary protocol needed to communicate with and control it.

A module can control the UI through the runtime API and handle switching between various 'screens', such as a dashboard, photo gallery, or liveview.
