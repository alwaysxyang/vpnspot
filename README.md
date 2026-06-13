# vpnspot

`vpnspot` is a no-root Android hotspot proxy for sharing a phone's network path with nearby devices. It runs a local HTTP/HTTPS proxy on the phone, exposes it to devices connected to the phone's hotspot, and records the destinations those devices connect to.

The main use case is simple: connect a phone to a VPN, turn on the phone hotspot, start `vpnspot`, and configure another device to use the phone as its Wi-Fi proxy. The other device can then send supported HTTP/HTTPS traffic through the phone's network path without rooting the phone.

## What It Does

- Runs an HTTP/HTTPS proxy directly on Android.
- Supports configurable proxy ports, such as `8080` or `12345`.
- Supports HTTPS tunneling through the `CONNECT` method.
- Shows the current proxy address in the app.
- Provides a bottom-tab interface:
  - `首页`: runtime controls and connection instructions.
  - `数据`: live connection log.
- Clears connection logs automatically whenever the proxy is started.
- Allows manual log clearing from the `数据` tab.
- Runs as an Android foreground service while active.

## What It Is Not

`vpnspot` is not a transparent VPN tunnel and does not capture all traffic automatically. It is intentionally built as a no-root manual proxy.

That means:

- The client device must manually configure a Wi-Fi proxy.
- Apps that ignore system proxy settings may bypass it.
- UDP traffic is not supported.
- Traffic interception or TLS decryption is not performed.

## Requirements

- Android device with hotspot support.
- Android 7.0 or later, based on the current `minSdk`.
- USB debugging enabled for development installation.
- Android SDK and `adb` available on the development machine.
- A working Gradle environment. The project includes `./gradlew`.

## Install On A Phone

Enable USB debugging on the Android phone:

1. Open Settings -> About phone.
2. Tap the build number several times to enable Developer options.
3. Open Settings -> Developer options.
4. Enable USB debugging.
5. Connect the phone to the computer with a USB data cable.
6. Accept the USB debugging authorization prompt on the phone.

Confirm that `adb` can see the phone:

```bash
adb devices
```

Expected output:

```text
List of devices attached
xxxxx device
```

Install the debug build:

```bash
./gradlew installDebug
```

Launch the app from the command line:

```bash
adb shell monkey -p com.xxd.vpnspot 1
```

You can also build the APK first and install it manually:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

If Android rejects the install because an older build has a different signature, uninstall the old package first:

```bash
adb uninstall com.xxd.vpnspot
./gradlew installDebug
```

## Usage

1. Open `vpnspot` on the phone.
2. On the `首页` tab, enter the proxy port.
3. Tap `Start`.
4. Keep the app running. The button changes to `Stop` while the proxy is active.
5. Enable the phone hotspot.
6. Connect another device to that hotspot.
7. On the other device, open the Wi-Fi proxy settings and choose manual proxy.
8. Set:
   - Proxy host: the IP address shown by `vpnspot`.
   - Proxy port: the port shown by `vpnspot`.
9. Browse from the other device.
10. Open the `数据` tab in `vpnspot` to watch the live connection log.

To route supported traffic through the phone's VPN, connect the VPN on the phone first, then start the proxy in `vpnspot`.

## Development Commands

Build the debug APK:

```bash
./gradlew assembleDebug
```

Install the debug APK on the connected phone:

```bash
./gradlew installDebug
```

Run local unit tests:

```bash
./gradlew testDebugUnitTest
```

Run Android instrumentation tests on a connected device or emulator:

```bash
./gradlew app:connectedDebugAndroidTest
```

Run everything commonly needed before handing off a change:

```bash
./gradlew testDebugUnitTest
./gradlew app:connectedDebugAndroidTest
./gradlew assembleDebug
```

## Debugging

Check connected devices:

```bash
adb devices
```

Watch app-related logs:

```bash
adb logcat | rg "vpnspot|ProxyService|AndroidRuntime"
```

If `rg` is not installed:

```bash
adb logcat
```

Restart `adb` if device discovery gets stuck:

```bash
adb kill-server
adb start-server
adb devices
```

Test the proxy from the development machine when forwarding a local emulator/device port:

```bash
curl -x "http://127.0.0.1:8080" "https://www.baidu.com" -v
```

For a physical phone hotspot setup, use the phone IP and configured proxy port from the app instead of `127.0.0.1`.

## Troubleshooting

### `No target device found`

Gradle cannot find an install target. Check:

- The phone is connected with a data-capable USB cable.
- USB debugging is enabled.
- The phone has accepted the USB debugging authorization prompt.
- `adb devices` shows the phone as `device`, not `unauthorized`.

### Install fails because the package already exists

This usually means the installed app was signed by a different debug key. Remove it and install again:

```bash
adb uninstall com.xxd.vpnspot
./gradlew installDebug
```

### The client device connects to the hotspot but cannot browse

Check:

- `vpnspot` shows `Running`.
- The client device uses the exact proxy host and port shown in the app.
- The phone hotspot works without the proxy.
- The phone itself has working network access.
- The phone VPN, if used, is already connected.
- The client app honors system Wi-Fi proxy settings.

### HTTPS sites connect but some apps still bypass the proxy

This is expected for apps that use custom network stacks, certificate pinning, QUIC/UDP, or their own proxy rules. `vpnspot` provides a manual HTTP/HTTPS proxy; it does not force every app on the client device through the proxy.

## Network Model

```text
Client device
    |
    | Wi-Fi hotspot + manual HTTP proxy
    v
Android phone running vpnspot
    |
    | Phone network path, optionally through phone VPN
    v
Internet
```

## Security Notes

- Do not expose the proxy on untrusted networks.
- Keep the phone hotspot password protected.
- Anyone connected to the hotspot and configured to use the proxy can send traffic through it.
- The connection log records destination hostnames and ports.
- The app does not decrypt HTTPS content.

## Limitations

- No root required.
- No transparent traffic capture.
- No UDP forwarding.
- No automatic client configuration.
- No TLS interception.
- Manual Wi-Fi proxy configuration is required on client devices.
