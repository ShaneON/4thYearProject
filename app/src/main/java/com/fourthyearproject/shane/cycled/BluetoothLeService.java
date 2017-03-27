package com.fourthyearproject.shane.cycled;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hp on 22/02/2017.
 */

public class BluetoothLeService extends Service {
    private static final String TAG = "BluetoothLeService";
    private BluetoothAdapter adapter;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic tx;
    private BluetoothGattCharacteristic rx;
    ResultReceiver resultReceiver;
    private Handler handler;
    private int currentState;
    private boolean scanning;
    private String receivedValue = "";
    private UUIDS uuids;
    private ArrayList<String> latLngList = new ArrayList<>();
    private int currentLatLngListIndex = 0;
    private String currentDirection = null;
    private int directionCount = 0;

    @Override // Function called when service is started from activity
    public int onStartCommand(Intent intent, int flags, int startId) {
        adapter = BluetoothAdapter.getDefaultAdapter();
        handler = new Handler();
        resultReceiver = intent.getParcelableExtra("resultReceiver");
        uuids = new UUIDS();
        scanForDevices();
        LocalBroadcastManager.getInstance(this).registerReceiver(directionsReceiver,
                new IntentFilter("directions message"));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        stopConnection();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(directionsReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver directionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(null != intent.getExtras().getString("direction"))
                sendDirection(intent);
            else if(null != intent.getExtras().getStringArrayList("LatLng")) {
                ArrayList<String> latLngTogetherList = intent.getExtras().getStringArrayList("LatLng");
                createFinalLatLngList(latLngTogetherList);
                sendMessage(latLngList.get(currentLatLngListIndex));
            }
        }
    };

    private void createFinalLatLngList(ArrayList<String> latLngTogetherList) {
        for(int i = 0; i < latLngTogetherList.size(); i++){
            String [] latLngArray;
            if(!"end".equals(latLngTogetherList.get(i))) {
                latLngArray = seperateLatLng(removeSpaces(latLngTogetherList.get(i)));
                Log.d(TAG, "The lat is " + latLngArray[0] + " and the lng is " + latLngArray[1]);
                latLngList.add(latLngArray[0]);
                latLngList.add(latLngArray[1]);
            }
            else latLngList.add(latLngTogetherList.get(i));
        }
    }

    private void sendDirection(Intent intent){
        Bundle bluetoothBundle = new Bundle();
        String direction = intent.getExtras().getString("direction");
        Log.d(TAG, direction);
        bluetoothBundle.putString("direction", direction);
        resultReceiver.send(0, bluetoothBundle);
        if("turn-left".equals(direction)) {
            currentDirection = "lft" + directionCount;
            directionCount++;
            sendMessage(currentDirection);
        }
        else if("turn-right".equals(direction)) {
            currentDirection = "rgt" + directionCount;
            directionCount++;
            sendMessage(currentDirection);
        }
    }

    private String [] seperateLatLng(String latLngString){
        String [] latLngArray;
        int commaIndex = latLngString.indexOf(',');
        int lngLength = latLngString.substring(commaIndex + 1).length();
        if(commaIndex <= 13 && lngLength <= 13)
            latLngArray = getLatLngSubstring(0, commaIndex, commaIndex + 1, latLngString.length(), latLngString);
        else if(commaIndex <= 13 && lngLength > 13)
            latLngArray = getLatLngSubstring(0, commaIndex, commaIndex + 1, commaIndex + 13, latLngString);
        else if(commaIndex > 13 && lngLength <= 13)
            latLngArray = getLatLngSubstring(0, 13, commaIndex + 1, latLngString.length(), latLngString);
        else
            latLngArray =  getLatLngSubstring(0, 13, commaIndex + 1, commaIndex + 13, latLngString);
        return latLngArray;
    }

    private String [] getLatLngSubstring(int startOne, int endOne, int startTwo, int endTwo,
                                 String latLngString){
        String [] latLngArray = new String[2];
        latLngArray[0] = latLngString.substring(startOne, endOne);
        latLngArray[1] = latLngString.substring(startTwo, endTwo);
        return latLngArray;
    }

    private String removeSpaces(String latLngs){ return latLngs.replaceAll("\\s+",""); }

