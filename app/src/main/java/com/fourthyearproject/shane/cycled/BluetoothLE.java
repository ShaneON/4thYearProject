package com.fourthyearproject.shane.cycled;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.ParcelUuid;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

/**
 * Created by hp on 28/01/2017.
 */

class BluetoothLE {

    // UUIDs for UAT service and associated characteristics.
    private static UUID UART_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static UUID TX_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    private static UUID RX_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    // UUID for the BTLE client characteristic which is necessary for notifications.
    private static UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // BTLE state
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    private MainActivity mainActivity;
    private Handler handler;
    private boolean scanning;
    private final int SCAN_TIME = 5000;

    public BluetoothLE(MainActivity mActivity)
    {
        adapter = BluetoothAdapter.getDefaultAdapter();
        mainActivity = mActivity;
        handler = new Handler();
    }

    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mainActivity.writeLine("Connected.");
                // Discover services.
                if (!gatt.discoverServices()) {
                    mainActivity.writeLine("Failed to connect.");
                }
            }
            else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                mainActivity.writeLine("Disconnected!");
            }
            else {
                mainActivity.writeLine("Connection state changed.  New state: " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mainActivity.writeLine("Service discovery completed!");
            }
            else {
                mainActivity.writeLine("Service discovery failed with status: " + status);
            }
            // Save reference to each characteristic.
            tx = gatt.getService(UART_UUID).getCharacteristic(TX_UUID);
            rx = gatt.getService(UART_UUID).getCharacteristic(RX_UUID);
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true)) {
                mainActivity.writeLine("Couldn't set notifications for RX characteristic!");
            }
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(CLIENT_UUID) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc)) {
                    mainActivity.writeLine("Couldn't write RX client descriptor value!");
                }
            }
            else {
                mainActivity.writeLine("Couldn't get RX client descriptor!");
            }
        }
        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            mainActivity.writeLine("Received: " + characteristic.getStringValue(0));
        }
    };

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
            BluetoothDevice device = result.getDevice();
            mainActivity.writeLine("Found device: " + device.getAddress());

            if(null != result.getScanRecord().getServiceUuids()) {
                List<ParcelUuid> parcelUuidList = result.getScanRecord().getServiceUuids();

                for (ParcelUuid u : parcelUuidList) {
                    if (UART_UUID.toString().equals(u.getUuid().toString())) {
                        bluetoothLeScanner.stopScan(leScanCallback);
                        mainActivity.writeLine("Found UART service!");
                        // Connect to the device.
                        // Control flow will now go to the callback functions when BTLE events occur.
                        gatt = result.getDevice().connectGatt(mainActivity.getApplicationContext(), false, callback);
                    }
                }
            }

            else mainActivity.writeLine("No UUIDs found for device.");

        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    void scanForDevices() {
        final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }, SCAN_TIME);

        scanning = true;
        mainActivity.writeLine("Scanning for devices...");
        bluetoothLeScanner.startScan(leScanCallback);
    }

    void stopConnection() {
        if (gatt != null) {
            // disconnect and close the connection.
            gatt.disconnect();
            gatt.close();
            gatt = null;
            tx = null;
            rx = null;
        }
    }

    void rightCLick() {
        String message = "Right";
        if (tx == null) {
            // Do nothing if there is no device
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            mainActivity.writeLine("Sent: " + message);
        }
        else {
            mainActivity.writeLine("Couldn't write TX characteristic!");
        }
    }

    void leftClick() {
        String message = "Left";
        if (tx == null) {
            // Do nothing if there is no device
            return;
        }
        // Update TX characteristic value.  Note the setValue overload that takes a byte array must be used.
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) {
            mainActivity.writeLine("Sent: " + message);
        }
        else {
            mainActivity.writeLine("Couldn't write TX characteristic!");
        }
    }
}