# NFC Scanner Plugin

A Cordova plugin for reading NFC tags on Android devices.

## Installation

To install the plugin, run the following command in your Cordova project:

```
cordova plugin add https://github.com/stcheng1982/cordova-plugin-nfcscanner.git
```

or for local installation
```
cordova plugin add ../../cordova-plugin-nfcscanner
```

## Usage

To use the plugin, first make sure that the device supports NFC and that NFC is enabled. Then, call the `registerReaderModeCallback` function in your JavaScript code to register the callback function that will be invoked when NFC tag is detected after start scanning (by invoking `startReading`):


```javascript
window.plugins.NFCScanner.nfcEnabled(function(ret) {
  console.info('NFC enabled', ret);
}, function(error) {
  console.info('NFC NOT enabled' + error);
});

window.plugins.NFCScanner.registerReaderModeCallback(
  (ntag) => {
    console.info('NFC tag discovered: ', ntag);
  },
  (err) => {
    console.error(err);
  }
);

window.plugins.NFCScanner.startReading(function() {
  console.info('NFC scanner started.');
}, function(error) {
  console.info('Failed to start NFC scanner.' + error);
});

```

## Platform Support

This plugin is currently only supported on Android devices running Android 8.0 or later.

## License

This plugin is available under the MIT license. See the LICENSE file for more information.

## Acknowledgments

This plugin was inspired by the [cordova-plugin-nfc](https://github.com/chariotsolutions/cordova-plugin-nfc) plugin.