    void scanForDevices() {
        final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
        final Bundle bluetoothBundle = new Bundle();
        final int SCAN_TIME = 10000;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                scanning = false;
                bluetoothBundle.putString("No device", "No device detected, please turn on bluetooth device.");
                resultReceiver.send(0, bluetoothBundle);
            }
        }, SCAN_TIME);
        bluetoothBundle.putString("Scanning", "Scanning for devices...");
        scanning = true;
        bluetoothLeScanner.startScan(leScanCallback);
    }

    // Main BTLE device callback where much of the logic occurs.
    private BluetoothGattCallback callback = new BluetoothGattCallback() {
        // Called whenever the device connection state changes, i.e. from disconnected to connected.
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Bundle bluetoothBundle = new Bundle();
            currentState = newState;
            if (newState == BluetoothGatt.STATE_CONNECTED) {

                if (!gatt.discoverServices()) {
                    Log.d(TAG, "Failed to Connect");
                    bluetoothBundle.putString("Failed to Connect", "Failed to Connect");
                    resultReceiver.send(0, bluetoothBundle);
                }
                else {
                    bluetoothBundle.putString("Connected", "Connected");
                    resultReceiver.send(0, bluetoothBundle);
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected");
                bluetoothBundle.putString("Disconnected", "Disconnected");
                resultReceiver.send(0, bluetoothBundle);
                scanForDevices();
            } else Log.d(TAG, "New State: " + newState);

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS)
                Log.d(TAG, "Service discovery completed!");
            else
                Log.d(TAG, "Service discovery failed with status: " + status);
            // Save reference to each characteristic.
            tx = gatt.getService(uuids.getUartUuid()).getCharacteristic(uuids.getTxUuid());
            rx = gatt.getService(uuids.getUartUuid()).getCharacteristic(uuids.getRxUuid());
            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.
            if (!gatt.setCharacteristicNotification(rx, true))
                Log.d(TAG, "Couldn't set notifications for RX characteristic!");
            // Next update the RX characteristic's client descriptor to enable notifications.
            if (rx.getDescriptor(uuids.getClientUuid()) != null) {
                BluetoothGattDescriptor desc = rx.getDescriptor(uuids.getClientUuid());
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(desc))
                    Log.d(TAG, "Couldn't write RX client descriptor value!");
            }
            else Log.d(TAG, "Couldn't get RX client descriptor!");
        }

        // Called when a remote characteristic changes (like the RX characteristic).
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            receivedValue = characteristic.getStringValue(0);
            Log.d(TAG, "Received: " + receivedValue);
            if(receivedValue.equals(latLngList.get(currentLatLngListIndex)) &&
                    currentLatLngListIndex < latLngList.size() - 1) {
                sendMessage(latLngList.get(++currentLatLngListIndex));
                Log.d(TAG, "Successful");
            }
            /*else if(!receivedValue.equals(currentDirection) && (receivedValue.charAt(0) == 'r'
                            || receivedValue.charAt(1) == 'f')) {
                sendMessage(currentDirection);
            }*/
            else if(currentLatLngListIndex < latLngList.size() - 1){
                Log.d(TAG, "unsuccessful");
                sendMessage(latLngList.get(currentLatLngListIndex));
            }
        }
    };

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothLeScanner bluetoothLeScanner = adapter.getBluetoothLeScanner();
            BluetoothDevice device = result.getDevice();
            //mainActivity.writeLine("Found device: " + device.getAddress());

            if(null != result.getScanRecord().getServiceUuids()) {
                List<ParcelUuid> parcelUuidList = result.getScanRecord().getServiceUuids();

                for (ParcelUuid u : parcelUuidList) {
                    if (uuids.getUartUuid().toString().equals(u.getUuid().toString())) {
                        bluetoothLeScanner.stopScan(leScanCallback);
                        // Connect to the device.
                        // Control flow will now go to the callback functions when BTLE events occur.
                        gatt = result.getDevice().connectGatt(getApplication(), false, callback);
                    }
                }
            }
            else {Log.d(TAG, "No UUIDs found for device.");}

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

    void sendMessage(String message) {
        if (tx == null) return;
        // Update TX characteristic value.
        tx.setValue(message.getBytes(Charset.forName("UTF-8")));
        if (gatt.writeCharacteristic(tx)) Log.d(TAG, "Sent: " + message);
       /* else if(message.charAt(1) == 'e' || message.charAt(0) == 'r') {
            Log.d(TAG, "Couldn't write message: " + message);
            sendMessage(currentDirection);
        }*/
        else {
            Log.d(TAG, "Couldn't write message: " + message);
            sendMessage(latLngList.get(currentLatLngListIndex));
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
