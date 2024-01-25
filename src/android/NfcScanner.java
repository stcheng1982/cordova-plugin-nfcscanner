package com.juntaocheng.cordova.plugin.nfcscanner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.cordova.LOG;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Base64;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class NfcScanner extends CordovaPlugin implements NfcAdapter.ReaderCallback {
    static final String TAG = "NFCScanner";

    private static final String STATUS_NFC_OK = "NFC_OK";
    private static final String STATUS_NO_NFC = "NO_NFC";
    private static final String STATUS_NFC_DISABLED = "NFC_DISABLED";
    private static final String STATUS_NDEF_PUSH_DISABLED = "NDEF_PUSH_DISABLED";

    private static final String NFC_ENABLED = "nfcEnabled";
    private static final String START_READING = "startReading";
    private static final String STOP_READING = "stopReading";
    private static final String REGISTER_READERMODE_CALLBACK = "registerReaderModeCallback";
    private static final String UNREGISTER_READERMODE_CALLBACK = "unregisterReaderModeCallback";

    private AppCompatActivity cdvActivity;
    private Context cdvContext;
    private NfcAdapter nfcAdapter;

    private CallbackContext readerModeCallback;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        cdvActivity = cordova.getActivity();
        cdvContext = cordova.getContext();
        nfcAdapter = NfcAdapter.getDefaultAdapter(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "Execute Action: " + action);

        // Check NFC adapter status
        if (!getNfcStatus().equals(STATUS_NFC_OK)) {
            callbackContext.error(getNfcStatus());
            return true;
        }

        // Prepare parameters and invoke action methods based on action name
        JSONObject opts = args.getJSONObject(0);
        if (opts == null) {
            opts = new JSONObject();
        }

        if (action.equalsIgnoreCase(NFC_ENABLED)) {
            callbackContext.success(STATUS_NFC_OK);
            return true;
        } else if (action.equalsIgnoreCase(START_READING)) {
            return doStartReading(opts, callbackContext);
        } else if (action.equalsIgnoreCase(STOP_READING)) {
            return doStopReading(opts, callbackContext);
        } else if (action.equalsIgnoreCase(REGISTER_READERMODE_CALLBACK)) {
            return doRegisterReaderModeCallback(opts, callbackContext);
        } else if (action.equalsIgnoreCase(UNREGISTER_READERMODE_CALLBACK)) {
            return doUnregisterReaderModeCallback(opts, callbackContext);
        }

        return false;
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            JSONObject json;

            List<String> techList = Arrays.asList(tag.getTechList());
            if (techList.contains(Ndef.class.getName())) {
                Ndef ndef = Ndef.get(tag);
                json = Util.ndefToJSON(ndef);
            } else {
                json = Util.tagToJSON(tag);
            }

            PluginResult result = new PluginResult(PluginResult.Status.OK, json);
            result.setKeepCallback(true);
            if (readerModeCallback != null) {
                readerModeCallback.sendPluginResult(result);
            } else {
                Log.i(TAG, "readerModeCallback is null - reader mode probably disabled in the meantime");
            }
        } catch (Exception e) {
            LOG.e(TAG, e.getMessage());
        }
    }

    private boolean doStartReading(JSONObject opts, CallbackContext callbackContext) {
        cdvActivity.runOnUiThread(() -> {
            try {
                if (nfcAdapter != null) {
                    nfcAdapter.enableReaderMode(cordova.getActivity(), this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V, null);
                    callbackContext.success("Reader Mode started.");
                } else {
                    callbackContext.error("NFC not supported on this device");
                }
            } catch (Exception e) {
                LOG.e(TAG,e.getMessage());
                if (callbackContext != null) {
                    callbackContext.error(e.getMessage());
                }
            }

        });

        return true;
    }

    private boolean doStopReading(JSONObject opts, CallbackContext callbackContext) {
        cdvActivity.runOnUiThread(() -> {
            try {
                if (nfcAdapter != null) {
                    nfcAdapter.disableReaderMode(cordova.getActivity());
                    callbackContext.success("Reader Mode stopped.");
                } else {
                    callbackContext.error("NFC not supported on this device");
                }
            } catch (Exception e) {
                LOG.e(TAG,e.getMessage());
                if (callbackContext != null) {
                    callbackContext.error(e.getMessage());
                }
            }

        });

        return true;
    }

    private boolean doRegisterReaderModeCallback(JSONObject opts, CallbackContext callbackContext) {
        if (callbackContext != null) {
            readerModeCallback = callbackContext;
        } else {
            Log.i(TAG, "callbackContext is null - cannot register new readerModeCallback");
        }
        return true;
    }

    private boolean doUnregisterReaderModeCallback(JSONObject opts, CallbackContext callbackContext) {
        readerModeCallback = null;
        if (callbackContext != null) {
            callbackContext.success("readerModeCallback is unregistered.");
        }
        return true;
    }

    private String getNfcStatus() {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(cdvActivity);
        if (nfc == null) {
            return STATUS_NO_NFC;
        } else if (!nfc.isEnabled()) {
            return STATUS_NFC_DISABLED;
        } else {
            return STATUS_NFC_OK;
        }
    }

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
