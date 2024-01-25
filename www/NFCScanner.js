// cordova-plugin-nfcscanner - NFCScanner.js
// Author: Juntao Cheng <stcheng_msft@hotmail.com>
// ----------------------------------------------
const pluginServiceName = 'NFCScanner';

const nfcEnabled = 'nfcEnabled';
const startReading = 'startReading';
const stopReading = 'stopReading';
const registerReaderModeCallback = 'registerReaderModeCallback';
const unregisterReaderModeCallback = 'unregisterReaderModeCallback';

var util = {
  // i must be <= 256
  toHex: function (i) {
      var hex;

      if (i < 0) {
          i += 256;
      }

      hex = i.toString(16);

      // zero padding
      if (hex.length === 1) {
          hex = "0" + hex;
      }

      return hex;
  },

  toPrintable: function(i) {

      if (i >= 0x20 & i <= 0x7F) {
          return String.fromCharCode(i);
      } else {
          return '.';
      }
  },

  bytesToString: function(bytes) {

      var result = "";
      var i, c, c1, c2, c3;
      i = c = c1 = c2 = c3 = 0;

      // Perform byte-order check.
      if( bytes.length >= 3 ) {
          if( (bytes[0] & 0xef) == 0xef && (bytes[1] & 0xbb) == 0xbb && (bytes[2] & 0xbf) == 0xbf ) {
              // stream has a BOM at the start, skip over
              i = 3;
          }
      }

      while ( i < bytes.length ) {
          c = bytes[i] & 0xff;

          if ( c < 128 ) {

              result += String.fromCharCode(c);
              i++;

          } else if ( (c > 191) && (c < 224) ) {

              if ( i + 1 >= bytes.length ) {
                  throw "Un-expected encoding error, UTF-8 stream truncated, or incorrect";
              }
              c2 = bytes[i + 1] & 0xff;
              result += String.fromCharCode( ((c & 31) << 6) | (c2 & 63) );
              i += 2;

          } else {

              if ( i + 2 >= bytes.length  || i + 1 >= bytes.length ) {
                  throw "Un-expected encoding error, UTF-8 stream truncated, or incorrect";
              }
              c2 = bytes[i + 1] & 0xff;
              c3 = bytes[i + 2] & 0xff;
              result += String.fromCharCode( ((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63) );
              i += 3;

          }
      }
      return result;
  },

  stringToBytes: function(string) {

      var bytes = [];

      for (var n = 0; n < string.length; n++) {

          var c = string.charCodeAt(n);

          if (c < 128) {

              bytes[bytes.length]= c;

          } else if((c > 127) && (c < 2048)) {

              bytes[bytes.length] = (c >> 6) | 192;
              bytes[bytes.length] = (c & 63) | 128;

          } else {

              bytes[bytes.length] = (c >> 12) | 224;
              bytes[bytes.length] = ((c >> 6) & 63) | 128;
              bytes[bytes.length] = (c & 63) | 128;

          }

      }

      return bytes;
  },

  bytesToHexString: function (bytes) {
      var dec, hexstring, bytesAsHexString = "";
      for (var i = 0; i < bytes.length; i++) {
          if (bytes[i] >= 0) {
              dec = bytes[i];
          } else {
              dec = 256 + bytes[i];
          }
          hexstring = dec.toString(16);
          // zero padding
          if (hexstring.length === 1) {
              hexstring = "0" + hexstring;
          }
          bytesAsHexString += hexstring;
      }
      return bytesAsHexString;
  },

  // This function can be removed if record.type is changed to a String
  /**
   * Returns true if the record's TNF and type matches the supplied TNF and type.
   *
   * @record NDEF record
   * @tnf 3-bit TNF (Type Name Format) - use one of the TNF_* constants
   * @type byte array or String
   */
  isType: function(record, tnf, type) {
      if (record.tnf === tnf) { // TNF is 3-bit
          var recordType;
          if (typeof(type) === 'string') {
              recordType = type;
          } else {
              recordType = nfc.bytesToString(type);
          }
          return (nfc.bytesToString(record.type) === recordType);
      }
      return false;
  },

  /**
   * Convert an ArrayBuffer to a hex string
   *
   * @param {ArrayBuffer} buffer
   * @returns {srting} - hex representation of bytes e.g. 000407AF 
   */
  arrayBufferToHexString: function(buffer) {
      function toHexString(byte) {
          return ('0' + (byte & 0xFF).toString(16)).slice(-2);
      }
      var typedArray = new Uint8Array(buffer);
      var array = Array.from(typedArray);  // need to convert to [] so our map result is not typed
      var parts = array.map(function(i) { return toHexString(i) });

      return parts.join('');
  },

  /**
   * Convert a hex string to an ArrayBuffer.
   *
   * @param {string} hexString - hex representation of bytes
   * @return {ArrayBuffer} - The bytes in an ArrayBuffer.
   */
  hexStringToArrayBuffer: function(hexString) {

      // remove any delimiters - space, dash, or colon
      hexString = hexString.replace(/[\s-:]/g, '');

      // remove the leading 0x
      hexString = hexString.replace(/^0x/, '');

      // ensure even number of characters
      if (hexString.length % 2 != 0) {
          console.log('WARNING: expecting an even number of characters in the hexString');
      }

      // check for some non-hex characters
      var bad = hexString.match(/[G-Z\s]/i);
      if (bad) {
          console.log('WARNING: found non-hex characters', bad);
      }

      // split the string into pairs of octets
      var pairs = hexString.match(/[\dA-F]{2}/gi);

      // convert the octets to integers
      var ints = pairs.map(function(s) { return parseInt(s, 16) });

      var array = new Uint8Array(ints);
      return array.buffer;
  }

};

var NfcScanner = {
  util: util,
  nfcEnabled: function (successCallback, errorCallback) {
    cordova.exec(
      successCallback,
      errorCallback,
      pluginServiceName,
      nfcEnabled,
      [{}]);
  },  
  startReading: function (successCallback, errorCallback) {
    cordova.exec(
      successCallback,
      errorCallback,
      pluginServiceName,
      startReading,
      [{}]);
  },
  stopReading: function (successCallback, errorCallback) {
    cordova.exec(
      successCallback,
      errorCallback,
      pluginServiceName,
      stopReading,
      [{}]);
  },
  registerReaderModeCallback: function (successCallback, errorCallback) {
    cordova.exec(
      successCallback,
      errorCallback,
      pluginServiceName,
      registerReaderModeCallback,
      [{}]);
  },
  unregisterReaderModeCallback: function (successCallback, errorCallback) {
    cordova.exec(
      successCallback,
      errorCallback,
      pluginServiceName,
      unregisterReaderModeCallback,
      [{}]);
  }
};

module.exports = NfcScanner;
